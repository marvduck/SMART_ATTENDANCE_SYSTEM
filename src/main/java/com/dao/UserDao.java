package com.dao;

import com.model.Role;
import com.model.User;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

// CRUD for users table
public class UserDao {

    private static final String SELECT =
            "SELECT user_id, username, password_hash, role, is_active, created_at, last_login FROM users";

    static User map(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("password_hash"),
                Role.valueOf(rs.getString("role")),
                rs.getBoolean("is_active"),
                DaoSupport.dateTime(rs, "created_at"),
                DaoSupport.dateTime(rs, "last_login"));
    }

    // Create
    public int insert(User u) {
        return (int) DaoSupport.insert(
                "INSERT INTO users (username, password_hash, role, is_active) VALUES (?, ?, ?, ?)",
                u.username(), u.passwordHash(), u.role(), u.active());
    }

    int insert(Connection c, User u) throws SQLException {
        return (int) DaoSupport.insert(c,
                "INSERT INTO users (username, password_hash, role, is_active) VALUES (?, ?, ?, ?)",
                u.username(), u.passwordHash(), u.role(), u.active());
    }

    // Read
    public Optional<User> findById(int userId) {
        return DaoSupport.one(SELECT + " WHERE user_id = ?", UserDao::map, userId);
    }

    public Optional<User> findByUsername(String username) {
        return DaoSupport.one(SELECT + " WHERE username = ?", UserDao::map, username);
    }

    public List<User> findAll() {
        return DaoSupport.list(SELECT + " ORDER BY username", UserDao::map);
    }

    public List<User> findByRole(Role role) {
        return DaoSupport.list(SELECT + " WHERE role = ? ORDER BY username", UserDao::map, role);
    }

    public List<User> search(String keyword) {
        return DaoSupport.list(SELECT + " WHERE username LIKE ? ORDER BY username",
                UserDao::map, DaoSupport.like(keyword));
    }

    public boolean existsByUsername(String username) {
        return DaoSupport.count("SELECT COUNT(*) FROM users WHERE username = ?", username) > 0;
    }

    public int count() {
        return DaoSupport.count("SELECT COUNT(*) FROM users");
    }

    // Update
    public boolean update(User u) {
        return DaoSupport.update(
                "UPDATE users SET username = ?, role = ?, is_active = ? WHERE user_id = ?",
                u.username(), u.role(), u.active(), u.userId()) > 0;
    }

    public boolean updatePasswordHash(int userId, String passwordHash) {
        return DaoSupport.update("UPDATE users SET password_hash = ? WHERE user_id = ?",
                passwordHash, userId) > 0;
    }

    public boolean setActive(int userId, boolean active) {
        return DaoSupport.update("UPDATE users SET is_active = ? WHERE user_id = ?", active, userId) > 0;
    }

    // Successful login
    public void updateLastLogin(int userId) {
        DaoSupport.update("UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
    }

    // Delete
    public boolean delete(int userId) {
        return DaoSupport.update("DELETE FROM users WHERE user_id = ?", userId) > 0;
    }
}
