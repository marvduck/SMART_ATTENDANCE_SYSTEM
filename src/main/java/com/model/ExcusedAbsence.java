package com.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** markedBy is the user_id of the teacher/admin who excused the absence. */
public record ExcusedAbsence(Integer excusedId, int studentId, LocalDate absenceDate, String reason,
                             Integer markedBy, LocalDateTime createdAt, String studentName) {
}
