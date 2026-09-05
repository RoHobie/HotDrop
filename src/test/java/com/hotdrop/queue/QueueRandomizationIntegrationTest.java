package com.hotdrop.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.security.JwtService;
import com.hotdrop.user.Role;
import com.hotdrop.user.User;
import com.hotdrop.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class QueueRandomizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void cleanDatabase() {
        queueEntryRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRandomizeWaitingRoomFairlyAndAppendLatecomers() throws Exception {
        // 1. Create an event starting in 1 second, waiting room open
        Instant saleStart = Instant.now().plus(1, ChronoUnit.SECONDS);
        Event event = new Event("Shuffled Festival", "Live music", 200, saleStart, 300);
        event = eventRepository.save(event);
        final Long eventId = event.getId();

        // 2. Register 30 users and have them join the waiting room
        List<User> users = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            User user = new User("User " + i, "user" + i + "@hotdrop.local", "hashed", Role.USER);
            user = userRepository.save(user);
            users.add(user);
            tokens.add(jwtService.generateToken(user));

            queueService.joinWaitingRoom(eventId, user.getId());
        }

        // Verify all 30 entries are WAITING with null position
        List<QueueEntry> beforeFinalize = queueEntryRepository.findAll();
        assertThat(beforeFinalize).hasSize(30);
        assertThat(beforeFinalize).allMatch(e -> e.getStatus() == QueueStatus.WAITING && e.getQueuePosition() == null);

        // 3. Finalize queue
        boolean finalized = queueService.finalizeQueue(eventId);
        assertThat(finalized).isTrue();

        // Verify event is marked finalized and LIVE
        Event updatedEvent = eventRepository.findById(eventId).orElseThrow();
        assertThat(updatedEvent.getQueueFinalizedAt()).isNotNull();
        assertThat(updatedEvent.getStatus()).isEqualTo(EventStatus.LIVE);

        // 4. Verify all 30 entries have been assigned contiguous positions 1..30 with zero duplicates or gaps
        List<QueueEntry> afterFinalize = queueEntryRepository.findAll();
        assertThat(afterFinalize).hasSize(30);
        assertThat(afterFinalize).allMatch(e -> e.getStatus() == QueueStatus.QUEUED);

        Set<Long> assignedPositions = new HashSet<>();
        boolean orderDiffersFromJoin = false;
        for (int i = 0; i < 30; i++) {
            Long userId = users.get(i).getId();
            QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, userId).orElseThrow();
            Long pos = entry.getQueuePosition();
            assertThat(pos).isNotNull().isBetween(1L, 30L);
            assignedPositions.add(pos);

            // User i was inserted i+1th. Check if randomized position differs from join order.
            if (pos != (i + 1L)) {
                orderDiffersFromJoin = true;
            }
        }
        assertThat(assignedPositions).hasSize(30); // strictly unique positions 1..30
        assertThat(orderDiffersFromJoin).isTrue(); // randomized!

        // 5. Verify finalization is idempotent
        boolean doubleFinalize = queueService.finalizeQueue(eventId);
        assertThat(doubleFinalize).isFalse();

        // 6. Test Late Joiner: User 31 joins AFTER queue finalization
        User lateUser = new User("Late User", "late@hotdrop.local", "hashed", Role.USER);
        lateUser = userRepository.save(lateUser);
        String lateToken = jwtService.generateToken(lateUser);

        mockMvc.perform(post("/events/" + eventId + "/waiting-room/join")
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("QUEUED"));

        QueueEntry lateEntry = queueEntryRepository.findByEventIdAndUserId(eventId, lateUser.getId()).orElseThrow();
        assertThat(lateEntry.getStatus()).isEqualTo(QueueStatus.QUEUED);
        assertThat(lateEntry.getQueuePosition()).isEqualTo(31L); // Appended strictly to the back

        // 7. Verify Queue status endpoint
        mockMvc.perform(get("/events/" + eventId + "/queue/status")
                        .header("Authorization", "Bearer " + lateToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.userId").value(lateUser.getId()))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.queuePosition").value(31))
                .andExpect(jsonPath("$.totalInQueue").value(31));
    }
}
