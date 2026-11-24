package com.dao;

import java.io.Serial;
import java.sql.SQLException;

/** Unchecked wrapper for SQLException so service and UI code does not need try/catch everywhere. */
public class DaoException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;

    public DaoException(String message, SQLException cause) {
        super(message + ": " + cause.getMessage(), cause);
    }

    /** True when the failure was a UNIQUE violation (duplicate username, LRN, card UID, ...). */
    public boolean isDuplicateKey() {
        return getCause() instanceof SQLException e && e.getErrorCode() == MYSQL_DUPLICATE_ENTRY;
    }
}
