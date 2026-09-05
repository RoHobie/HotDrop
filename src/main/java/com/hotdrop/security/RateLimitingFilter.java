package com.hotdrop.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotdrop.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int capacity;
    private final double refillRatePerSecond;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${hotdrop.rate-limiting.enabled:true}") boolean enabled,
            @Value("${hotdrop.rate-limiting.capacity:50}") int capacity,
            @Value("${hotdrop.rate-limiting.refill-rate:20}") double refillRatePerSecond
    ) {
        this.enabled = enabled;
        this.capacity = capacity;
        this.refillRatePerSecond = refillRatePerSecond;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        // Rate limit sensitive endpoints: join, book, and login
        if (isRateLimitedPath(path)) {
            String clientKey = resolveClientKey(request);
            TokenBucket bucket = buckets.computeIfAbsent(clientKey, k -> new TokenBucket(capacity, refillRatePerSecond));

            if (!bucket.tryConsume()) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                ErrorResponse error = ErrorResponse.of(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too Many Requests",
                        "Rate limit exceeded. Please slow down.",
                        path
                );
                response.getWriter().write(objectMapper.writeValueAsString(error));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedPath(String path) {
        return path.contains("/waiting-room/join") || path.contains("/book") || path.equals("/auth/login");
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    static class TokenBucket {
        private final int maxTokens;
        private final double refillRatePerNano;
        private double tokens;
        private long lastRefillNanos;

        TokenBucket(int maxTokens, double refillRatePerSecond) {
            this.maxTokens = maxTokens;
            this.refillRatePerNano = refillRatePerSecond / 1_000_000_000.0;
            this.tokens = maxTokens;
            this.lastRefillNanos = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos;
            tokens = Math.min(maxTokens, tokens + elapsed * refillRatePerNano);
            lastRefillNanos = now;
        }
    }
}
