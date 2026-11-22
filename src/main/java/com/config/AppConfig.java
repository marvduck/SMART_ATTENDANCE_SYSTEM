package com.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties is missing from the classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
        for (String key : PROPS.stringPropertyNames()) {
            String override = System.getProperty(key);
            if (override != null) {
                PROPS.setProperty(key, override);
            }
        }
    }

    private AppConfig() {
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    public static String get(String key, String fallback) {
        return PROPS.getProperty(key, fallback);
    }

    public static int getInt(String key, int fallback) {
        try {
            return Integer.parseInt(PROPS.getProperty(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static String jdbcUrl() {
        String host = get("db.host", "localhost");
        int port = getInt("db.port", 3307);
        String name = get("db.name", "smart_attendance");
        return "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?useSSL=false"
                + "&allowPublicKeyRetrieval=true"
                + "&serverTimezone=Asia/Manila"
                + "&characterEncoding=UTF-8"
                + "&connectTimeout=5000";
    }
}