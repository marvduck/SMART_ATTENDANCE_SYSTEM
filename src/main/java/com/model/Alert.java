package com.model;

import java.time.LocalDateTime;

/** studentId, cardUid and studentName may be null for unregistered cards. */
public record Alert(Integer alertId, AlertType type, Integer studentId, String cardUid, String message,
                    boolean read, LocalDateTime createdAt, String studentName) {
}
