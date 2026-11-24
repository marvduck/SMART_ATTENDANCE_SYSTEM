package com.dao;

import com.model.Teacher;
import com.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// CRUD for teacher table
public class TeacherDao {

    private static final String SELECT =
            "SELECT teacher_id, user_id, employee_no, full_name, email, contact_no FROM teachers";

    static Teacher map(ResultSet rs) throws SQLException {
        return new Teacher(
                rs.getInt("teacher_id"),
                rs.getInt("user_id"),
                rs.getString("employee_no"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("contact_no"));
    }

    // Create
   public int insert(Teacher t) {
        return (int) DaoSupport.insert(
                "INSERT INTO teachers (user_id, employee_no, full_name, email, contact_no) VALUES (?, ?, ?, ?, ?)",
                t.userId(), t.employeeNo(), t.fullName(), t.email(), t.contactNo());
    }

    // Account creation
    public int insertWithAccount(User account, Teacher t) {
        return JDBC.inTransaction(c -> {
            int userId = new UserDao().insert(c, account);
            return (int) DaoSupport.insert(c,
                    "INSERT INTO teachers (user_id, employee_no, full_name, email, contact_no) VALUES (?, ?, ?, ?, ?)",
                    userId, t.employeeNo(), t.fullName(), t.email(), t.contactNo());
        });
    }

    // Read
    public Optional<Teacher> findById(int teacherId) {
        return DaoSupport.one(SELECT + " WHERE teacher_id = ?", TeacherDao::map, teacherId);
    }

    public Optional<Teacher> findByUserId(int userId) {
        return DaoSupport.one(SELECT + " WHERE user_id = ?", TeacherDao::map, userId);
    }

    public Optional<Teacher> findByEmployeeNo(String employeeNo) {
        return DaoSupport.one(SELECT + " WHERE employee_no = ?", TeacherDao::map, employeeNo);
    }

    public List<Teacher> findAll() {
        return DaoSupport.list(SELECT + " ORDER BY full_name", TeacherDao::map);
    }

    // Compare name, email
    public List<Teacher> search(String keyword) {
        String k = DaoSupport.like(keyword);
        return DaoSupport.list(
                SELECT + " WHERE full_name LIKE ? OR employee_no LIKE ? OR email LIKE ? ORDER BY full_name",
                TeacherDao::map, k, k, k);
    }

    public boolean existsByEmployeeNo(String employeeNo) {
        return DaoSupport.count("SELECT COUNT(*) FROM teachers WHERE employee_no = ?", employeeNo) > 0;
    }

    public int count() {
        return DaoSupport.count("SELECT COUNT(*) FROM teachers");
    }

    // Update

    public boolean update(Teacher t) {
        return DaoSupport.update(
                "UPDATE teachers SET employee_no = ?, full_name = ?, email = ?, contact_no = ? WHERE teacher_id = ?",
                t.employeeNo(), t.fullName(), t.email(), t.contactNo(), t.teacherId()) > 0;
    }

    // Delete

    // Deletes teacher account but not their login account
    public boolean delete(int teacherId) {
        return DaoSupport.update("DELETE FROM teachers WHERE teacher_id = ?", teacherId) > 0;
    }

    // Deletes the teacher and their login account
    public boolean deleteWithAccount(int teacherId) {
        return JDBC.inTransaction(c -> {
            Optional<Integer> userId = DaoSupport.one(c,
                    "SELECT user_id FROM teachers WHERE teacher_id = ?", rs -> rs.getInt(1), teacherId);
            if (userId.isEmpty()) {
                return false;
            }

            return DaoSupport.update(c, "DELETE FROM users WHERE user_id = ?", userId.get()) > 0;
        });
    }
}
