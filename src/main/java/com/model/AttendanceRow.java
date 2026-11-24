package com.model;

import java.time.LocalTime;

/**
 * One line of a class attendance list. status, timeIn and timeOut are null when the
 * student has no attendance record yet for that day.
 */
public record AttendanceRow(int studentId, String lrn, String fullName,
                            LocalTime timeIn, LocalTime timeOut, AttendanceStatus status) {
}
