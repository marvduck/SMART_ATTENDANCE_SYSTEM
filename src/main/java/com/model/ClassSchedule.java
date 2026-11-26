package com.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** Monday-Saturday schedule; sectionName and teacherName are query-only display fields. */
public record ClassSchedule(Integer scheduleId, int sectionId, Integer teacherId, String subject,
                            DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String room,
                            String sectionName, String teacherName) {

    public static ClassSchedule create(int sectionId, Integer teacherId, String subject,
                                       DayOfWeek day, LocalTime start, LocalTime end, String room) {
        return new ClassSchedule(null, sectionId, teacherId, subject, day, start, end, room, null, null);
    }
}
