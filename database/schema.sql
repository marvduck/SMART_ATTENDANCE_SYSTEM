-- SMART ATTENDANCE SYSTEM - DATABASE SCHEMA

DROP DATABASE IF EXISTS smart_attendance;
CREATE DATABASE smart_attendance
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE smart_attendance;

-- TABLES
-- 1. USERS  (login accounts for Admin / Teacher / Student)
CREATE TABLE users (
    user_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,              -- BCrypt hash (60 chars), never plain text
    role           ENUM('ADMIN','TEACHER','STUDENT') NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login     TIMESTAMP    NULL
) ENGINE=InnoDB;

-- 2. TEACHERS
CREATE TABLE teachers (
    teacher_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id      INT UNSIGNED NOT NULL UNIQUE,
    employee_no  VARCHAR(20)  NOT NULL UNIQUE,
    full_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NULL,
    contact_no   VARCHAR(20)  NULL,
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 3. SECTIONS
CREATE TABLE sections (
    section_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    section_name  VARCHAR(50) NOT NULL,                -- e.g. 'TechPro C'
    grade_level   TINYINT     NOT NULL,                -- 11 or 12
    strand        VARCHAR(30) NULL,                    -- e.g. 'TVL-ICT'
    school_year   VARCHAR(9)  NOT NULL,                -- e.g. '2026-2027'
    adviser_id    INT UNSIGNED NULL,
    UNIQUE KEY uq_section (section_name, grade_level, school_year),
    CONSTRAINT fk_section_adviser FOREIGN KEY (adviser_id)
        REFERENCES teachers(teacher_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 4. STUDENTS
CREATE TABLE students (
    student_id   INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id      INT UNSIGNED NULL UNIQUE,             -- login account (optional until created)
    lrn          CHAR(12)     NOT NULL UNIQUE,         -- Learner Reference Number
    full_name    VARCHAR(100) NOT NULL,
    grade_level  TINYINT      NOT NULL,
    section_id   INT UNSIGNED NULL,
    guardian_contact VARCHAR(20) NULL,
    status       ENUM('ENROLLED','DROPPED','TRANSFERRED','GRADUATED') NOT NULL DEFAULT 'ENROLLED',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_name (full_name),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE SET NULL,
    CONSTRAINT fk_student_section FOREIGN KEY (section_id)
        REFERENCES sections(section_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5. RFID CARDS  (one active card per student)
CREATE TABLE rfid_cards (
    card_id      INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    card_uid     VARCHAR(20)  NOT NULL UNIQUE,         -- UID from RC522, e.g. 'A1B2C3D4'
    student_id   INT UNSIGNED NOT NULL,
    status       ENUM('ACTIVE','LOST','DISABLED') NOT NULL DEFAULT 'ACTIVE',
    assigned_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_card_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. CLASS SCHEDULES  (used for the "Within Class Schedule?" check)
CREATE TABLE class_schedules (
    schedule_id  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    section_id   INT UNSIGNED NOT NULL,
    teacher_id   INT UNSIGNED NULL,
    subject      VARCHAR(80)  NOT NULL,
    day_of_week  ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY') NOT NULL,
    start_time   TIME NOT NULL,
    end_time     TIME NOT NULL,
    room         VARCHAR(30) NULL,
    CONSTRAINT chk_time_order CHECK (end_time > start_time),
    INDEX idx_schedule_lookup (section_id, day_of_week, start_time, end_time),
    CONSTRAINT fk_sched_section FOREIGN KEY (section_id)
        REFERENCES sections(section_id) ON DELETE CASCADE,
    CONSTRAINT fk_sched_teacher FOREIGN KEY (teacher_id)
        REFERENCES teachers(teacher_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 7. ATTENDANCE RECORDS  (one row per student per day)
CREATE TABLE attendance_records (
    record_id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    student_id       INT UNSIGNED NOT NULL,
    attendance_date  DATE NOT NULL,
    time_in          TIME NULL,
    time_out         TIME NULL,
    status           ENUM('PRESENT','LATE','ABSENT','EXCUSED') NOT NULL DEFAULT 'ABSENT',
    remarks          VARCHAR(255) NULL,
    UNIQUE KEY uq_student_day (student_id, attendance_date),
    INDEX idx_date (attendance_date),
    CONSTRAINT fk_att_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 8. SCAN LOGS  (every raw tap, valid or not - audit trail)
CREATE TABLE scan_logs (
    log_id      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    card_uid    VARCHAR(20)  NOT NULL,
    student_id  INT UNSIGNED NULL,                     -- NULL when the card is unregistered
    scan_time   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result      ENUM('TIME_IN','TIME_OUT','INVALID_CARD','OUT_OF_BOUNDS') NOT NULL,
    INDEX idx_scan_time (scan_time),
    CONSTRAINT fk_scan_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 9. EXCUSED ABSENCES  (marked by teachers)
CREATE TABLE excused_absences (
    excused_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    student_id    INT UNSIGNED NOT NULL,
    absence_date  DATE NOT NULL,
    reason        VARCHAR(255) NOT NULL,
    marked_by     INT UNSIGNED NULL,                   -- teacher's user_id
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_excused (student_id, absence_date),
    CONSTRAINT fk_exc_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE,
    CONSTRAINT fk_exc_marker FOREIGN KEY (marked_by)
        REFERENCES users(user_id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 10. ALERTS  (invalid card, out-of-bounds, absence threshold)
CREATE TABLE alerts (
    alert_id    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    alert_type  ENUM('INVALID_CARD','OUT_OF_BOUNDS','THRESHOLD_NEAR','THRESHOLD_REACHED') NOT NULL,
    student_id  INT UNSIGNED NULL,
    card_uid    VARCHAR(20)  NULL,
    message     VARCHAR(255) NOT NULL,
    is_read     BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alert_unread (is_read, created_at),
    CONSTRAINT fk_alert_student FOREIGN KEY (student_id)
        REFERENCES students(student_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 11. SETTINGS  (key-value config, e.g. absence threshold)
CREATE TABLE settings (
    setting_key    VARCHAR(50)  PRIMARY KEY,
    setting_value  VARCHAR(100) NOT NULL,
    description    VARCHAR(255) NULL
) ENGINE=InnoDB;

--  VIEW: absence summary per student (feeds Threshold list + dashboards)
CREATE VIEW v_student_attendance_summary AS
SELECT  s.student_id,
        s.lrn,
        s.full_name,
        s.section_id,
        SUM(a.status = 'PRESENT') AS present_days,
        SUM(a.status = 'LATE')    AS late_days,
        SUM(a.status = 'ABSENT')  AS absent_days,
        SUM(a.status = 'EXCUSED') AS excused_days
FROM students s
LEFT JOIN attendance_records a ON a.student_id = s.student_id
GROUP BY s.student_id, s.lrn, s.full_name, s.section_id;

--  SAMPLE DATA  (for testing only - delete before the final defense)
--  NOTE: password_hash values are PLACEHOLDERS. Generate real BCrypt
--  hashes from Java (BCrypt.hashpw("yourPassword", BCrypt.gensalt()))
--  and replace them, or the login module will reject these accounts.
INSERT INTO settings (setting_key, setting_value, description) VALUES
    ('absence_threshold',       '10', 'Max allowed absences before drop/expulsion flag'),
    ('absence_warning_offset',  '2',  'Warn when student is this many absences from threshold'),
    ('late_grace_minutes',      '15', 'Minutes after start time before a student is marked LATE');

INSERT INTO users (username, password_hash, role) VALUES
    ('admin',    '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH_000000000000000000000000', 'ADMIN'),
    ('t.delossantos', '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH_000000000000000000000000', 'TEACHER'),
    ('s.sample1', '$2a$10$REPLACE_WITH_REAL_BCRYPT_HASH_000000000000000000000000', 'STUDENT');

INSERT INTO teachers (user_id, employee_no, full_name, email) VALUES
    (2, 'T-0001', 'Ryan John D. Delos Santos', 'rjdelossantos@example.com');

INSERT INTO sections (section_name, grade_level, strand, school_year, adviser_id) VALUES
    ('TechPro C', 11, 'TVL-ICT', '2026-2027', 1);

INSERT INTO students (user_id, lrn, full_name, grade_level, section_id) VALUES
    (3,    '123456789012', 'Sample Student One', 11, 1),
    (NULL, '123456789013', 'Sample Student Two', 11, 1);

INSERT INTO rfid_cards (card_uid, student_id) VALUES
    ('A1B2C3D4', 1),
    ('E5F6A7B8', 2);

INSERT INTO class_schedules (section_id, teacher_id, subject, day_of_week, start_time, end_time, room) VALUES
    (1, 1, 'Computer Programming (Java)', 'MONDAY',    '07:30:00', '09:30:00', 'ICT Lab'),
    (1, 1, 'Computer Programming (Java)', 'WEDNESDAY', '07:30:00', '09:30:00', 'ICT Lab');

INSERT INTO attendance_records (student_id, attendance_date, time_in, time_out, status) VALUES
    (1, CURDATE(), '07:25:00', '16:00:00', 'PRESENT'),
    (2, CURDATE(), NULL,       NULL,       'ABSENT');

--  EXAMPLE QUERIES YOUR DAO CLASSES WILL USE
-- Is the card registered?
--   SELECT s.* FROM rfid_cards c JOIN students s ON s.student_id = c.student_id
--   WHERE c.card_uid = ? AND c.status = 'ACTIVE';
--
-- Is the scan within the class schedule?
--   SELECT 1 FROM class_schedules
--   WHERE section_id = ? AND day_of_week = ? AND ? BETWEEN start_time AND end_time;
--
-- Students at or near the absence threshold:
--   SELECT * FROM v_student_attendance_summary
--   WHERE absent_days >= (SELECT CAST(setting_value AS UNSIGNED) FROM settings
--                         WHERE setting_key = 'absence_threshold') - 2;
