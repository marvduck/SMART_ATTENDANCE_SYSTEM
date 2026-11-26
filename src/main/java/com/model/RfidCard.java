package com.model;

import java.time.LocalDateTime;

/** studentName is a query-filled display field. */
public record RfidCard(Integer cardId, String cardUid, int studentId, CardStatus status,
                       LocalDateTime assignedAt, String studentName) {
}
