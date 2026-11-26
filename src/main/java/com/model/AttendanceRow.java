package com.model;

import java.time.LocalTime;

/** Class-list row; status and times are null when the student has no record that day. */
public record AttendanceRow(int studentId, String lrn, String fullName,
                            LocalTime timeIn, LocalTime timeOut, AttendanceStatus status) {
}
