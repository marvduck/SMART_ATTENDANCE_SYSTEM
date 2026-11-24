package com.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** sectionName and teacherName are display-only (filled by queries). Days: Monday to Saturday. */
public record ClassSchedule(Integer scheduleId, int sectionId, Integer teacherId, String subject,
                            DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime, String room,
                            String sectionName, String teacherName) {

    public static ClassSchedule create(int sectionId, Integer teacherId, String subject,
                                       DayOfWeek day, LocalTime start, LocalTime end, String room) {
        return new ClassSchedule(null, sectionId, teacherId, subject, day, start, end, room, null, null);
    }
}
