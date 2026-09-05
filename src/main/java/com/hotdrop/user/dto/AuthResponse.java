package com.hotdrop.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresIn,
        UserDto user
) {
    public static AuthResponse of(String token, long expiresIn, UserDto user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
