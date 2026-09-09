package com.hotdrop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.QueueEntry;
import com.hotdrop.queue.QueueEntryRepository;
import com.hotdrop.queue.QueueService;
import com.hotdrop.queue.QueueStatus;
import com.hotdrop.security.JwtService;
import com.hotdrop.user.User;
import com.hotdrop.user.UserRepository;
import com.hotdrop.user.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the in-memory H2 datastore mode (no external PostgreSQL required).
 * Verifies application boot, Flyway migrations V1-V8, seeded accounts/events,
 * queue randomization, and booking flow on H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InMemoryDatabaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBootWithInMemoryDatastoreAndSeedInitialData() throws Exception {
        // 1. Verify pre-seeded demo accounts from V7
        assertThat(userRepository.findByEmail("admin@hotdrop.io")).isPresent();
        assertThat(userRepository.findByEmail("buyer@hotdrop.io")).isPresent();

        // 2. Verify pre-seeded demo events from V8
        List<Event> events = eventRepository.findAll();
        assertThat(events).isNotEmpty();
        assertThat(events).anyMatch(e -> e.getName().contains("Cyberpunk 2099"));
        assertThat(events).anyMatch(e -> e.getName().contains("Neon Genesis Live"));

        // 3. Test public login with pre-seeded buyer demo credentials
        LoginRequest buyerLogin = new LoginRequest("buyer@hotdrop.io", "Buyer123!@#");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyerLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("buyer@hotdrop.io"))
                .andExpect(jsonPath("$.user.role").value("USER"));

        // 4. Test public login with pre-seeded admin demo credentials
        LoginRequest adminLogin = new LoginRequest("admin@hotdrop.io", "Admin123!@#");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@hotdrop.io"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));

        // 5. Test health endpoint on in-memory mode
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldPerformQueueShuffleAndBookingOnInMemoryDatastore() throws Exception {
        // Create an event for queue test
        Instant saleStart = Instant.now().plus(10, ChronoUnit.SECONDS);
        Event event = new Event("In-Memory Drop", "Testing queue on H2", 100, saleStart, 300);
        event = eventRepository.save(event);
        Long eventId = event.getId();

        User user = userRepository.findByEmail("buyer@hotdrop.io").orElseThrow();
        String token = jwtService.generateToken(user);

        // Join waiting room
        queueService.joinWaitingRoom(eventId, user.getId());
        QueueEntry entry = queueEntryRepository.findByEventIdAndUserId(eventId, user.getId()).orElseThrow();
        assertThat(entry.getStatus()).isEqualTo(QueueStatus.WAITING);

        // Finalize queue (executes custom H2 MERGE query)
        boolean finalized = queueService.finalizeQueue(eventId);
        assertThat(finalized).isTrue();

        QueueEntry queuedEntry = queueEntryRepository.findByEventIdAndUserId(eventId, user.getId()).orElseThrow();
        assertThat(queuedEntry.getStatus()).isEqualTo(QueueStatus.QUEUED);
        assertThat(queuedEntry.getQueuePosition()).isEqualTo(1L);

        // Admit user to allow checkout
        queuedEntry.setStatus(QueueStatus.ADMITTED);
        queuedEntry.setAdmittedAt(Instant.now());
        queuedEntry.setAdmissionExpiresAt(Instant.now().plus(120, ChronoUnit.SECONDS));
        queueEntryRepository.save(queuedEntry);

        // Book ticket
        mockMvc.perform(post("/events/" + eventId + "/book")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.eventId").value(eventId));

        Event updatedEvent = eventRepository.findById(eventId).orElseThrow();
        assertThat(updatedEvent.getTicketsSold()).isEqualTo(1);
    }
}
