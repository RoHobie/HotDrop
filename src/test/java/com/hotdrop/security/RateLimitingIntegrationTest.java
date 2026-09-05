package com.hotdrop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.TestcontainersConfiguration;
import com.hotdrop.user.dto.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "hotdrop.rate-limiting.enabled=true",
        "hotdrop.rate-limiting.capacity=3",
        "hotdrop.rate-limiting.refill-rate=0.1"
})
class RateLimitingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldReturn429WhenRateLimitExceeded() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "pass");

        // First 3 requests consume available tokens
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/auth/login")
                            .header("X-Forwarded-For", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)));
        }

        // 4th request from same IP must be rate limited with 429
        mockMvc.perform(post("/auth/login")
                        .header("X-Forwarded-For", "192.168.1.100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please slow down."));
    }
}
