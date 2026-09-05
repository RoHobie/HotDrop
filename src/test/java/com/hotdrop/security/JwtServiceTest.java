package com.hotdrop.security;

import com.hotdrop.user.Role;
import com.hotdrop.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, 3600000);
    }

    @Test
    void shouldGenerateAndExtractClaims() {
        User user = new User("Alice", "alice@example.com", "hashed", Role.ADMIN);
        user.setId(42L);

        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();

        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void shouldDetectExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(secret, -1000);
        User user = new User("Bob", "bob@example.com", "hashed", Role.USER);
        user.setId(7L);

        String token = shortLivedJwtService.generateToken(user);
        assertThat(shortLivedJwtService.isTokenExpired(token)).isTrue();
    }
}
