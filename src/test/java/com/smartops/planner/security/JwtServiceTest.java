package com.smartops.planner.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartops.planner.user.Role;
import com.smartops.planner.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-with-enough-length-for-hmac";
    private static final Instant NOW = Instant.parse("2026-05-27T10:00:00Z");

    @Test
    void generateToken_shouldCreateTokenContainingUsername() {
        JwtService jwtService = jwtService(NOW, 3600);
        User user = new User("admin", "encoded-password", Role.ADMIN);

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("admin");
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenTokenIsValid() {
        JwtService jwtService = jwtService(NOW, 3600);
        User user = new User("manager", "encoded-password", Role.MANAGER);
        UserDetails userDetails = userDetails("manager");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        JwtService jwtService = jwtService(NOW, 3600);
        User user = new User("manager", "encoded-password", Role.MANAGER);
        UserDetails userDetails = userDetails("other");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsExpired() {
        JwtService issuer = jwtService(NOW, 1);
        JwtService verifier = jwtService(NOW.plusSeconds(2), 1);
        User user = new User("employee", "encoded-password", Role.EMPLOYEE);

        String token = issuer.generateToken(user);

        assertThat(verifier.isTokenValid(token, userDetails("employee"))).isFalse();
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenTokenIsTampered() {
        JwtService jwtService = jwtService(NOW, 3600);
        User user = new User("admin", "encoded-password", Role.ADMIN);

        String token = jwtService.generateToken(user);
        String tamperedToken = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtService.isTokenValid(tamperedToken, userDetails("admin"))).isFalse();
    }

    private JwtService jwtService(Instant instant, long expirationSeconds) {
        return new JwtService(
                new ObjectMapper(),
                Clock.fixed(instant, ZoneOffset.UTC),
                SECRET,
                expirationSeconds
        );
    }

    private UserDetails userDetails(String username) {
        return org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("encoded-password")
                .roles("ADMIN")
                .build();
    }
}
