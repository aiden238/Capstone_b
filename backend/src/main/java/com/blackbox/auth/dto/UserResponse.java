package com.blackbox.auth.dto;

import com.blackbox.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String email;
    private String name;
    private String role;
    private String avatarUrl;
    private OffsetDateTime createdAt;

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }
}
