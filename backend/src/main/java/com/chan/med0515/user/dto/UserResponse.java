package com.chan.med0515.user.dto;

import com.chan.med0515.user.entity.User;
import com.chan.med0515.user.enums.UserRole;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        UserRole role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
