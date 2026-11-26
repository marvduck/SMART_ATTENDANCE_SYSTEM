package com.dao;

import com.model.AttendanceRecord;
import com.model.AttendanceRow;
import com.model.AttendanceStatus;
import com.model.DailyCounts;
import com.model.StudentAttendanceSummary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Daily attendance, RFID tap processing, dashboard counts and reports. */
public class AttendanceDao {

    private static final String SELECT =
            "SELECT record_id, student_id, attendance_date, time_in, time_out, status, remarks"
                    + " FROM attendance_records";

    static AttendanceRecord map(ResultSet rs) throws SQLException {
        return new AttendanceRecord(
                rs.getInt("record_id"),
                rs.getInt("student_id"),
                DaoSupport.date(rs, "attendance_date"),
                DaoSupport.time(rs, "time_in"),
                DaoSupport.time(rs, "time_out"),
                AttendanceStatus.valueOf(rs.getString("status")),
                rs.getString("remarks"));
    }

    private static AttendanceRow mapRow(ResultSet rs) throws SQLException {
        String status = rs.getString("status");
        return new AttendanceRow(
                rs.getInt("student_id"),
                rs.getString("lrn"),
                rs.getString("full_name"),
                DaoSupport.time(rs, "time_in"),
                DaoSupport.time(rs, "time_out"),
                status == null ? null : AttendanceStatus.valueOf(status));
    }

    private static StudentAttendanceSummary mapSummary(ResultSet rs) throws SQLException {
        return new StudentAttendanceSummary(
                rs.getInt("student_id"),
                rs.getString("lrn"),
                rs.getString("full_name"),
                DaoSupport.getInteger(rs, "section_id"),
                rs.getInt("present_days"),
                rs.getInt("late_days"),
                rs.getInt("absent_days"),
                rs.getInt("excused_days"));
    }

    /** Records the first tap, filling time_in only when absent; status is PRESENT or LATE. */
    public void recordTimeIn(int studentId, LocalDate date, LocalTime time, AttendanceStatus status) {
        // MySQL assigns left to right; status must precede time_in.
        DaoSupport.update("""
                INSERT INTO attendance_records (student_id, attendance_date, time_in, status)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    status  = IF(time_in IS NULL, VALUES(status), status),
                    time_in = COALESCE(time_in, VALUES(time_in))""",
                studentId, date, time, status);
    }

    /** Records the exit tap. Returns false when the student has no time-in for that day. */
    public boolean recordTimeOut(int studentId, LocalDate date, LocalTime time) {
        return DaoSupport.update("""
                UPDATE attendance_records SET time_out = ?
                WHERE student_id = ? AND attendance_date = ? AND time_in IS NOT NULL""",
                time, studentId, date) > 0;
    }

    /** Creates or updates a day's status/remarks for manual corrections. */
    public void setStatus(int studentId, LocalDate date, AttendanceStatus status, String remarks) {
        DaoSupport.update("""
                INSERT INTO attendance_records (student_id, attendance_date, status, remarks)
                VALUES (?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE status = VALUES(status), remarks = VALUES(remarks)""",
                studentId, date, status, remarks);
    }

    /** Marks ENROLLED students without a daily record ABSENT; existing records are unchanged. */
    public int markAbsentees(LocalDate date, Integer sectionId) {
        if (sectionId == null) {
            return DaoSupport.update("""
                    INSERT IGNORE INTO attendance_records (student_id, attendance_date, status)
                    SELECT student_id, ?, 'ABSENT' FROM students WHERE status = 'ENROLLED'""", date);
        }
        return DaoSupport.update("""
                INSERT IGNORE INTO attendance_records (student_id, attendance_date, status)
                SELECT student_id, ?, 'ABSENT' FROM students WHERE status = 'ENROLLED' AND section_id = ?""",
                date, sectionId);
    }

    /** Returns record_id; duplicate student/date raises DaoException. */
    public int insert(AttendanceRecord r) {
        return (int) DaoSupport.insert(
                "INSERT INTO attendance_records (student_id, attendance_date, time_in, time_out, status, remarks)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                r.studentId(), r.attendanceDate(), r.timeIn(), r.timeOut(),
                r.status() == null ? AttendanceStatus.ABSENT : r.status(), r.remarks());
    }

    public Optional<AttendanceRecord> findById(int recordId) {
        return DaoSupport.one(SELECT + " WHERE record_id = ?", AttendanceDao::map, recordId);
    }

    /** Daily record; empty means no tap yet. */
    public Optional<AttendanceRecord> findByStudentAndDate(int studentId, LocalDate date) {
        return DaoSupport.one(SELECT + " WHERE student_id = ? AND attendance_date = ?",
                AttendanceDao::map, studentId, date);
    }

    public List<AttendanceRecord> findByDate(LocalDate date) {
        return DaoSupport.list(SELECT + " WHERE attendance_date = ? ORDER BY time_in", AttendanceDao::map, date);
    }

    /** Student history, newest first. */
    public List<AttendanceRecord> findByStudent(int studentId) {
        return DaoSupport.list(SELECT + " WHERE student_id = ? ORDER BY attendance_date DESC",
                AttendanceDao::map, studentId);
    }

    public List<AttendanceRecord> findByStudentBetween(int studentId, LocalDate from, LocalDate to) {
        return DaoSupport.list(SELECT + " WHERE student_id = ? AND attendance_date BETWEEN ? AND ?"
                + " ORDER BY attendance_date DESC", AttendanceDao::map, studentId, from, to);
    }

    /** Updates time_in, time_out, status and remarks of an existing record. */
    public boolean update(AttendanceRecord r) {
        return DaoSupport.update(
                "UPDATE attendance_records SET time_in = ?, time_out = ?, status = ?, remarks = ?"
                        + " WHERE record_id = ?",
                r.timeIn(), r.timeOut(), r.status(), r.remarks(), r.recordId()) > 0;
    }

    public boolean delete(int recordId) {
        return DaoSupport.update("DELETE FROM attendance_records WHERE record_id = ?", recordId) > 0;
    }

    /** Daily ENROLLED class list; null filters are optional and missing records have null status/times. */
    public List<AttendanceRow> findRows(LocalDate date, Integer sectionId, String keyword,
                                        AttendanceStatus status) {
        StringBuilder sql = new StringBuilder("""
                SELECT s.student_id, s.lrn, s.full_name, a.time_in, a.time_out, a.status
                FROM students s
                LEFT JOIN attendance_records a ON a.student_id = s.student_id AND a.attendance_date = ?
                WHERE s.status = 'ENROLLED'""");
        List<Object> params = new ArrayList<>();
        params.add(date);
        if (sectionId != null) {
            sql.append(" AND s.section_id = ?");
            params.add(sectionId);
        }
        if (DaoSupport.hasText(keyword)) {
            String k = DaoSupport.like(keyword);
            sql.append(" AND (s.full_name LIKE ? OR s.lrn LIKE ?)");
            params.add(k);
            params.add(k);
        }
        if (status != null) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY s.full_name");
        return DaoSupport.list(sql.toString(), AttendanceDao::mapRow, params.toArray());
    }

    /** Daily counts; sectionId null means the whole school. */
    public DailyCounts countsForDate(LocalDate date, Integer sectionId) {
        String sectionFilter = sectionId == null ? "" : " AND s.section_id = ?";
        String sql = """
                SELECT (SELECT COUNT(*) FROM students s WHERE s.status = 'ENROLLED'""" + sectionFilter + """
                ) AS enrolled,
                       COALESCE(SUM(a.status = 'PRESENT'), 0) AS present,
                       COALESCE(SUM(a.status = 'LATE'), 0)    AS late,
                       COALESCE(SUM(a.status = 'ABSENT'), 0)  AS absent,
                       COALESCE(SUM(a.status = 'EXCUSED'), 0) AS excused
                FROM attendance_records a
                JOIN students s ON s.student_id = a.student_id
                WHERE a.attendance_date = ? AND s.status = 'ENROLLED'""" + sectionFilter;
        List<Object> params = new ArrayList<>();
        if (sectionId != null) {
            params.add(sectionId);
        }
        params.add(date);
        if (sectionId != null) {
            params.add(sectionId);
        }
        return DaoSupport.one(sql, rs -> new DailyCounts(
                        rs.getInt("enrolled"), rs.getInt("present"), rs.getInt("late"),
                        rs.getInt("absent"), rs.getInt("excused")), params.toArray())
                .orElse(new DailyCounts(0, 0, 0, 0, 0));
    }

    /** Present/late percentages by date, oldest first. */
    public Map<LocalDate, Double> dailyRates(LocalDate from, LocalDate to, Integer sectionId) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.attendance_date AS att_date,
                       SUM(a.status IN ('PRESENT','LATE')) * 100.0 / COUNT(*) AS rate
                FROM attendance_records a
                JOIN students s ON s.student_id = a.student_id
                WHERE a.attendance_date BETWEEN ? AND ?""");
        List<Object> params = new ArrayList<>(List.of(from, to));
        if (sectionId != null) {
            sql.append(" AND s.section_id = ?");
            params.add(sectionId);
        }
        sql.append(" GROUP BY a.attendance_date ORDER BY a.attendance_date");
        Map<LocalDate, Double> rates = new LinkedHashMap<>();
        DaoSupport.list(sql.toString(), rs -> {
            rates.put(DaoSupport.date(rs, "att_date"), rs.getDouble("rate"));
            return null;
        }, params.toArray());
        return rates;
    }

    /** Student day totals; empty if the student does not exist. */
    public Optional<StudentAttendanceSummary> findSummary(int studentId) {
        return DaoSupport.one("SELECT * FROM v_student_attendance_summary WHERE student_id = ?",
                AttendanceDao::mapSummary, studentId);
    }

    /** All student totals, optionally limited to one section. */
    public List<StudentAttendanceSummary> findSummaries(Integer sectionId) {
        if (sectionId == null) {
            return DaoSupport.list("SELECT * FROM v_student_attendance_summary ORDER BY full_name",
                    AttendanceDao::mapSummary);
        }
        return DaoSupport.list(
                "SELECT * FROM v_student_attendance_summary WHERE section_id = ? ORDER BY full_name",
                AttendanceDao::mapSummary, sectionId);
    }

    /** ENROLLED students at minAbsences, worst first; use threshold - offset for warnings. */
    public List<StudentAttendanceSummary> findByMinAbsences(int minAbsences, Integer sectionId) {
        StringBuilder sql = new StringBuilder("""
                SELECT v.* FROM v_student_attendance_summary v
                JOIN students s ON s.student_id = v.student_id
                WHERE s.status = 'ENROLLED' AND v.absent_days >= ?""");
        List<Object> params = new ArrayList<>();
        params.add(minAbsences);
        if (sectionId != null) {
            sql.append(" AND v.section_id = ?");
            params.add(sectionId);
        }
        sql.append(" ORDER BY v.absent_days DESC, v.full_name");
        return DaoSupport.list(sql.toString(), AttendanceDao::mapSummary, params.toArray());
    }
}
