package com.dao;

import com.model.ClassSchedule;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * CRUD for the class_schedules table. The table only allows Monday to Saturday,
 * so a Sunday query simply returns nothing.
 */
public class ScheduleDao {

    private static final String SELECT = """
            SELECT cs.schedule_id, cs.section_id, cs.teacher_id, cs.subject, cs.day_of_week,
                   cs.start_time, cs.end_time, cs.room,
                   sec.section_name, t.full_name AS teacher_name
            FROM class_schedules cs
            JOIN sections sec ON sec.section_id = cs.section_id
            LEFT JOIN teachers t ON t.teacher_id = cs.teacher_id""";

    private static final String ORDER = " ORDER BY FIELD(cs.day_of_week, 'MONDAY','TUESDAY','WEDNESDAY',"
            + "'THURSDAY','FRIDAY','SATURDAY'), cs.start_time";

    static ClassSchedule map(ResultSet rs) throws SQLException {
        return new ClassSchedule(
                rs.getInt("schedule_id"),
                rs.getInt("section_id"),
                DaoSupport.getInteger(rs, "teacher_id"),
                rs.getString("subject"),
                DayOfWeek.valueOf(rs.getString("day_of_week")),
                DaoSupport.time(rs, "start_time"),
                DaoSupport.time(rs, "end_time"),
                rs.getString("room"),
                rs.getString("section_name"),
                rs.getString("teacher_name"));
    }

    // ---- Create ----------------------------------------------------------------------------

    /** Returns the new schedule_id. The end time must be after the start time (checked by the database). */
    public int insert(ClassSchedule s) {
        return (int) DaoSupport.insert(
                "INSERT INTO class_schedules (section_id, teacher_id, subject, day_of_week, start_time, end_time, room)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                s.sectionId(), s.teacherId(), s.subject(), s.dayOfWeek(), s.startTime(), s.endTime(), s.room());
    }

    // ---- Read ------------------------------------------------------------------------------

    public Optional<ClassSchedule> findById(int scheduleId) {
        return DaoSupport.one(SELECT + " WHERE cs.schedule_id = ?", ScheduleDao::map, scheduleId);
    }

    public List<ClassSchedule> findAll() {
        return DaoSupport.list(SELECT + " ORDER BY sec.grade_level, sec.section_name, "
                + "FIELD(cs.day_of_week, 'MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'), "
                + "cs.start_time", ScheduleDao::map);
    }

    public List<ClassSchedule> findBySection(int sectionId) {
        return DaoSupport.list(SELECT + " WHERE cs.section_id = ?" + ORDER, ScheduleDao::map, sectionId);
    }

    public List<ClassSchedule> findBySectionAndDay(int sectionId, DayOfWeek day) {
        return DaoSupport.list(SELECT + " WHERE cs.section_id = ? AND cs.day_of_week = ? ORDER BY cs.start_time",
                ScheduleDao::map, sectionId, day);
    }

    public List<ClassSchedule> findByTeacher(int teacherId) {
        return DaoSupport.list(SELECT + " WHERE cs.teacher_id = ?" + ORDER, ScheduleDao::map, teacherId);
    }

    public List<ClassSchedule> findByTeacherAndDay(int teacherId, DayOfWeek day) {
        return DaoSupport.list(SELECT + " WHERE cs.teacher_id = ? AND cs.day_of_week = ? ORDER BY cs.start_time",
                ScheduleDao::map, teacherId, day);
    }

    /**
     * The "Within Class Schedule?" check from the flowchart. Returns the class that a scan at
     * the given day and time belongs to, or empty when the scan is out of bounds.
     * earlyMinutes lets students tap in a little before the class starts (e.g. 15).
     */
    public Optional<ClassSchedule> findActive(int sectionId, DayOfWeek day, LocalTime time, int earlyMinutes) {
        return DaoSupport.one(SELECT + """

                WHERE cs.section_id = ? AND cs.day_of_week = ?
                  AND ? >= SUBTIME(cs.start_time, SEC_TO_TIME(?))
                  AND ? <= cs.end_time
                ORDER BY cs.start_time""",
                ScheduleDao::map, sectionId, day, time, earlyMinutes * 60, time);
    }

    public boolean isWithinSchedule(int sectionId, DayOfWeek day, LocalTime time, int earlyMinutes) {
        return findActive(sectionId, day, time, earlyMinutes).isPresent();
    }

    /**
     * True if another class of the same section on the same day overlaps start-end.
     * Pass excludeScheduleId when editing an existing schedule (null when adding a new one).
     */
    public boolean hasOverlap(int sectionId, DayOfWeek day, LocalTime start, LocalTime end,
                              Integer excludeScheduleId) {
        return DaoSupport.count("""
                SELECT COUNT(*) FROM class_schedules
                WHERE section_id = ? AND day_of_week = ?
                  AND start_time < ? AND end_time > ?
                  AND schedule_id <> ?""",
                sectionId, day, end, start, excludeScheduleId == null ? 0 : excludeScheduleId) > 0;
    }

    public int count() {
        return DaoSupport.count("SELECT COUNT(*) FROM class_schedules");
    }

    // ---- Update ----------------------------------------------------------------------------

    public boolean update(ClassSchedule s) {
        return DaoSupport.update(
                "UPDATE class_schedules SET section_id = ?, teacher_id = ?, subject = ?, day_of_week = ?,"
                        + " start_time = ?, end_time = ?, room = ? WHERE schedule_id = ?",
                s.sectionId(), s.teacherId(), s.subject(), s.dayOfWeek(), s.startTime(), s.endTime(),
                s.room(), s.scheduleId()) > 0;
    }

    // ---- Delete ----------------------------------------------------------------------------

    public boolean delete(int scheduleId) {
        return DaoSupport.update("DELETE FROM class_schedules WHERE schedule_id = ?", scheduleId) > 0;
    }
}
