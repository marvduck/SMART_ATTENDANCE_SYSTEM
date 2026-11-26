package com.dao;

import java.sql.SQLException;

/** Unchecked SQLException wrapper. */
public class DaoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    public DaoException(String message, SQLException cause) {
        super(message + ": " + cause.getMessage(), cause);
    }

    /** Detects MySQL UNIQUE violations. */
    public boolean isDuplicateKey() {
        return getCause() instanceof SQLException e && e.getErrorCode() == MYSQL_DUPLICATE_ENTRY;
    }
}
