package com.hotdrop.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.event.dto.CreateEventRequest;
import com.hotdrop.event.dto.EventDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class EventLifecycleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register and login admin
        SignupRequest adminSignup = new SignupRequest("Admin", "admin@hotdrop.local", "AdminPass123!", Role.ADMIN);
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminSignup)))
                .andExpect(status().isCreated());

        MvcResult adminLogin = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("admin@hotdrop.local", "AdminPass123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readValue(adminLogin.getResponse().getContentAsString(), AuthResponse.class).token();

        // 2. Register and login regular user
        SignupRequest userSignup = new SignupRequest("User", "user@hotdrop.local", "UserPass123!", Role.USER);
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignup)))
                .andExpect(status().isCreated());

        MvcResult userLogin = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("user@hotdrop.local", "UserPass123!"))))
                .andExpect(status().isOk())
                .andReturn();
        userToken = objectMapper.readValue(userLogin.getResponse().getContentAsString(), AuthResponse.class).token();
    }

    @Test
    void adminCanCreateEventAndUserCanViewIt() throws Exception {
        Instant saleTime = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateEventRequest request = new CreateEventRequest(
                "Rock Festival 2026",
                "Grand outdoor festival",
                500,
                saleTime,
                900
        );

        // Admin creates event
        MvcResult createResult = mockMvc.perform(post("/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Rock Festival 2026"))
                .andExpect(jsonPath("$.totalTickets").value(500))
                .andExpect(jsonPath("$.ticketsSold").value(0))
                .andExpect(jsonPath("$.status").value("UPCOMING"))
                .andReturn();

        EventDto eventDto = objectMapper.readValue(createResult.getResponse().getContentAsString(), EventDto.class);

        // Public/User can view event
        mockMvc.perform(get("/events/" + eventDto.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rock Festival 2026"))
                .andExpect(jsonPath("$.status").value("UPCOMING"));

        // User can list events by status
        mockMvc.perform(get("/events?status=UPCOMING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/events?status=ENDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void nonAdminCannotCreateOrCancelEvents() throws Exception {
        Instant saleTime = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateEventRequest request = new CreateEventRequest(
                "Exclusive VIP Sale",
                "VIP only",
                50,
                saleTime,
                300
        );

        // User trying to create event -> 403
        mockMvc.perform(post("/admin/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        // Admin creates event
        MvcResult createResult = mockMvc.perform(post("/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        EventDto eventDto = objectMapper.readValue(createResult.getResponse().getContentAsString(), EventDto.class);

        // User trying to cancel event -> 403
        mockMvc.perform(patch("/admin/events/" + eventDto.id() + "/cancel")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCancelEventAndCannotDoubleCancel() throws Exception {
        Instant saleTime = Instant.now().plus(1, ChronoUnit.HOURS);
        CreateEventRequest request = new CreateEventRequest(
                "Indie Night",
                "Acoustic session",
                80,
                saleTime,
                600
        );

        MvcResult createResult = mockMvc.perform(post("/admin/events")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        EventDto eventDto = objectMapper.readValue(createResult.getResponse().getContentAsString(), EventDto.class);

        // Admin cancels event -> 200 OK
        mockMvc.perform(patch("/admin/events/" + eventDto.id() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.message").value("Event cancelled successfully"));

        // Admin attempts cancelling again -> 400 Bad Request
        mockMvc.perform(patch("/admin/events/" + eventDto.id() + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Event is already cancelled"));
    }
}
