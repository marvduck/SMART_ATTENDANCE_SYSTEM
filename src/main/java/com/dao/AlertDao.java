package com.dao;

import com.model.Alert;
import com.model.AlertType;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Administrator alerts: invalid cards, out-of-bounds scans and absence thresholds. */
public class AlertDao {

    private static final String SELECT = """
            SELECT a.alert_id, a.alert_type, a.student_id, a.card_uid, a.message, a.is_read, a.created_at,
                   s.full_name
            FROM alerts a
            LEFT JOIN students s ON s.student_id = a.student_id""";

    static Alert map(ResultSet rs) throws SQLException {
        return new Alert(
                rs.getInt("alert_id"),
                AlertType.valueOf(rs.getString("alert_type")),
                DaoSupport.getInteger(rs, "student_id"),
                rs.getString("card_uid"),
                rs.getString("message"),
                rs.getBoolean("is_read"),
                DaoSupport.dateTime(rs, "created_at"),
                rs.getString("full_name"));
    }

    // ---- Create ----------------------------------------------------------------------------

    /**
     * Raises a new, unread alert. studentId and cardUid may be null (an unregistered card has no
     * student). Returns the new alert_id.
     */
    public int log(AlertType type, Integer studentId, String cardUid, String message) {
        return (int) DaoSupport.insert(
                "INSERT INTO alerts (alert_type, student_id, card_uid, message) VALUES (?, ?, ?, ?)",
                type, studentId, DaoSupport.normalizeUid(cardUid), message);
    }

    // ---- Read ------------------------------------------------------------------------------

    public Optional<Alert> findById(int alertId) {
        return DaoSupport.one(SELECT + " WHERE a.alert_id = ?", AlertDao::map, alertId);
    }

    /** The latest alerts, newest first. */
    public List<Alert> findRecent(int limit) {
        return DaoSupport.list(SELECT + " ORDER BY a.created_at DESC, a.alert_id DESC LIMIT ?",
                AlertDao::map, limit);
    }

    public List<Alert> findUnread() {
        return DaoSupport.list(SELECT + " WHERE a.is_read = FALSE ORDER BY a.created_at DESC, a.alert_id DESC",
                AlertDao::map);
    }

    public List<Alert> findByType(AlertType type, int limit) {
        return DaoSupport.list(SELECT + " WHERE a.alert_type = ? ORDER BY a.created_at DESC, a.alert_id DESC LIMIT ?",
                AlertDao::map, type, limit);
    }

    public List<Alert> findByStudent(int studentId) {
        return DaoSupport.list(SELECT + " WHERE a.student_id = ? ORDER BY a.created_at DESC, a.alert_id DESC",
                AlertDao::map, studentId);
    }

    /** The red badge number ("5 open"). */
    public int countUnread() {
        return DaoSupport.count("SELECT COUNT(*) FROM alerts WHERE is_read = FALSE");
    }

    public int countUnreadByType(AlertType type) {
        return DaoSupport.count("SELECT COUNT(*) FROM alerts WHERE is_read = FALSE AND alert_type = ?",
                type.name());
    }

    /**
     * True if this student already has an unread alert of this type. Check it before raising a
     * threshold alert so the admin is not notified again on every tap.
     */
    public boolean existsUnread(int studentId, AlertType type) {
        return DaoSupport.count(
                "SELECT COUNT(*) FROM alerts WHERE student_id = ? AND alert_type = ? AND is_read = FALSE",
                studentId, type.name()) > 0;
    }

    // ---- Update ----------------------------------------------------------------------------

    public boolean markRead(int alertId) {
        return DaoSupport.update("UPDATE alerts SET is_read = TRUE WHERE alert_id = ?", alertId) > 0;
    }

    /** Returns how many alerts were marked read. */
    public int markAllRead() {
        return DaoSupport.update("UPDATE alerts SET is_read = TRUE WHERE is_read = FALSE");
    }

    // ---- Delete ----------------------------------------------------------------------------

    public boolean delete(int alertId) {
        return DaoSupport.update("DELETE FROM alerts WHERE alert_id = ?", alertId) > 0;
    }

    /** Clears alerts that were already read. Returns how many were deleted. */
    public int deleteRead() {
        return DaoSupport.update("DELETE FROM alerts WHERE is_read = TRUE");
    }

    /** Housekeeping: removes alerts older than the given time. */
    public int deleteOlderThan(LocalDateTime cutoff) {
        return DaoSupport.update("DELETE FROM alerts WHERE created_at < ?", cutoff);
    }
}
