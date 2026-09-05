package com.hotdrop.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.queue.QueueEntry;
import com.hotdrop.queue.QueueEntryRepository;
import com.hotdrop.queue.QueueStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BookingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private User testUser;
    private String userToken;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        queueEntryRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User("Alice Booker", "alice.booker@hotdrop.local", "hashed", Role.USER);
        testUser = userRepository.save(testUser);
        userToken = jwtService.generateToken(testUser);

        testEvent = new Event("Hot Summer Concert", "Live outdoor concert", 100, Instant.now().minus(5, ChronoUnit.MINUTES), 300);
        testEvent.setStatus(EventStatus.LIVE);
        testEvent.setQueueFinalizedAt(Instant.now().minus(4, ChronoUnit.MINUTES));
        testEvent = eventRepository.save(testEvent);
    }

    @Test
    void admittedUserCanBookTicketAndRetrieveBookings() throws Exception {
        // Set queue entry to ADMITTED
        QueueEntry queueEntry = new QueueEntry(testEvent.getId(), testUser.getId());
        queueEntry.setStatus(QueueStatus.ADMITTED);
        queueEntry.setQueuePosition(1L);
        queueEntry.setAdmittedAt(Instant.now());
        queueEntry.setAdmissionExpiresAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        queueEntryRepository.save(queueEntry);

        // Book ticket
        mockMvc.perform(post("/events/" + testEvent.getId() + "/book")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").isNumber())
                .andExpect(jsonPath("$.eventId").value(testEvent.getId()))
                .andExpect(jsonPath("$.eventName").value("Hot Summer Concert"))
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify queue entry transitioned to COMPLETED
        QueueEntry updatedEntry = queueEntryRepository.findByEventIdAndUserId(testEvent.getId(), testUser.getId()).orElseThrow();
        assertThat(updatedEntry.getStatus()).isEqualTo(QueueStatus.COMPLETED);

        // Verify tickets_sold updated in DB
        Event updatedEvent = eventRepository.findById(testEvent.getId()).orElseThrow();
        assertThat(updatedEvent.getTicketsSold()).isEqualTo(1);

        // Verify GET /users/me/bookings
        mockMvc.perform(get("/users/me/bookings")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventId").value(testEvent.getId()))
                .andExpect(jsonPath("$[0].eventName").value("Hot Summer Concert"));
    }

    @Test
    void nonAdmittedUserCannotBook() throws Exception {
        // Queue entry is QUEUED, not ADMITTED
        QueueEntry queueEntry = new QueueEntry(testEvent.getId(), testUser.getId());
        queueEntry.setStatus(QueueStatus.QUEUED);
        queueEntry.setQueuePosition(25L);
        queueEntryRepository.save(queueEntry);

        mockMvc.perform(post("/events/" + testEvent.getId() + "/book")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("It is not your turn yet")));
    }

    @Test
    void doubleBookingIsRejected() throws Exception {
        QueueEntry queueEntry = new QueueEntry(testEvent.getId(), testUser.getId());
        queueEntry.setStatus(QueueStatus.ADMITTED);
        queueEntry.setQueuePosition(1L);
        queueEntry.setAdmittedAt(Instant.now());
        queueEntry.setAdmissionExpiresAt(Instant.now().plus(2, ChronoUnit.MINUTES));
        queueEntryRepository.save(queueEntry);

        // First booking succeeds
        mockMvc.perform(post("/events/" + testEvent.getId() + "/book")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        // Second booking attempt is rejected
        mockMvc.perform(post("/events/" + testEvent.getId() + "/book")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already booked")));
    }
}
