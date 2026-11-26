package com.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** markedBy is the excusing teacher/admin user_id. */
public record ExcusedAbsence(Integer excusedId, int studentId, LocalDate absenceDate, String reason,
                             Integer markedBy, LocalDateTime createdAt, String studentName) {
}
