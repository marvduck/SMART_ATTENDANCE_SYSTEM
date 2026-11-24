package com.model;

/** One row of the v_student_attendance_summary view. */
public record StudentAttendanceSummary(int studentId, String lrn, String fullName, Integer sectionId,
                                       int presentDays, int lateDays, int absentDays, int excusedDays) {
}
