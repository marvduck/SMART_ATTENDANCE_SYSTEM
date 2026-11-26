package com.model;

import java.time.LocalDateTime;

/** Login account; passwordHash stores BCrypt, never plaintext. */
public record User(Integer userId, String username, String passwordHash, Role role,
                   boolean active, LocalDateTime createdAt, LocalDateTime lastLogin) {

    public static User create(String username, String passwordHash, Role role) {
        return new User(null, username, passwordHash, role, true, null, null);
    }
}
