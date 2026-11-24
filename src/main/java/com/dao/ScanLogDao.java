package com.dao;

import com.model.ScanLog;
import com.model.ScanResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Audit trail of every raw RFID tap (valid or not) in the scan_logs table. */
public class ScanLogDao {

    private static final String SELECT = """
            SELECT l.log_id, l.card_uid, l.student_id, l.scan_time, l.result, s.full_name
            FROM scan_logs l
            LEFT JOIN students s ON s.student_id = l.student_id""";

    static ScanLog map(ResultSet rs) throws SQLException {
        return new ScanLog(
                rs.getLong("log_id"),
                rs.getString("card_uid"),
                DaoSupport.getInteger(rs, "student_id"),
                DaoSupport.dateTime(rs, "scan_time"),
                ScanResult.valueOf(rs.getString("result")),
                rs.getString("full_name"));
    }

    // ---- Create ----------------------------------------------------------------------------

    /**
     * Logs one tap. studentId is null for an unregistered card. Returns the new log_id.
     * The time is the current server time.
     */
    public long insert(String cardUid, Integer studentId, ScanResult result) {
        return DaoSupport.insert(
                "INSERT INTO scan_logs (card_uid, student_id, scan_time, result) VALUES (?, ?, ?, ?)",
                DaoSupport.normalizeUid(cardUid), studentId, LocalDateTime.now(), result);
    }

    // ---- Read ------------------------------------------------------------------------------

    /** The latest taps, newest first (the "Live RFID activity" table). */
    public List<ScanLog> findRecent(int limit) {
        return DaoSupport.list(SELECT + " ORDER BY l.scan_time DESC, l.log_id DESC LIMIT ?",
                ScanLogDao::map, limit);
    }

    /** The latest taps of one section's students, newest first (teacher view). */
    public List<ScanLog> findRecentBySection(int sectionId, int limit) {
        return DaoSupport.list(SELECT + " WHERE s.section_id = ? ORDER BY l.scan_time DESC, l.log_id DESC LIMIT ?",
                ScanLogDao::map, sectionId, limit);
    }

    public List<ScanLog> findByStudent(int studentId, int limit) {
        return DaoSupport.list(SELECT + " WHERE l.student_id = ? ORDER BY l.scan_time DESC, l.log_id DESC LIMIT ?",
                ScanLogDao::map, studentId, limit);
    }

    public List<ScanLog> findByDate(LocalDate date) {
        return DaoSupport.list(SELECT + " WHERE l.scan_time >= ? AND l.scan_time < ? ORDER BY l.scan_time DESC",
                ScanLogDao::map, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public List<ScanLog> findByResult(ScanResult result, int limit) {
        return DaoSupport.list(SELECT + " WHERE l.result = ? ORDER BY l.scan_time DESC, l.log_id DESC LIMIT ?",
                ScanLogDao::map, result, limit);
    }

    public int countByResultOnDate(ScanResult result, LocalDate date) {
        return DaoSupport.count(
                "SELECT COUNT(*) FROM scan_logs WHERE result = ? AND scan_time >= ? AND scan_time < ?",
                result.name(), date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    // ---- Delete ----------------------------------------------------------------------------

    /** Housekeeping: removes logs older than the given time. Returns how many were deleted. */
    public int deleteOlderThan(LocalDateTime cutoff) {
        return DaoSupport.update("DELETE FROM scan_logs WHERE scan_time < ?", cutoff);
    }
}
