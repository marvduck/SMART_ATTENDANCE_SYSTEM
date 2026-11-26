package com.dao;

import com.model.CardStatus;
import com.model.RfidCard;
import com.model.Student;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** RFID cards; UIDs are stored and compared uppercase without whitespace. */
public class RfidCardDao {

    private static final String SELECT = """
            SELECT c.card_id, c.card_uid, c.student_id, c.status, c.assigned_at, s.full_name
            FROM rfid_cards c
            JOIN students s ON s.student_id = c.student_id""";

    static RfidCard map(ResultSet rs) throws SQLException {
        return new RfidCard(
                rs.getInt("card_id"),
                rs.getString("card_uid"),
                rs.getInt("student_id"),
                CardStatus.valueOf(rs.getString("status")),
                DaoSupport.dateTime(rs, "assigned_at"),
                rs.getString("full_name"));
    }

    /** Assigns a card; duplicate UIDs raise DaoException. */
    public int assign(String cardUid, int studentId) {
        return (int) DaoSupport.insert("INSERT INTO rfid_cards (card_uid, student_id) VALUES (?, ?)",
                DaoSupport.normalizeUid(cardUid), studentId);
    }

    /** Atomically retires active cards with oldCardStatus and registers a replacement. */
    public int replaceCard(int studentId, String newCardUid, CardStatus oldCardStatus) {
        return JDBC.inTransaction(c -> {
            DaoSupport.update(c,
                    "UPDATE rfid_cards SET status = ? WHERE student_id = ? AND status = 'ACTIVE'",
                    oldCardStatus, studentId);
            return (int) DaoSupport.insert(c,
                    "INSERT INTO rfid_cards (card_uid, student_id) VALUES (?, ?)",
                    DaoSupport.normalizeUid(newCardUid), studentId);
        });
    }

    public Optional<RfidCard> findById(int cardId) {
        return DaoSupport.one(SELECT + " WHERE c.card_id = ?", RfidCardDao::map, cardId);
    }

    /** Finds a card by UID regardless of status. */
    public Optional<RfidCard> findByUid(String cardUid) {
        return DaoSupport.one(SELECT + " WHERE c.card_uid = ?", RfidCardDao::map,
                DaoSupport.normalizeUid(cardUid));
    }

    /** Returns the owner only for an ACTIVE card and ENROLLED student. */
    public Optional<Student> findStudentByActiveUid(String cardUid) {
        return DaoSupport.one(StudentDao.SELECT + """

                JOIN rfid_cards c ON c.student_id = s.student_id
                WHERE c.card_uid = ? AND c.status = 'ACTIVE' AND s.status = 'ENROLLED'""",
                StudentDao::map, DaoSupport.normalizeUid(cardUid));
    }

    public List<RfidCard> findByStudent(int studentId) {
        return DaoSupport.list(SELECT + " WHERE c.student_id = ? ORDER BY c.assigned_at DESC",
                RfidCardDao::map, studentId);
    }

    public Optional<RfidCard> findActiveByStudent(int studentId) {
        return DaoSupport.one(SELECT + " WHERE c.student_id = ? AND c.status = 'ACTIVE'"
                + " ORDER BY c.assigned_at DESC", RfidCardDao::map, studentId);
    }

    public List<RfidCard> findAll() {
        return DaoSupport.list(SELECT + " ORDER BY s.full_name, c.assigned_at DESC", RfidCardDao::map);
    }

    public List<RfidCard> findByStatus(CardStatus status) {
        return DaoSupport.list(SELECT + " WHERE c.status = ? ORDER BY s.full_name",
                RfidCardDao::map, status);
    }

    /** Searches card UID or student name. */
    public List<RfidCard> search(String keyword) {
        String k = DaoSupport.like(keyword);
        return DaoSupport.list(SELECT + " WHERE c.card_uid LIKE ? OR s.full_name LIKE ? ORDER BY s.full_name",
                RfidCardDao::map, k, k);
    }

    public boolean existsByUid(String cardUid) {
        return DaoSupport.count("SELECT COUNT(*) FROM rfid_cards WHERE card_uid = ?",
                DaoSupport.normalizeUid(cardUid)) > 0;
    }

    public int countByStatus(CardStatus status) {
        return DaoSupport.count("SELECT COUNT(*) FROM rfid_cards WHERE status = ?", status.name());
    }

    public boolean updateStatus(int cardId, CardStatus status) {
        return DaoSupport.update("UPDATE rfid_cards SET status = ? WHERE card_id = ?", status, cardId) > 0;
    }

    /** Deletes the card but keeps scan-log UID text; prefer DISABLED to retain history. */
    public boolean delete(int cardId) {
        return DaoSupport.update("DELETE FROM rfid_cards WHERE card_id = ?", cardId) > 0;
    }
}
