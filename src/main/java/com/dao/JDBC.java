// Central class for storing and setting JDBC options.
// Connection details come from src/main/resources/db.properties (see AppConfig)
// and connections are pooled with HikariCP.

package com.dao;

import com.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class JDBC {

    private static HikariDataSource pool;

    private JDBC() {
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T run(Connection connection) throws SQLException;
    }

    // Hikari pooling
    private static synchronized HikariDataSource pool() {
        if (pool == null || pool.isClosed()) {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(AppConfig.jdbcUrl());
            cfg.setUsername(AppConfig.get("db.user", "root"));
            cfg.setPassword(AppConfig.get("db.password", ""));
            cfg.setMaximumPoolSize(AppConfig.getInt("db.pool.size", 10));
            cfg.setConnectionTimeout(5000);
            cfg.setPoolName("smart-attendance");
            pool = new HikariDataSource(cfg);
        }
        return pool;
    }

    // Borrow pool connection
    public static Connection getConnection() throws SQLException {
        return pool().getConnection();
    }

    // Test connection valid or not
    public static boolean testConnection() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    // Runs statements as single commit
    public static <T> T inTransaction(TxWork<T> work) {
        try (Connection c = getConnection()) {
            boolean previous = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(previous);
            }
        } catch (SQLException e) {
            throw new DaoException("Transaction failed", e);
        }
    }

    // Close pool
    public static synchronized void close() {
        if (pool != null) {
            pool.close();
            pool = null;
        }
    }
}
