package com.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Shared JDBC helpers for SQL execution and row mapping. */
final class DaoSupport {

    private DaoSupport() {
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Stores enums by name to match MySQL ENUM columns. */
    static void bind(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            if (value instanceof Enum<?> e) {
                value = e.name();
            }
            ps.setObject(i + 1, value);
        }
    }

    static <T> List<T> list(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapper.map(rs));
                }
                return out;
            }
        }
    }

    static <T> List<T> list(String sql, RowMapper<T> mapper, Object... params) {
        try (Connection c = JDBC.getConnection()) {
            return list(c, sql, mapper, params);
        } catch (SQLException e) {
            throw new DaoException("Query failed", e);
        }
    }

    static <T> Optional<T> one(Connection c, String sql, RowMapper<T> mapper, Object... params) throws SQLException {
        List<T> rows = list(c, sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    static <T> Optional<T> one(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = list(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    /** First column of the first row as an int (for COUNT(*) queries). */
    static int count(String sql, Object... params) {
        return one(sql, rs -> rs.getInt(1), params).orElse(0);
    }

    static int update(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, params);
            return ps.executeUpdate();
        }
    }

    static int update(String sql, Object... params) {
        try (Connection c = JDBC.getConnection()) {
            return update(c, sql, params);
        } catch (SQLException e) {
            throw new DaoException("Update failed", e);
        }
    }

    /** INSERT and return the AUTO_INCREMENT id that MySQL generated. */
    static long insert(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    static long insert(String sql, Object... params) {
        try (Connection c = JDBC.getConnection()) {
            return insert(c, sql, params);
        } catch (SQLException e) {
            throw new DaoException("Insert failed", e);
        }
    }

    /** Reads nullable INT without converting NULL to 0. */
    static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    static LocalDate date(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    static LocalTime time(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalTime.class);
    }

    static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDateTime.class);
    }

    /** Escapes LIKE wildcards so search text stays literal. */
    static String like(String keyword) {
        String k = keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + k + "%";
    }

    /** Normalizes card UIDs to uppercase without whitespace. */
    static String normalizeUid(String uid) {
        return uid == null ? null : uid.replaceAll("\\s+", "").toUpperCase();
    }

    static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
