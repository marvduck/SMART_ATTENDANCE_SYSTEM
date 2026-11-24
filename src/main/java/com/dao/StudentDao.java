package com.dao;

import com.model.Student;
import com.model.StudentStatus;
import com.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// CRUD for student table
public class StudentDao {

    static final String SELECT = """
            SELECT s.student_id, s.user_id, s.lrn, s.full_name, s.grade_level, s.section_id,
                   s.guardian_contact, s.status, sec.section_name
            FROM students s
            LEFT JOIN sections sec ON sec.section_id = s.section_id""";

    static Student map(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("student_id"),
                DaoSupport.getInteger(rs, "user_id"),
                rs.getString("lrn"),
                rs.getString("full_name"),
                rs.getInt("grade_level"),
                DaoSupport.getInteger(rs, "section_id"),
                rs.getString("guardian_contact"),
                StudentStatus.valueOf(rs.getString("status")),
                rs.getString("section_name"));
    }

    // Create
    public int insert(Student s) {
        return (int) DaoSupport.insert(
                "INSERT INTO students (user_id, lrn, full_name, grade_level, section_id, guardian_contact, status)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                s.userId(), s.lrn(), s.fullName(), s.gradeLevel(), s.sectionId(), s.guardianContact(),
                s.status() == null ? StudentStatus.ENROLLED : s.status());
    }

    // Account creation
    public int insertWithAccount(User account, Student s) {
        return JDBC.inTransaction(c -> {
            int userId = new UserDao().insert(c, account);
            return (int) DaoSupport.insert(c,
                    "INSERT INTO students (user_id, lrn, full_name, grade_level, section_id, guardian_contact, status)"
                            + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                    userId, s.lrn(), s.fullName(), s.gradeLevel(), s.sectionId(), s.guardianContact(),
                    s.status() == null ? StudentStatus.ENROLLED : s.status());
        });
    }

    // Read
    public Optional<Student> findById(int studentId) {
        return DaoSupport.one(SELECT + " WHERE s.student_id = ?", StudentDao::map, studentId);
    }

    public Optional<Student> findByLrn(String lrn) {
        return DaoSupport.one(SELECT + " WHERE s.lrn = ?", StudentDao::map, lrn);
    }

    // Find user from student profile
    public Optional<Student> findByUserId(int userId) {
        return DaoSupport.one(SELECT + " WHERE s.user_id = ?", StudentDao::map, userId);
    }

    public List<Student> findAll() {
        return DaoSupport.list(SELECT + " ORDER BY s.full_name", StudentDao::map);
    }

    public List<Student> findBySection(int sectionId) {
        return DaoSupport.list(SELECT + " WHERE s.section_id = ? ORDER BY s.full_name",
                StudentDao::map, sectionId);
    }

    public List<Student> findByGrade(int gradeLevel) {
        return DaoSupport.list(SELECT + " WHERE s.grade_level = ? ORDER BY s.full_name",
                StudentDao::map, gradeLevel);
    }

    // Search and filter for student directory
    public List<Student> search(String keyword, Integer gradeLevel, Integer sectionId, StudentStatus status) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (DaoSupport.hasText(keyword)) {
            String k = DaoSupport.like(keyword);
            sql.append(" AND (s.full_name LIKE ? OR s.lrn LIKE ?)");
            params.add(k);
            params.add(k);
        }
        if (gradeLevel != null) {
            sql.append(" AND s.grade_level = ?");
            params.add(gradeLevel);
        }
        if (sectionId != null) {
            sql.append(" AND s.section_id = ?");
            params.add(sectionId);
        }
        if (status != null) {
            sql.append(" AND s.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY s.full_name");
        return DaoSupport.list(sql.toString(), StudentDao::map, params.toArray());
    }

    // Unassigned students for RFID cards
    public List<Student> findWithoutActiveCard() {
        return DaoSupport.list(SELECT + """

                WHERE s.status = 'ENROLLED'
                  AND NOT EXISTS (SELECT 1 FROM rfid_cards c
                                  WHERE c.student_id = s.student_id AND c.status = 'ACTIVE')
                ORDER BY s.full_name""", StudentDao::map);
    }

    public boolean existsByLrn(String lrn) {
        return DaoSupport.count("SELECT COUNT(*) FROM students WHERE lrn = ?", lrn) > 0;
    }

    public int count() {
        return DaoSupport.count("SELECT COUNT(*) FROM students");
    }

    public int countByStatus(StudentStatus status) {
        return DaoSupport.count("SELECT COUNT(*) FROM students WHERE status = ?", status.name());
    }

    // Update
    public boolean update(Student s) {
        return DaoSupport.update(
                "UPDATE students SET lrn = ?, full_name = ?, grade_level = ?, section_id = ?,"
                        + " guardian_contact = ?, status = ? WHERE student_id = ?",
                s.lrn(), s.fullName(), s.gradeLevel(), s.sectionId(), s.guardianContact(), s.status(),
                s.studentId()) > 0;
    }

    public boolean updateStatus(int studentId, StudentStatus status) {
        return DaoSupport.update("UPDATE students SET status = ? WHERE student_id = ?", status, studentId) > 0;
    }

    public boolean updateSection(int studentId, Integer sectionId) {
        return DaoSupport.update("UPDATE students SET section_id = ? WHERE student_id = ?",
                sectionId, studentId) > 0;
    }

    // Link login account to student
    public boolean assignAccount(int studentId, Integer userId) {
        return DaoSupport.update("UPDATE students SET user_id = ? WHERE student_id = ?", userId, studentId) > 0;
    }

    // Delete
    public boolean delete(int studentId) {
        return DaoSupport.update("DELETE FROM students WHERE student_id = ?", studentId) > 0;
    }
}
