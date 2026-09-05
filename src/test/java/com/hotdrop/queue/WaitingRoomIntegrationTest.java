package com.hotdrop.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.event.Event;
import com.hotdrop.event.EventRepository;
import com.hotdrop.event.EventStatus;
import com.hotdrop.user.Role;
import com.hotdrop.user.UserRepository;
import com.hotdrop.user.dto.AuthResponse;
import com.hotdrop.user.dto.LoginRequest;
import com.hotdrop.user.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
class WaitingRoomIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String userToken;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        queueEntryRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        SignupRequest userSignup = new SignupRequest("Bob Builder", "bob@example.com", "Password123!", Role.USER);
        MvcResult signupResult = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignup)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("bob@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(loginResult.getResponse().getContentAsString(), AuthResponse.class);
        userToken = authResponse.token();
        userId = authResponse.user().id();
    }

    @Test
    void joiningTooEarlyIsRejected() throws Exception {
        // Sale starts in 2 hours, waiting room offset is 15 minutes (900 seconds) -> opens in 1 hr 45 min
        Instant saleStart = Instant.now().plus(2, ChronoUnit.HOURS);
        Event event = new Event("Far Future Event", "Description", 100, saleStart, 900);
        event = eventRepository.save(event);

        mockMvc.perform(post("/events/" + event.getId() + "/waiting-room/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Waiting room is not open yet")));
    }

    @Test
    void joiningWithinWindowSucceeds() throws Exception {
        // Sale starts in 5 minutes, waiting room offset is 10 minutes (600s) -> already open
        Instant saleStart = Instant.now().plus(5, ChronoUnit.MINUTES);
        Event event = new Event("Ready Event", "Description", 100, saleStart, 600);
        event = eventRepository.save(event);

        mockMvc.perform(post("/events/" + event.getId() + "/waiting-room/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(event.getId()))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("WAITING"));

        assertThat(queueEntryRepository.countByEventId(event.getId())).isEqualTo(1);
    }

    @Test
    void joiningTwiceIsIdempotent() throws Exception {
        Instant saleStart = Instant.now().plus(5, ChronoUnit.MINUTES);
        Event event = new Event("Idempotent Event", "Description", 100, saleStart, 600);
        event = eventRepository.save(event);

        // First join
        mockMvc.perform(post("/events/" + event.getId() + "/waiting-room/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));

        // Second join
        mockMvc.perform(post("/events/" + event.getId() + "/waiting-room/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING"));

        // Exactly one queue entry exists in database
        assertThat(queueEntryRepository.countByEventId(event.getId())).isEqualTo(1);
    }

    @Test
    void joiningCancelledEventIsRejected() throws Exception {
        Instant saleStart = Instant.now().plus(5, ChronoUnit.MINUTES);
        Event event = new Event("Cancelled Event", "Description", 100, saleStart, 600);
        event.setStatus(EventStatus.CANCELLED);
        event = eventRepository.save(event);

        mockMvc.perform(post("/events/" + event.getId() + "/waiting-room/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot join waiting room for a cancelled event"));
    }

    @Test
    void canCheckWaitingRoomStatus() throws Exception {
        Instant saleStart = Instant.now().plus(5, ChronoUnit.MINUTES);
        Event event = new Event("Status Check Event", "Description", 100, saleStart, 600);
        event = eventRepository.save(event);

        mockMvc.perform(post("/events/" + event.getId() + "/waiting-room/join")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/events/" + event.getId() + "/waiting-room/status")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(event.getId()))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.secondsUntilSale").isNumber());
    }
}
