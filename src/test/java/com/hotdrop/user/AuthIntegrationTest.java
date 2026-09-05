package com.hotdrop.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.user.dto.AuthResponse;
import com.hotdrop.user.dto.LoginRequest;
import com.hotdrop.user.dto.SignupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({TestcontainersConfiguration.class, AuthIntegrationTest.TestAdminControllerConfig.class})
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Autowired
    private UserRepository userRepository;

    @TestConfiguration
    static class TestAdminControllerConfig {
        @RestController
        @RequestMapping("/admin/test")
        static class TestAdminController {
            @GetMapping
            public Map<String, String> ping() {
                return Map.of("message", "admin pong");
            }
        }
    }

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserSuccessfully() throws Exception {
        SignupRequest request = new SignupRequest("Jane Doe", "jane@example.com", "Password123!", Role.USER);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Jane Doe"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findByEmail("jane@example.com")).isPresent();
    }

    @Test
    void shouldRejectDuplicateEmailOnSignup() throws Exception {
        SignupRequest request = new SignupRequest("Jane Doe", "duplicate@example.com", "Password123!", Role.USER);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void shouldRejectInvalidSignupInput() throws Exception {
        SignupRequest request = new SignupRequest("", "not-an-email", "short", Role.USER);

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldLoginAndReturnJwtToken() throws Exception {
        SignupRequest signup = new SignupRequest("John Smith", "john@example.com", "SecretPass123", Role.USER);
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("john@example.com", "SecretPass123");
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("john@example.com"))
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(result.getResponse().getContentAsString(), AuthResponse.class);
        assertThat(authResponse.token()).isNotBlank();
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        SignupRequest signup = new SignupRequest("John Smith", "wrongpass@example.com", "SecretPass123", Role.USER);
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("wrongpass@example.com", "WrongPassword!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldEnforceRoleBasedAccessControl() throws Exception {
        // 1. Create Regular User
        SignupRequest userSignup = new SignupRequest("Regular User", "user@example.com", "Password123!", Role.USER);
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userSignup)))
                .andExpect(status().isCreated());

        MvcResult userLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("user@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String userToken = objectMapper.readValue(userLoginResult.getResponse().getContentAsString(), AuthResponse.class).token();

        // 2. Create Admin User
        SignupRequest adminSignup = new SignupRequest("Admin User", "admin@example.com", "Password123!", Role.ADMIN);
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminSignup)))
                .andExpect(status().isCreated());

        MvcResult adminLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@example.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String adminToken = objectMapper.readValue(adminLoginResult.getResponse().getContentAsString(), AuthResponse.class).token();

        // 3. Request admin route without token -> 403 (or 401)
        mockMvc.perform(get("/admin/test"))
                .andExpect(status().isForbidden());

        // 4. Request admin route with regular USER token -> 403 Forbidden
        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        // 5. Request admin route with ADMIN token -> 200 OK
        mockMvc.perform(get("/admin/test")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("admin pong"));
    }

    @Test
    void shouldLogoutSuccessfully() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
