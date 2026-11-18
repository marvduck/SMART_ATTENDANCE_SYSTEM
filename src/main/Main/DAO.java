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


 */
public class DAO {
}
