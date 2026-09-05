package com.hotdrop.user.dto;

import com.hotdrop.user.Role;
import com.hotdrop.user.User;

import java.time.Instant;

public record UserDto(
        Long id,
        String name,
        String email,
        Role role,
        Instant createdAt
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
