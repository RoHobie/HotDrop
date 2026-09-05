package com.hotdrop.queue;

import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.user.Role;
import com.hotdrop.user.User;
import com.hotdrop.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class QueueAdmissionIntegrationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private AdmissionService admissionService;

    private Long eventId;
    private List<User> users;

    @BeforeEach
    void setUp() {
        queueEntryRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        Event event = new Event("Admission Live Event", "Test desc", 100, Instant.now().minus(10, ChronoUnit.SECONDS), 300);
        event.setStatus(EventStatus.LIVE);
        event.setQueueFinalizedAt(Instant.now().minus(5, ChronoUnit.SECONDS));
        event = eventRepository.save(event);
        eventId = event.getId();

        users = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            User u = new User("User " + i, "u" + i + "@hotdrop.local", "pass", Role.USER);
            u = userRepository.save(u);
            users.add(u);

            QueueEntry entry = new QueueEntry(eventId, u.getId());
            entry.setStatus(QueueStatus.QUEUED);
            entry.setQueuePosition((long) i);
            queueEntryRepository.save(entry);
        }
    }

    @Test
    void shouldAdmitQueuedUsersInBatchesOrderedByPosition() {
        // Admit first batch of 3
        int admittedCount = admissionService.admitNextBatchForEvent(eventId, 3);
        assertThat(admittedCount).isEqualTo(3);

        // Verify users 1, 2, 3 are ADMITTED with active window
        for (int i = 0; i < 3; i++) {
            QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, users.get(i).getId()).orElseThrow();
            assertThat(entry.getStatus()).isEqualTo(QueueStatus.ADMITTED);
            assertThat(entry.getAdmittedAt()).isNotNull();
            assertThat(entry.getAdmissionExpiresAt()).isNotNull();
            assertThat(entry.getAdmissionExpiresAt()).isAfter(Instant.now());

            // Active admission should validate successfully
            QueueEntry validated = admissionService.validateAndGetActiveAdmission(eventId, users.get(i).getId());
            assertThat(validated.getId()).isEqualTo(entry.getId());
        }

        // Verify users 4..10 are still QUEUED and rejected if attempting to book
        for (int i = 3; i < 10; i++) {
            Long userId = users.get(i).getId();
            QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, userId).orElseThrow();
            assertThat(entry.getStatus()).isEqualTo(QueueStatus.QUEUED);

            assertThatThrownBy(() -> admissionService.validateAndGetActiveAdmission(eventId, userId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("It is not your turn yet");
        }

        // Admit next batch of 2 -> users 4 and 5
        int nextBatch = admissionService.admitNextBatchForEvent(eventId, 2);
        assertThat(nextBatch).isEqualTo(2);

        QueueEntry user4 = queueEntryRepository.findByEventIdAndUserId(eventId, users.get(3).getId()).orElseThrow();
        QueueEntry user5 = queueEntryRepository.findByEventIdAndUserId(eventId, users.get(4).getId()).orElseThrow();
        assertThat(user4.getStatus()).isEqualTo(QueueStatus.ADMITTED);
        assertThat(user5.getStatus()).isEqualTo(QueueStatus.ADMITTED);
    }

    @Test
    void shouldExpireLapsedAdmissionsAndRejectBooking() {
        // Admit batch of 2
        admissionService.admitNextBatchForEvent(eventId, 2);

        // Manually expire user 1's admission window
        QueueEntry user1Entry = queueEntryRepository.findByEventIdAndUserId(eventId, users.get(0).getId()).orElseThrow();
        user1Entry.setAdmissionExpiresAt(Instant.now().minus(10, ChronoUnit.SECONDS));
        queueEntryRepository.save(user1Entry);

        // Run expiry job
        int expiredCount = admissionService.expireLapsedAdmissions();
        assertThat(expiredCount).isGreaterThanOrEqualTo(1);

        QueueEntry updatedEntry = queueEntryRepository.findByEventIdAndUserId(eventId, users.get(0).getId()).orElseThrow();
        assertThat(updatedEntry.getStatus()).isEqualTo(QueueStatus.EXPIRED);

        // Validation must reject expired user
        assertThatThrownBy(() -> admissionService.validateAndGetActiveAdmission(eventId, users.get(0).getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("admission window has expired");
    }
}
