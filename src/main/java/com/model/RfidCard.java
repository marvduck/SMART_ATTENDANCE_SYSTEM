package com.model;

import java.time.LocalDateTime;

/** studentName is display-only (filled by queries). */
public record RfidCard(Integer cardId, String cardUid, int studentId, CardStatus status,
                       LocalDateTime assignedAt, String studentName) {
}
