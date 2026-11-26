package com.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceRecord(Integer recordId, int studentId, LocalDate attendanceDate,
                               LocalTime timeIn, LocalTime timeOut, AttendanceStatus status, String remarks) {
}
