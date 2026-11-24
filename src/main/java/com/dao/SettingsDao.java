package com.dao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Key-value settings stored in the settings table (absence threshold and similar). */
public class SettingsDao {

    public static final String ABSENCE_THRESHOLD = "absence_threshold";
    public static final String ABSENCE_WARNING_OFFSET = "absence_warning_offset";
    public static final String LATE_GRACE_MINUTES = "late_grace_minutes";

    public Optional<String> get(String key) {
        return DaoSupport.one("SELECT setting_value FROM settings WHERE setting_key = ?",
                rs -> rs.getString(1), key);
    }

    public String get(String key, String fallback) {
        return get(key).orElse(fallback);
    }

    /** Reads a whole-number setting. Returns fallback when the key is missing or not a number. */
    public int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(get(key).orElse("").trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** All settings as key -> value, sorted by key. */
    public Map<String, String> findAll() {
        Map<String, String> all = new LinkedHashMap<>();
        DaoSupport.list("SELECT setting_key, setting_value FROM settings ORDER BY setting_key", rs -> {
            all.put(rs.getString(1), rs.getString(2));
            return null;
        });
        return all;
    }

    /** Creates the setting, or replaces its value if the key already exists. */
    public void set(String key, String value) {
        DaoSupport.update("""
                INSERT INTO settings (setting_key, setting_value) VALUES (?, ?)
                ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)""", key, value);
    }

    public void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    public boolean delete(String key) {
        return DaoSupport.update("DELETE FROM settings WHERE setting_key = ?", key) > 0;
    }

    // ---- Shortcuts used by the attendance logic ---------------------------------------------

    /** Maximum absences allowed before the drop/expulsion flag (default 10). */
    public int absenceThreshold() {
        return getInt(ABSENCE_THRESHOLD, 10);
    }

    /** How many absences before the threshold a student is flagged as "near" it (default 2). */
    public int warningOffset() {
        return getInt(ABSENCE_WARNING_OFFSET, 2);
    }

    /** Minutes after a class starts before a tap counts as LATE (default 15). */
    public int lateGraceMinutes() {
        return getInt(LATE_GRACE_MINUTES, 15);
    }
}
