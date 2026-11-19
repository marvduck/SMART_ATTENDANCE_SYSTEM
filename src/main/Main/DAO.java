/*
CLASS FOR STORING JDBC & CREATE, READ, UPDATE, DELETE UserDAO, StudentDAO, TeacherDAO, SectionDAO
ScheduleDAO, AttendanceDAO, AlertDAO.

## EXAMPLE:

IMPORTS

import java.sql.*;

JDBC SETUP
final private String URL = "jdbc:mysql://localhost:3306/SASdb" - FOR LOCALHOSTING.
final private String user = "root" - NAME OF USER ON MYSQL STARTUP.
final private String password "passwordexample123" - SET PASSWORD ON MYSQL STARTUP.

FETCHING CONNECTION
try (
Connection connection = DriverManager.getConnection(URL, user, password);
) catch (SQLException e) {SHOW ERROR MESSAGE: e}

RUNNING QUERIES
try (
String query = "SELECT name, lrn FROM user;";
Statement statement = connection.createStatement();

statement.executeQuery(query);
) catch (SQLException e) {SHOW ERROR MESSAGE: e}

CLAUDE PROMPT
  EXAMPLE QUERIES
      Is the card registered?
      SELECT s.* FROM rfid_cards c JOIN students s ON s.student_id = c.student_id
      WHERE c.card_uid = ? AND c.status = 'ACTIVE';

      Is the scan within the class schedule?
      SELECT 1 FROM class_schedules
      WHERE section_id = ? AND day_of_week = ? AND ? BETWEEN start_time AND end_time;

      Students at or near the absence threshold:
      SELECT * FROM v_student_attendance_summary
      WHERE absent_days >= (SELECT CAST(setting_value AS UNSIGNED) FROM settings
      WHERE setting_key = 'absence_threshold') - 2;

 */
public class DAO {
}
