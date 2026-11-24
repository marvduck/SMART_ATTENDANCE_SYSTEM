package com.model;

import java.time.LocalDateTime;

/** studentId, cardUid and studentName can be null (for example an unregistered card). */
public record Alert(Integer alertId, AlertType type, Integer studentId, String cardUid, String message,
                    boolean read, LocalDateTime createdAt, String studentName) {
}
