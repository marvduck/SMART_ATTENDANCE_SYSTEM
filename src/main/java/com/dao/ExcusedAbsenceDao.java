package com.dao;

import com.model.ExcusedAbsence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Excused absences; excuse and attendance changes commit in one transaction. */
public class ExcusedAbsenceDao {

    private static final String SELECT = """
            SELECT e.excused_id, e.student_id, e.absence_date, e.reason, e.marked_by, e.created_at, s.full_name
            FROM excused_absences e
            JOIN students s ON s.student_id = e.student_id""";

    static ExcusedAbsence map(ResultSet rs) throws SQLException {
        return new ExcusedAbsence(
                rs.getInt("excused_id"),
                rs.getInt("student_id"),
                DaoSupport.date(rs, "absence_date"),
                rs.getString("reason"),
                DaoSupport.getInteger(rs, "marked_by"),
                DaoSupport.dateTime(rs, "created_at"),
                rs.getString("full_name"));
    }

    /** Excuses a date; only ABSENT/missing attendance becomes EXCUSED, never PRESENT/LATE. */
    public int markExcused(int studentId, LocalDate date, String reason, Integer markedBy) {
        return JDBC.inTransaction(c -> {
            DaoSupport.update(c, """
                    INSERT INTO excused_absences (student_id, absence_date, reason, marked_by)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE reason = VALUES(reason), marked_by = VALUES(marked_by)""",
                    studentId, date, reason, markedBy);
            DaoSupport.update(c, """
                    INSERT INTO attendance_records (student_id, attendance_date, status, remarks)
                    VALUES (?, ?, 'EXCUSED', ?)
                    ON DUPLICATE KEY UPDATE
                        remarks = IF(status = 'ABSENT', VALUES(remarks), remarks),
                        status  = IF(status = 'ABSENT', 'EXCUSED', status)""",
                    studentId, date, reason);
            return DaoSupport.one(c,
                    "SELECT excused_id FROM excused_absences WHERE student_id = ? AND absence_date = ?",
                    rs -> rs.getInt(1), studentId, date).orElse(-1);
        });
    }

    public Optional<ExcusedAbsence> findById(int excusedId) {
        return DaoSupport.one(SELECT + " WHERE e.excused_id = ?", ExcusedAbsenceDao::map, excusedId);
    }

    public Optional<ExcusedAbsence> findByStudentAndDate(int studentId, LocalDate date) {
        return DaoSupport.one(SELECT + " WHERE e.student_id = ? AND e.absence_date = ?",
                ExcusedAbsenceDao::map, studentId, date);
    }

    /** Student excused days, newest first. */
    public List<ExcusedAbsence> findByStudent(int studentId) {
        return DaoSupport.list(SELECT + " WHERE e.student_id = ? ORDER BY e.absence_date DESC",
                ExcusedAbsenceDao::map, studentId);
    }

    public List<ExcusedAbsence> findByDate(LocalDate date) {
        return DaoSupport.list(SELECT + " WHERE e.absence_date = ? ORDER BY s.full_name",
                ExcusedAbsenceDao::map, date);
    }

    /** Section excuses in a date range. */
    public List<ExcusedAbsence> findBySection(int sectionId, LocalDate from, LocalDate to) {
        return DaoSupport.list(SELECT + " WHERE s.section_id = ? AND e.absence_date BETWEEN ? AND ?"
                + " ORDER BY e.absence_date DESC, s.full_name", ExcusedAbsenceDao::map, sectionId, from, to);
    }

    public List<ExcusedAbsence> findAll(int limit) {
        return DaoSupport.list(SELECT + " ORDER BY e.absence_date DESC, s.full_name LIMIT ?",
                ExcusedAbsenceDao::map, limit);
    }

    public int countForStudent(int studentId) {
        return DaoSupport.count("SELECT COUNT(*) FROM excused_absences WHERE student_id = ?", studentId);
    }

    /** Changes the reason text only. */
    public boolean updateReason(int excusedId, String reason) {
        return JDBC.inTransaction(c -> {
            int changed = DaoSupport.update(c, "UPDATE excused_absences SET reason = ? WHERE excused_id = ?",
                    reason, excusedId);
            DaoSupport.update(c, """
                    UPDATE attendance_records a
                    JOIN excused_absences e ON e.student_id = a.student_id AND e.absence_date = a.attendance_date
                    SET a.remarks = ?
                    WHERE e.excused_id = ? AND a.status = 'EXCUSED'""", reason, excusedId);
            return changed > 0;
        });
    }

    /** Removes an excuse; EXCUSED attendance reverts to ABSENT. */
    public boolean remove(int excusedId) {
        return JDBC.inTransaction(c -> {
            DaoSupport.update(c, """
                    UPDATE attendance_records a
                    JOIN excused_absences e ON e.student_id = a.student_id AND e.absence_date = a.attendance_date
                    SET a.status = 'ABSENT', a.remarks = NULL
                    WHERE e.excused_id = ? AND a.status = 'EXCUSED'""", excusedId);
            return DaoSupport.update(c, "DELETE FROM excused_absences WHERE excused_id = ?", excusedId) > 0;
        });
    }
}
