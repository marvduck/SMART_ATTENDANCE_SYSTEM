package com.model;

import java.time.LocalDateTime;

/** A login account. passwordHash is a BCrypt hash, never the plain password. */
public record User(Integer userId, String username, String passwordHash, Role role,
                   boolean active, LocalDateTime createdAt, LocalDateTime lastLogin) {

    /** A new, active user that has not been saved yet. */
    public static User create(String username, String passwordHash, Role role) {
        return new User(null, username, passwordHash, role, true, null, null);
    }
}
