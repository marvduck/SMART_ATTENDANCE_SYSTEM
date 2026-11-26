package com;

import com.dao.AlertDao;
import com.dao.AttendanceDao;
import com.dao.ExcusedAbsenceDao;
import com.dao.JDBC;
import com.dao.RfidCardDao;
import com.dao.ScanLogDao;
import com.dao.ScheduleDao;
import com.dao.SectionDao;
import com.dao.SettingsDao;
import com.dao.StudentDao;
import com.dao.TeacherDao;
import com.dao.UserDao;
import com.model.Alert;
import com.model.AlertType;
import com.model.AttendanceRecord;
import com.model.AttendanceRow;
import com.model.AttendanceStatus;
import com.model.CardStatus;
import com.model.ClassSchedule;
import com.model.DailyCounts;
import com.model.ExcusedAbsence;
import com.model.RfidCard;
import com.model.ScanLog;
import com.model.ScanResult;
import com.model.Section;
import com.model.Student;
import com.model.StudentAttendanceSummary;
import com.model.StudentStatus;
import com.model.Teacher;
import com.model.User;
import com.ui.AttendanceTheme;
import com.ui.LoginFrame;
import org.mindrot.jbcrypt.BCrypt;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** UI-facing session, formatting and use-case layer over the DAOs. */
public final class Core {

    private Core() {}

    // DAOs: the single place they are created
    private static final UserDao userDao = new UserDao();
    private static final TeacherDao teacherDao = new TeacherDao();
    private static final StudentDao studentDao = new StudentDao();
    private static final SectionDao sectionDao = new SectionDao();
    private static final ScheduleDao scheduleDao = new ScheduleDao();
    private static final AttendanceDao attendanceDao = new AttendanceDao();
    private static final ExcusedAbsenceDao excusedDao = new ExcusedAbsenceDao();
    private static final RfidCardDao cardDao = new RfidCardDao();
    private static final ScanLogDao scanDao = new ScanLogDao();
    private static final AlertDao alertDao = new AlertDao();
    private static final SettingsDao settingsDao = new SettingsDao();

    // Lifecycle + threading

    /** Call once from main before any window opens. */
    public static void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(JDBC::close, "db-close"));
    }

    /** Launches the Swing application. */
    public static void main(String[] args) {
        start();
        SwingUtilities.invokeLater(() -> {
            AttendanceTheme.install(false);
            new LoginFrame().setVisible(true);
        });
    }

    public static boolean dbOnline() {
        try { return JDBC.testConnection(); } catch (Exception e) { return false; }
    }

    /** Runs {@code work} on a worker thread and hands the result to {@code ok} on the Swing thread. */
    public static <T> void async(Callable<T> work, Consumer<T> ok, Consumer<Throwable> fail) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try { ok.accept(get()); }
                catch (java.util.concurrent.ExecutionException e) { fail.accept(e.getCause() == null ? e : e.getCause()); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                catch (RuntimeException e) { fail.accept(e); }
            }
        }.execute();
    }

    /** Same, but failures show a message dialog. */
    public static <T> void async(Callable<T> work, Consumer<T> ok) {
        async(work, ok, e -> SwingUtilities.invokeLater(() ->
                javax.swing.JOptionPane.showMessageDialog(null, describe(e), "Something went wrong", javax.swing.JOptionPane.ERROR_MESSAGE)));
    }

    /** Fire-and-forget write; {@code done} runs on the Swing thread afterwards. */
    public static void run(Runnable write, Runnable done) {
        async(() -> { write.run(); return Boolean.TRUE; }, ok -> done.run());
    }

    public static String describe(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isBlank()) ? t.getClass().getSimpleName() : m;
    }

    /** Thrown for problems the user can fix (bad input, duplicate card...). Shown as-is. */
    public static class UserError extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public UserError(String message) { super(message); }
    }

    // Session + login

    public enum Role { ADMIN, TEACHER, STUDENT }

    public record Session(int userId, String username, Role role, String name, String subtitle,
                          Integer teacherId, Integer studentId, Integer sectionId) {}

    private static volatile Session session;

    public static Session session() { return session; }

    public static void logout() { session = null; }

    /** Checks the password (BCrypt), loads the role profile and starts the session. Blocking: call via {@link #async}. */
    public static Session login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            throw new UserError("Enter your username and password.");
        Optional<User> found = userDao.findByUsername(username.trim());
        if (found.isEmpty() || !passwordMatches(password, found.get().passwordHash()))
            throw new UserError("Wrong username or password.");
        User u = found.get();
        if (!u.active()) throw new UserError("This account is disabled. Ask the administrator.");

        Role role = Role.valueOf(String.valueOf(u.role()));
        int userId = u.userId();
        Session s;
        switch (role) {
            case TEACHER -> {
                Teacher t = teacherDao.findByUserId(userId).orElseThrow(() -> new UserError("No teacher profile is linked to this account."));
                List<Section> mine = sectionDao.findByAdviser(t.teacherId());
                String sub = mine.isEmpty() ? "Teacher" : "Teacher · " + mine.get(0).gradeLevel() + " " + mine.get(0).sectionName();
                s = new Session(userId, u.username(), role, t.fullName(), sub, t.teacherId(), null, null);
            }
            case STUDENT -> {
                Student st = studentDao.findByUserId(userId).orElseThrow(() -> new UserError("No student profile is linked to this account."));
                String sec = st.sectionId() == null ? "" : sectionDao.findById(st.sectionId()).map(Core::sectionLabel).orElse("");
                s = new Session(userId, u.username(), role, st.fullName(), sec.isEmpty() ? "Student" : "Student · " + sec,
                        null, st.studentId(), st.sectionId());
            }
            default -> s = new Session(userId, u.username(), role, "School administrator", "Administrator", null, null, null);
        }
        userDao.updateLastLogin(userId);
        session = s;
        return s;
    }

    private static boolean passwordMatches(String plain, String hash) {
        try { return BCrypt.checkpw(plain, hash); }
        catch (IllegalArgumentException badHash) { return false; }   // Invalid or placeholder hash.
    }

    // Shared types + formatting

    /** A combo-box entry: database id (null = "All") + label. */
    public record Item(Integer id, String label) {
        @Override public String toString() { return label; }
    }

    /** A table row plus the id(s) it stands for. */
    public record Row(Object key, Object[] cells) {}

    public static Object[][] cells(List<Row> rows) {
        Object[][] out = new Object[rows.size()][];
        for (int i = 0; i < out.length; i++) out[i] = rows.get(i).cells();
        return out;
    }

    public record Counts(int enrolled, int present, int late, int absent, int excused) {
        public int attended() { return present + late; }
        public String rate() { return enrolled == 0 ? "0.0%" : String.format(Locale.ROOT, "%.1f%%", 100.0 * attended() / enrolled); }
        public String shareOfEnrolled(int n) { return enrolled == 0 ? "0.0%" : String.format(Locale.ROOT, "%.1f%%", 100.0 * n / enrolled); }
    }

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    public static String time(LocalTime t) { return t == null ? "—" : t.format(TIME); }
    public static String shortDate(LocalDate d) { return d == null ? "—" : d.format(SHORT_DATE); }
    public static String longDate(LocalDate d) { return d == null ? "—" : d.format(LONG_DATE); }

    /** "TIME_IN" -> "Time in". */
    public static String nice(Object e) {
        if (e == null) return "—";
        String s = String.valueOf(e).toLowerCase(Locale.ROOT).replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String stamp(LocalDateTime t) {
        if (t == null) return "—";
        return t.toLocalDate().equals(LocalDate.now()) ? time(t.toLocalTime())
                : t.toLocalDate().equals(LocalDate.now().minusDays(1)) ? "Yesterday" : shortDate(t.toLocalDate());
    }

    public static String sectionLabel(Section s) { return s.gradeLevel() + " " + s.sectionName(); }

    private static String initials(String name) {
        String[] p = name.trim().split("\\s+");
        if (p.length == 0 || p[0].isEmpty()) return "?";
        return ("" + p[0].charAt(0) + (p.length > 1 ? p[p.length - 1].charAt(0) : "")).toUpperCase(Locale.ROOT);
    }

    private static String last4(String uid) { return uid.length() <= 4 ? uid : uid.substring(uid.length() - 4); }

    private static String range(LocalTime a, LocalTime b) {
        return a.format(DateTimeFormatter.ofPattern("hh:mm", Locale.ENGLISH)) + "–" + b.format(TIME);
    }

    /** id -> name caches so a screen does not query once per row. */
    private static final class Names {
        private final Map<Integer, Section> sections = new HashMap<>();
        private final Map<Integer, Student> students = new HashMap<>();
        private final Map<Integer, Teacher> teachers = new HashMap<>();
        Names() {
            for (Section s : sectionDao.findAll()) sections.put(s.sectionId(), s);
            for (Teacher t : teacherDao.findAll()) teachers.put(t.teacherId(), t);
        }
        Student student(Integer id) {
            if (id == null) return null;
            return students.computeIfAbsent(id, k -> studentDao.findById(k).orElse(null));
        }
        String studentName(Integer id) { Student s = student(id); return s == null ? "Unknown card" : s.fullName(); }
        String section(Integer id) { Section s = id == null ? null : sections.get(id); return s == null ? "—" : sectionLabel(s); }
        String studentSection(Integer id) { Student s = student(id); return s == null ? "—" : section(s.sectionId()); }
        String teacher(Integer id) { Teacher t = id == null ? null : teachers.get(id); return t == null ? "—" : t.fullName(); }
    }

    private static int limit() { return settingsDao.absenceThreshold(); }
    private static int watchFrom() { return Math.max(1, settingsDao.absenceThreshold() - settingsDao.warningOffset()); }

    private static String levelFor(int absences, int limit) {
        return absences >= limit ? "School limit" : absences >= limit - 1 ? "Review" : "Watch";
    }

    private static Counts counts(LocalDate d, Integer sectionId) {
        DailyCounts c = attendanceDao.countsForDate(d, sectionId);
        return new Counts(c.enrolled(), c.present(), c.late(), c.absent(), c.excused());
    }

    // Weekly chart

    public record Week(String[] labels, Double[] rates, double average) {}

    private static Week week(Integer sectionId, boolean withDayNumber) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        Map<LocalDate, Double> m = attendanceDao.dailyRates(monday, monday.plusDays(4), sectionId);
        String[] labels = new String[5]; Double[] rates = new Double[5];
        double sum = 0; int n = 0;
        for (int i = 0; i < 5; i++) {
            LocalDate d = monday.plusDays(i);
            labels[i] = withDayNumber ? d.format(DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)) : d.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH));
            rates[i] = m.get(d);
            if (rates[i] != null) { sum += rates[i]; n++; }
        }
        return new Week(labels, rates, n == 0 ? 0 : sum / n);
    }

    // Admin > Overview

    public record AdminOverview(int enrolled, int sections, Counts today, Week week, List<Object[]> live,
                                int openAlerts, int nearLimit, int outOfBounds, int invalidCards) {}

    public static AdminOverview adminOverview() {
        LocalDate today = LocalDate.now();
        Names nm = new Names();
        List<Object[]> live = new ArrayList<>();
        for (ScanLog l : scanDao.findRecent(4)) {
            boolean known = l.studentId() != null;
            live.add(new Object[]{known ? nm.studentName(l.studentId()) : "Card " + l.cardUid(), known ? nm.studentSection(l.studentId()) : "—",
                    l.scanTime().toLocalTime().format(CLOCK), nice(l.result())});
        }
        return new AdminOverview(studentDao.countByStatus(StudentStatus.ENROLLED), sectionDao.count(), counts(today, null),
                week(null, true), live, alertDao.countUnread(),
                attendanceDao.findByMinAbsences(watchFrom(), null).size(),
                scanDao.countByResultOnDate(ScanResult.OUT_OF_BOUNDS, today),
                scanDao.countByResultOnDate(ScanResult.INVALID_CARD, today));
    }

    // Admin > Students

    public record StudentsOverview(int enrolled, int activeCards, int toAssign, int sections, List<Item> sectionItems) {}

    public static StudentsOverview studentsOverview() {
        List<Item> items = new ArrayList<>();
        items.add(new Item(null, "All sections"));
        for (Section s : sectionDao.findAll()) items.add(new Item(s.sectionId(), sectionLabel(s)));
        return new StudentsOverview(studentDao.countByStatus(StudentStatus.ENROLLED), cardDao.countByStatus(CardStatus.ACTIVE),
                studentDao.findWithoutActiveCard().size(), sectionDao.count(), items);
    }

    public record StudentRows(Object[][] cells, int[] ids, int total) {}

    private static final int PAGE = 100;

    /** cardFilter: "All RFID status", "Linked", "No card", "Lost", "Disabled". grade: 0 = all. */
    public static StudentRows studentRows(String keyword, int grade, Integer sectionId, String cardFilter) {
        Map<Integer, String> cardState = new HashMap<>();
        for (RfidCard c : cardDao.findAll()) {
            if (CardStatus.ACTIVE.equals(c.status())) cardState.put(c.studentId(), "Linked");
            else cardState.putIfAbsent(c.studentId(), nice(c.status()));
        }
        Names nm = new Names();
        List<Student> found = studentDao.search(keyword == null || keyword.isBlank() ? null : keyword.trim(),
                grade == 0 ? null : grade, sectionId, null);
        List<Object[]> rows = new ArrayList<>(); List<Integer> ids = new ArrayList<>();
        int total = 0;
        for (Student s : found) {
            String card = cardState.getOrDefault(s.studentId(), "No card");
            if (cardFilter != null && !cardFilter.startsWith("All") && !cardFilter.equalsIgnoreCase(card)) continue;
            total++;
            if (rows.size() < PAGE) { rows.add(new Object[]{s.fullName(), nm.section(s.sectionId()), card, nice(s.status()), "View"}); ids.add(s.studentId()); }
        }
        return new StudentRows(rows.toArray(new Object[0][]), ids.stream().mapToInt(Integer::intValue).toArray(), total);
    }

    public record Profile(int studentId, String name, String initials, String lrn, String section, String adviser,
                          String card, String login, String attendance, String guardian) {}

    public static Optional<Profile> studentProfile(int studentId) {
        Optional<Student> found = studentDao.findById(studentId);
        if (found.isEmpty()) return Optional.empty();
        Student s = found.get();
        Names nm = new Names();
        Section sec = s.sectionId() == null ? null : sectionDao.findById(s.sectionId()).orElse(null);
        String adviser = sec == null ? "—" : nm.teacher(sec.adviserId());
        String card = cardDao.findActiveByStudent(studentId).map(c -> "Card ending " + last4(c.cardUid()) + " · Active").orElse("No active card");
        String att = attendanceDao.findSummary(studentId)
                .map(x -> "Present " + (x.presentDays() + x.lateDays()) + " · Absent " + x.absentDays() + " · Excused " + x.excusedDays()).orElse("No records yet");
        return Optional.of(new Profile(studentId, s.fullName(), initials(s.fullName()), "LRN " + s.lrn(), sec == null ? "—" : sectionLabel(sec),
                adviser, card, s.userId() == null ? "No login account" : "Has a login account", att,
                s.guardianContact() == null ? "—" : s.guardianContact()));
    }

    // Admin > Teachers & sections

    public record TeachersSections(int teachers, int sections, int advisers, int withoutAdviser,
                                   List<Row> teacherRows, List<Row> sectionRows, List<Item> teacherItems) {}

    public static TeachersSections teachersAndSections(String teacherKeyword, String sectionKeyword) {
        Names nm = new Names();
        List<Section> allSections = sectionDao.findAll();
        Set<Integer> advisers = new HashSet<>();
        Map<Integer, List<String>> advisory = new HashMap<>();
        for (Section s : allSections) if (s.adviserId() != null) {
            advisers.add(s.adviserId());
            advisory.computeIfAbsent(s.adviserId(), k -> new ArrayList<>()).add(sectionLabel(s));
        }
        List<Teacher> allTeachers = teacherDao.findAll();
        List<Row> tRows = new ArrayList<>(); List<Item> tItems = new ArrayList<>();
        for (Teacher t : allTeachers) tItems.add(new Item(t.teacherId(), t.fullName()));
        List<Teacher> shownTeachers = teacherKeyword == null || teacherKeyword.isBlank() ? allTeachers : teacherDao.search(teacherKeyword.trim());
        for (Teacher t : shownTeachers)
            tRows.add(new Row(t.teacherId(), new Object[]{t.fullName(), t.employeeNo(), t.email() == null ? "—" : t.email(),
                    advisory.containsKey(t.teacherId()) ? String.join(", ", advisory.get(t.teacherId())) : "—"}));
        List<Row> sRows = new ArrayList<>();
        List<Section> shownSections = sectionKeyword == null || sectionKeyword.isBlank() ? allSections : sectionDao.search(sectionKeyword.trim());
        for (Section s : shownSections)
            sRows.add(new Row(s.sectionId(), new Object[]{s.sectionName(), String.valueOf(s.gradeLevel()), s.schoolYear(),
                    nm.teacher(s.adviserId()), String.valueOf(sectionDao.countStudents(s.sectionId())), "Set adviser"}));
        int without = (int) allSections.stream().filter(s -> s.adviserId() == null).count();
        return new TeachersSections(teacherDao.count(), allSections.size(), advisers.size(), without, tRows, sRows, tItems);
    }

    public static void addSection(String name, int grade, String schoolYear, Integer adviserId) {
        if (name == null || name.isBlank() || schoolYear == null || schoolYear.isBlank()) throw new UserError("Enter the section name and school year.");
        if (sectionDao.exists(name.trim(), grade, schoolYear.trim())) throw new UserError("That section already exists for this school year.");
        sectionDao.insert(Section.create(name.trim(), grade, null, schoolYear.trim(), adviserId));
    }

    public static void setAdviser(int sectionId, Integer teacherId) { sectionDao.setAdviser(sectionId, teacherId); }

    // Admin > Class schedules

    public record Schedules(int count, int sectionsCovered, int teachers, String earlyWindow, List<Item> sectionItems, List<Item> teacherItems) {}

    public static Schedules schedulesOverview() {
        List<Section> secs = sectionDao.findAll();
        List<Item> secItems = new ArrayList<>(), teachItems = new ArrayList<>();
        for (Section s : secs) secItems.add(new Item(s.sectionId(), sectionLabel(s)));
        for (Teacher t : teacherDao.findAll()) teachItems.add(new Item(t.teacherId(), t.fullName()));
        Set<Integer> covered = new HashSet<>(), teaching = new HashSet<>();
        for (ClassSchedule c : scheduleDao.findAll()) { covered.add(c.sectionId()); if (c.teacherId() != null) teaching.add(c.teacherId()); }
        int grace = settingsDao.getInt(SettingsDao.LATE_GRACE_MINUTES, 15);
        return new Schedules(scheduleDao.count(), covered.size(), teaching.size(), grace + " min", secItems, teachItems);
    }

    /** sectionId null = every section; day null = every day. */
    public static List<Row> scheduleRows(Integer sectionId, DayOfWeek day) {
        Names nm = new Names();
        List<ClassSchedule> list = sectionId == null ? scheduleDao.findAll() : day == null ? scheduleDao.findBySection(sectionId) : scheduleDao.findBySectionAndDay(sectionId, day);
        List<Row> rows = new ArrayList<>();
        for (ClassSchedule c : list) {
            if (day != null && !day.equals(c.dayOfWeek())) continue;
            rows.add(new Row(c.scheduleId(), new Object[]{nm.section(c.sectionId()), c.subject(), nice(c.dayOfWeek()),
                    range(c.startTime(), c.endTime()), nm.teacher(c.teacherId()), "Delete"}));
        }
        return rows;
    }

    public static void addSchedule(Integer sectionId, Integer teacherId, String subject, DayOfWeek day, String start, String end) {
        if (sectionId == null) throw new UserError("Choose a section.");
        if (day == null || day == DayOfWeek.SUNDAY) throw new UserError("Choose a day from Monday to Saturday.");
        if (subject == null || subject.isBlank()) throw new UserError("Enter the subject.");
        LocalTime s, e;
        try { s = LocalTime.parse(start.trim()); e = LocalTime.parse(end.trim()); }
        catch (DateTimeParseException | NullPointerException ex) { throw new UserError("Times must look like 08:00 (24-hour)."); }
        if (!e.isAfter(s)) throw new UserError("The end time must be after the start time.");
        if (scheduleDao.hasOverlap(sectionId, day, s, e, null)) throw new UserError("This section already has a class that overlaps that time.");
        scheduleDao.insert(ClassSchedule.create(sectionId, teacherId, subject.trim(), day, s, e, null));
    }

    public static void deleteSchedule(int scheduleId) { scheduleDao.delete(scheduleId); }

    // Admin > RFID cards

    public record Rfid(int linked, int withoutCard, int lostOrDisabled, int invalidToday, String invalidUid, String invalidWhen,
                       List<Item> studentsWithoutCard) {}

    public static Rfid rfidOverview() {
        List<ScanLog> bad = scanDao.findByResult(ScanResult.INVALID_CARD, 1);
        Set<Integer> studentsWithCards = new HashSet<>();
        for (RfidCard card : cardDao.findAll()) studentsWithCards.add(card.studentId());
        List<Item> free = new ArrayList<>();
        for (Student s : studentDao.findWithoutActiveCard())
            if (!studentsWithCards.contains(s.studentId())) free.add(new Item(s.studentId(), s.fullName()));
        return new Rfid(cardDao.countByStatus(CardStatus.ACTIVE), free.size(),
                cardDao.countByStatus(CardStatus.LOST) + cardDao.countByStatus(CardStatus.DISABLED),
                scanDao.countByResultOnDate(ScanResult.INVALID_CARD, LocalDate.now()),
                bad.isEmpty() ? null : bad.get(0).cardUid(), bad.isEmpty() ? null : stamp(bad.get(0).scanTime()), free);
    }

    /** Row key = {studentId, cardUid, status}; null card fields mean unregistered. */
    public static List<Row> rfidRows(String keyword, String filter) {
        Names nm = new Names();
        Map<Integer, LocalDateTime> lastTap = new HashMap<>();
        for (ScanLog l : scanDao.findRecent(300)) if (l.studentId() != null) lastTap.putIfAbsent(l.studentId(), l.scanTime());
        boolean hasKey = keyword != null && !keyword.isBlank();
        List<RfidCard> allCards = cardDao.findAll();
        Set<Integer> studentsWithCards = new HashSet<>();
        for (RfidCard card : allCards) studentsWithCards.add(card.studentId());
        List<RfidCard> cards = hasKey ? cardDao.search(keyword.trim()) : allCards;
        List<Row> rows = new ArrayList<>();
        boolean wantAll = filter == null || filter.startsWith("All");
        for (RfidCard c : cards) {
            String status = nice(c.status());
            if (!wantAll && !filter.equalsIgnoreCase(status)) continue;
            String action = CardStatus.ACTIVE.equals(c.status()) ? "Disable" : "Replace";
            rows.add(new Row(new Object[]{c.studentId(), c.cardUid(), String.valueOf(c.status())},
                    new Object[]{nm.studentName(c.studentId()), nm.studentSection(c.studentId()), c.cardUid(), status,
                            stamp(lastTap.get(c.studentId())), action}));
        }
        if (wantAll || "Unregistered".equalsIgnoreCase(filter)) {
            String k = hasKey ? keyword.trim().toLowerCase(Locale.ROOT) : "";
            for (Student s : studentDao.findWithoutActiveCard())
                if (!studentsWithCards.contains(s.studentId())
                        && (k.isEmpty() || s.fullName().toLowerCase(Locale.ROOT).contains(k)
                        || s.lrn().toLowerCase(Locale.ROOT).contains(k)))
                    rows.add(new Row(new Object[]{s.studentId(), null, null},
                            new Object[]{s.fullName(), nm.section(s.sectionId()), "—", "Unregistered", "—", "Register"}));
        }
        return rows;
    }

    public static void registerCard(Integer studentId, String uid) {
        if (studentId == null) throw new UserError("Choose a student first.");
        if (uid == null || uid.isBlank()) throw new UserError("Tap a card on the reader, or type its UID.");
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new UserError("That student no longer exists."));
        if (student.status() != StudentStatus.ENROLLED) throw new UserError("Only enrolled students can receive a card.");
        if (cardDao.findActiveByStudent(studentId).isPresent())
            throw new UserError("This student already has an active card. Replace it instead.");
        String clean = uid.trim().toUpperCase(Locale.ROOT);
        if (cardDao.existsByUid(clean)) throw new UserError("That card is already registered.");
        cardDao.assign(clean, studentId);
    }

    public static void disableCard(int cardStudentId, String uid) {
        if (uid == null || uid.isBlank()) throw new UserError("Enter the card UID.");
        RfidCard card = cardDao.findByUid(uid)
                .orElseThrow(() -> new UserError("That card is not registered."));
        if (card.studentId() != cardStudentId) throw new UserError("That card belongs to another student.");
        if (card.status() != CardStatus.ACTIVE) throw new UserError("That card is already disabled or lost.");
        if (!cardDao.updateStatus(card.cardId(), CardStatus.DISABLED))
            throw new UserError("The card could not be disabled.");
    }

    public static void replaceCard(int studentId, String newUid, String oldStatus) {
        if (newUid == null || newUid.isBlank()) throw new UserError("Enter the new card UID.");
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new UserError("That student no longer exists."));
        if (student.status() != StudentStatus.ENROLLED) throw new UserError("Only enrolled students can receive a card.");
        String clean = newUid.trim().toUpperCase(Locale.ROOT);
        if (cardDao.existsByUid(clean)) throw new UserError("That card is already registered.");
        cardDao.replaceCard(studentId, clean, "LOST".equalsIgnoreCase(oldStatus) ? CardStatus.LOST : CardStatus.DISABLED);
    }

    // Admin > Alerts & thresholds

    public record AlertsOverview(int open, int invalid, int outOfBounds, int near, int threshold, int offset) {}

    public static AlertsOverview alertsOverview() {
        return new AlertsOverview(alertDao.countUnread(), alertDao.countUnreadByType(AlertType.INVALID_CARD),
                alertDao.countUnreadByType(AlertType.OUT_OF_BOUNDS),
                alertDao.countUnreadByType(AlertType.THRESHOLD_NEAR) + alertDao.countUnreadByType(AlertType.THRESHOLD_REACHED),
                settingsDao.absenceThreshold(), settingsDao.warningOffset());
    }

    private static String alertLabel(Object type) {
        return switch (String.valueOf(type)) {
            case "INVALID_CARD" -> "Invalid card";
            case "OUT_OF_BOUNDS" -> "Out of bounds";
            case "THRESHOLD_NEAR" -> "Near limit";
            case "THRESHOLD_REACHED" -> "Over limit";
            default -> nice(type);
        };
    }

    /** Row key = {alertId, unread}; filters accept the UI labels, including "Absence limit". */
    public static List<Row> alertRows(String typeFilter, boolean unreadOnly) {
        List<Alert> alerts = unreadOnly ? alertDao.findUnread() : alertDao.findRecent(60);
        List<Row> rows = new ArrayList<>();
        for (Alert a : alerts) {
            String label = alertLabel(a.type());
            boolean absenceAlert = a.type() == AlertType.THRESHOLD_NEAR || a.type() == AlertType.THRESHOLD_REACHED;
            boolean typeMatches = typeFilter == null || typeFilter.startsWith("All")
                    || typeFilter.equalsIgnoreCase(label)
                    || ("Absence limit".equalsIgnoreCase(typeFilter) && absenceAlert);
            if (!typeMatches) continue;
            rows.add(new Row(new Object[]{a.alertId(), !a.read()},
                    new Object[]{label, a.message(), stamp(a.createdAt()), a.read() ? "Read" : "Unread", a.read() ? "Delete" : "Mark read"}));
        }
        return rows;
    }

    public static void markAlertRead(int id) { alertDao.markRead(id); }
    public static void deleteAlert(int id) { alertDao.delete(id); }
    public static void markAllAlertsRead() { alertDao.markAllRead(); }
    public static void clearReadAlerts() { alertDao.deleteRead(); }

    public static void saveLimits(String threshold, String offset) {
        int t, o;
        try { t = Integer.parseInt(threshold.trim()); o = Integer.parseInt(offset.trim()); }
        catch (NumberFormatException | NullPointerException e) { throw new UserError("Both limits must be whole numbers."); }
        if (t < 1 || o < 0 || o >= t) throw new UserError("The threshold must be at least 1 and larger than the warning offset.");
        settingsDao.set("absence_threshold", t);
        settingsDao.set("absence_warning_offset", o);
    }

    // Reports (admin + teacher)

    public record ReportsOverview(List<Item> sectionItems, double weekAverage, int absentToday, int excusedToday, int overLimit, Week week) {}

    private static List<Section> visibleSections() {
        Session s = session;
        if (s == null) throw new UserError("Sign in before viewing reports.");
        return switch (s.role()) {
            case ADMIN -> sectionDao.findAll();
            case TEACHER -> {
                if (s.teacherId() == null) throw new UserError("No teacher profile is linked to this account.");
                yield sectionDao.findByAdviser(s.teacherId());
            }
            case STUDENT -> throw new UserError("Students cannot view staff reports.");
        };
    }

    private static Counts counts(List<Section> sections, LocalDate date) {
        if (sections.size() == 1) return counts(date, sections.get(0).sectionId());
        int enrolled = 0, present = 0, late = 0, absent = 0, excused = 0;
        for (Section section : sections) {
            Counts c = counts(date, section.sectionId());
            enrolled += c.enrolled();
            present += c.present();
            late += c.late();
            absent += c.absent();
            excused += c.excused();
        }
        return new Counts(enrolled, present, late, absent, excused);
    }

    private static Week weekForSections(List<Section> sections, boolean withDayNumber) {
        if (sections.size() == 1) return week(sections.get(0).sectionId(), withDayNumber);
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        Map<LocalDate, Double> weightedRates = new HashMap<>();
        Map<LocalDate, Integer> weights = new HashMap<>();
        for (Section section : sections) {
            Map<LocalDate, Double> rates = attendanceDao.dailyRates(monday, monday.plusDays(4), section.sectionId());
            for (int i = 0; i < 5; i++) {
                LocalDate date = monday.plusDays(i);
                Double rate = rates.get(date);
                if (rate == null) continue;
                Counts c = counts(date, section.sectionId());
                int records = c.present() + c.late() + c.absent() + c.excused();
                if (records == 0) continue;
                weightedRates.merge(date, rate * records, Double::sum);
                weights.merge(date, records, Integer::sum);
            }
        }
        String[] labels = new String[5];
        Double[] rates = new Double[5];
        double sum = 0;
        int n = 0;
        for (int i = 0; i < 5; i++) {
            LocalDate date = monday.plusDays(i);
            labels[i] = date.format(DateTimeFormatter.ofPattern(withDayNumber ? "EEE d" : "EEE", Locale.ENGLISH));
            Integer weight = weights.get(date);
            rates[i] = weight == null ? null : weightedRates.get(date) / weight;
            if (rates[i] != null) { sum += rates[i]; n++; }
        }
        return new Week(labels, rates, n == 0 ? 0 : sum / n);
    }

    public static ReportsOverview reportsOverview() {
        List<Item> items = new ArrayList<>();
        List<Section> secs = visibleSections();
        Session s = session;
        if (s == null) throw new UserError("Sign in before viewing reports.");
        if (s.role() == Role.ADMIN) items.add(new Item(null, "All sections"));
        for (Section section : secs) items.add(new Item(section.sectionId(), sectionLabel(section)));
        Counts c = s.role() == Role.TEACHER ? counts(secs, LocalDate.now()) : counts(LocalDate.now(), null);
        Week w = s.role() == Role.TEACHER ? weekForSections(secs, false) : week(null, false);
        int threshold = limit();
        int overLimit = s.role() == Role.TEACHER
                ? secs.stream().mapToInt(section -> attendanceDao.findByMinAbsences(threshold, section.sectionId()).size()).sum()
                : attendanceDao.findByMinAbsences(threshold, null).size();
        return new ReportsOverview(items, w.average(), c.absent(), c.excused(), overLimit, w);
    }

    /** One row per section (or just the chosen one): enrolled, present+late, absent, excused, rate. */
    public static List<Object[]> reportRows(Integer sectionId, LocalDate date) {
        if (date == null) throw new UserError("Choose a report date.");
        List<Object[]> rows = new ArrayList<>();
        for (Section s : visibleSections()) {
            if (sectionId != null && !sectionId.equals(s.sectionId())) continue;
            Counts c = counts(date, s.sectionId());
            rows.add(new Object[]{sectionLabel(s), String.valueOf(c.enrolled()), String.valueOf(c.attended()),
                    String.valueOf(c.absent()), String.valueOf(c.excused()), c.rate()});
        }
        return rows;
    }

    /** Plain CSV export (quotes values that contain commas, quotes or line breaks). */
    public static void exportCsv(File file, String[] header, Object[][] rows) throws IOException {
        try (FileWriter w = new FileWriter(file, java.nio.charset.StandardCharsets.UTF_8)) {
            w.write(csvLine(header));
            for (Object[] r : rows) w.write(csvLine(r));
        }
    }

    private static String csvLine(Object[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            String v = cells[i] == null ? "" : String.valueOf(cells[i]);
            if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r"))
                v = "\"" + v.replace("\"", "\"\"") + "\"";
            sb.append(i == 0 ? "" : ",").append(v);
        }
        return sb.append("\r\n").toString();
    }

    // Teacher

    private static Session teacherSession() {
        Session s = session;
        if (s == null || s.role() != Role.TEACHER || s.teacherId() == null)
            throw new UserError("Sign in as a teacher to view this page.");
        return s;
    }

    private static Integer requireTeacherSection(Integer sectionId) {
        Session s = teacherSession();
        if (sectionId == null) throw new UserError("Choose one of your sections.");
        boolean owned = sectionDao.findByAdviser(s.teacherId()).stream()
                .anyMatch(section -> section.sectionId().equals(sectionId));
        if (!owned) throw new UserError("You can only view your own sections.");
        return sectionId;
    }

    private static Student requireTeacherStudent(int studentId) {
        teacherSession();
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new UserError("That student no longer exists."));
        if (student.status() != StudentStatus.ENROLLED)
            throw new UserError("That student is not currently enrolled.");
        requireTeacherSection(student.sectionId());
        return student;
    }

    public static List<Item> mySectionItems() {
        Session s = teacherSession();
        List<Item> items = new ArrayList<>();
        for (Section section : sectionDao.findByAdviser(s.teacherId()))
            items.add(new Item(section.sectionId(), sectionLabel(section)));
        return items;
    }

    private static Integer firstSectionId() {
        List<Item> mine = mySectionItems();
        return mine.isEmpty() ? null : mine.get(0).id();
    }

    private static Object[] attendanceCells(AttendanceRow r) {
        String status = r.status() == null ? "Absent" : nice(r.status());
        String action = "Absent".equals(status) ? "Mark excused" : "Excused".equals(status) ? "View reason" : "View";
        return new Object[]{r.fullName(), time(r.timeIn()), time(r.timeOut()), status, action};
    }

    public record TeacherOverview(boolean hasClass, String sectionLabel, Counts today, String classTime,
                                  List<Row> classRows, Week week, List<Object[]> watch, int watchTotal, Object[] excusedReview) {}

    public static TeacherOverview teacherOverview() {
        Session s = teacherSession();
        Integer sec = firstSectionId();
        LocalDate today = LocalDate.now();
        if (sec == null) return new TeacherOverview(false, "No class assigned", new Counts(0, 0, 0, 0, 0), "", List.of(),
                new Week(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri"}, new Double[5], 0), List.of(), 0, null);
        String label = mySectionItems().get(0).label();
        String classTime = "";
        for (ClassSchedule c : scheduleDao.findByTeacherAndDay(s.teacherId(), today.getDayOfWeek()))
            if (sec.equals(c.sectionId())) { classTime = " · " + range(c.startTime(), c.endTime()); break; }
        List<Row> rows = new ArrayList<>();
        List<AttendanceRow> all = attendanceDao.findRows(today, sec, null, null);
        for (int i = 0; i < Math.min(5, all.size()); i++) rows.add(new Row(all.get(i).studentId(), attendanceCells(all.get(i))));
        List<StudentAttendanceSummary> near = attendanceDao.findByMinAbsences(watchFrom(), sec);
        List<Object[]> watch = new ArrayList<>();
        for (int i = 0; i < Math.min(3, near.size()); i++)
            watch.add(new Object[]{near.get(i).fullName(), near.get(i).absentDays() + " unexcused absences"});
        Object[] review = null;
        List<ExcusedAbsence> ex = excusedDao.findBySection(sec, today.minusDays(14), today);
        if (!ex.isEmpty()) {
            ExcusedAbsence e = ex.get(0);
            review = new Object[]{new Names().studentName(e.studentId()), longDate(e.absenceDate()) + " · " + e.reason(), "Approved · Recorded"};
        }
        return new TeacherOverview(true, label, counts(today, sec), classTime, rows, week(sec, false), watch, near.size(), review);
    }

    // ---- Teacher > Class attendance ----

    public static Counts classCounts(Integer sectionId) {
        return counts(LocalDate.now(), requireTeacherSection(sectionId));
    }

    /** status: "All statuses","Present","Late","Absent","Excused". */
    public static List<Row> classRows(Integer sectionId, String keyword, String status) {
        Integer ownedSectionId = requireTeacherSection(sectionId);
        AttendanceStatus st = status == null || status.startsWith("All") ? null : AttendanceStatus.valueOf(status.toUpperCase(Locale.ROOT));
        List<Row> rows = new ArrayList<>();
        for (AttendanceRow r : attendanceDao.findRows(LocalDate.now(), ownedSectionId,
                keyword == null || keyword.isBlank() ? null : keyword.trim(), st))
            rows.add(new Row(r.studentId(), attendanceCells(r)));
        return rows;
    }

    public record ClassDetail(String name, String initials, String status, String reason, String markedBy, String when) {}

    public static ClassDetail classDetail(int studentId) {
        LocalDate today = LocalDate.now();
        Student student = requireTeacherStudent(studentId);
        String name = student.fullName();
        Optional<AttendanceRecord> rec = attendanceDao.findByStudentAndDate(studentId, today);
        Optional<ExcusedAbsence> ex = excusedDao.findByStudentAndDate(studentId, today);
        String status = rec.map(r -> nice(r.status())).orElse("Absent");
        String by = ex.map(e -> e.markedBy() == null ? "—" : teacherDao.findByUserId(e.markedBy()).map(Teacher::fullName).orElse("—")).orElse("—");
        return new ClassDetail(name, initials(name), status, ex.map(ExcusedAbsence::reason).orElse("—"), by, shortDate(today));
    }

    /** Marks an absence as excused and records the reason. */
    public static void markExcused(int studentId, LocalDate date, String reason) {
        Session s = teacherSession();
        requireTeacherStudent(studentId);
        if (date == null || date.isAfter(LocalDate.now())) throw new UserError("Choose today or a past date.");
        if (reason == null || reason.isBlank()) throw new UserError("Give a reason for the excused absence.");
        if (excusedDao.findByStudentAndDate(studentId, date).isPresent())
            throw new UserError("This day is already marked as excused.");
        String cleanReason = reason.trim();
        excusedDao.markExcused(studentId, date, cleanReason, s.userId());
    }

    // ---- Teacher > Excused absences ----

    public record ExcusedOverview(int today, int week, int month, int needExcuse, List<Item> studentItems) {}

    public static ExcusedOverview excusedOverview() {
        Integer sec = firstSectionId();
        LocalDate d = LocalDate.now();
        if (sec == null) return new ExcusedOverview(0, 0, 0, 0, List.of());
        List<Item> students = new ArrayList<>();
        for (Student s : studentDao.findBySection(sec))
            if (s.status() == StudentStatus.ENROLLED) students.add(new Item(s.studentId(), s.fullName()));
        return new ExcusedOverview(excusedDao.findBySection(sec, d, d).size(),
                excusedDao.findBySection(sec, d.with(DayOfWeek.MONDAY), d).size(),
                excusedDao.findBySection(sec, d.withDayOfMonth(1), d).size(),
                attendanceDao.findRows(d, sec, null, AttendanceStatus.ABSENT).size(), students);
    }

    /** Row key = excusedId. monthsBack 0 = this month. */
    public static List<Row> excusedRows(String keyword, int monthsBack) {
        Integer sec = firstSectionId();
        if (sec == null) return List.of();
        LocalDate first = LocalDate.now().withDayOfMonth(1).minusMonths(monthsBack);
        LocalDate last = first.plusMonths(1).minusDays(1);
        Names nm = new Names();
        String k = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<Row> rows = new ArrayList<>();
        for (ExcusedAbsence e : excusedDao.findBySection(sec, first, last)) {
            String name = nm.studentName(e.studentId());
            if (!k.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(k)) continue;
            String by = e.markedBy() == null ? "—" : teacherDao.findByUserId(e.markedBy()).map(Teacher::fullName).orElse("—");
            rows.add(new Row(new Object[]{e.excusedId(), e.reason()}, new Object[]{name, longDate(e.absenceDate()), e.reason(), by, "Approved", "Edit"}));
        }
        return rows;
    }

    public static void updateExcusedReason(int excusedId, String reason) {
        ExcusedAbsence excuse = excusedDao.findById(excusedId)
                .orElseThrow(() -> new UserError("That excused absence no longer exists."));
        requireTeacherStudent(excuse.studentId());
        if (reason == null || reason.isBlank()) throw new UserError("The reason cannot be empty.");
        if (!excusedDao.updateReason(excusedId, reason.trim()))
            throw new UserError("The excused absence could not be updated.");
    }

    public static void removeExcused(int excusedId) {
        ExcusedAbsence excuse = excusedDao.findById(excusedId)
                .orElseThrow(() -> new UserError("That excused absence no longer exists."));
        requireTeacherStudent(excuse.studentId());
        if (!excusedDao.remove(excusedId)) throw new UserError("The excused absence could not be removed.");
    }

    public static LocalDate parseDate(String text) {
        try { return LocalDate.parse(text.trim()); }
        catch (DateTimeParseException | NullPointerException e) { throw new UserError("Use the date format YYYY-MM-DD."); }
    }

    // ---- Teacher > Threshold watch ----

    public record ThresholdOverview(int threshold, int offset, int onWatch, int atReview, int atLimit, int excusedDays) {}

    public static ThresholdOverview thresholdOverview() {
        Integer sec = firstSectionId();
        int threshold = limit();
        int offset = settingsDao.warningOffset();
        if (sec == null) return new ThresholdOverview(threshold, offset, 0, 0, 0, 0);
        List<StudentAttendanceSummary> list = attendanceDao.findByMinAbsences(Math.max(1, threshold - offset), sec);
        int review = 0, atLimit = 0, excused = 0;
        for (StudentAttendanceSummary s : list) {
            String l = levelFor(s.absentDays(), threshold);
            if ("Review".equals(l)) review++; else if ("School limit".equals(l)) atLimit++;
            excused += s.excusedDays();
        }
        return new ThresholdOverview(threshold, offset, list.size(), review, atLimit, excused);
    }

    /** Row key = studentId. level: "All levels","Watch","Review","School limit". */
    public static List<Row> thresholdRows(String level) {
        Integer sec = firstSectionId();
        if (sec == null) return List.of();
        int threshold = limit();
        int watchFrom = Math.max(1, threshold - settingsDao.warningOffset());
        Names nm = new Names();
        List<Row> rows = new ArrayList<>();
        for (StudentAttendanceSummary s : attendanceDao.findByMinAbsences(watchFrom, sec)) {
            String l = levelFor(s.absentDays(), threshold);
            if (level != null && !level.startsWith("All") && !level.equals(l)) continue;
            rows.add(new Row(s.studentId(), new Object[]{s.fullName(), nm.section(s.sectionId()), String.valueOf(s.absentDays()), l, "Review →"}));
        }
        return rows;
    }

    public record FollowUp(String name, String initials, String section, int absences, List<String> dates, String guardian) {}

    public static Optional<FollowUp> followUp(int studentId) {
        Student s = requireTeacherStudent(studentId);
        List<String> dates = new ArrayList<>();
        for (AttendanceRecord r : attendanceDao.findByStudentBetween(studentId, LocalDate.now().minusMonths(3), LocalDate.now()))
            if (AttendanceStatus.ABSENT.equals(r.status())) dates.add(shortDate(r.attendanceDate()));
        int absences = attendanceDao.findSummary(studentId).map(StudentAttendanceSummary::absentDays).orElse(dates.size());
        return Optional.of(new FollowUp(s.fullName(), initials(s.fullName()), new Names().section(s.sectionId()), absences,
                dates.size() > 5 ? dates.subList(0, 5) : dates, s.guardianContact() == null ? "Not on file" : s.guardianContact()));
    }

    // Student

    private static int myStudentId() {
        if (session == null || session.studentId() == null) throw new UserError("No student profile for this account.");
        return session.studentId();
    }

    public record StudentOverview(int attended, int absent, int excused, int totalDays, String rate, String calendarStates,
                                  List<Object[]> recent, String status, String timeIn, String timeOut, String classLine,
                                  String scheduleLine, String cardLine, String excusedDate, String excusedReason, int excusedCount) {}

    public static StudentOverview studentOverview() {
        int id = myStudentId();
        LocalDate today = LocalDate.now();
        StudentAttendanceSummary sum = attendanceDao.findSummary(id).orElseThrow(() -> new UserError("No attendance summary yet."));
        int attended = sum.presentDays() + sum.lateDays();
        int total = attended + sum.absentDays() + sum.excusedDays();

        StringBuilder states = new StringBuilder();
        for (AttendanceRecord r : attendanceDao.findByStudentBetween(id, today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()))) {
            String st = String.valueOf(r.status());
            char c = st.equals("ABSENT") ? 'A' : st.equals("EXCUSED") ? 'E' : 'P';
            states.append(r.attendanceDate().getDayOfMonth()).append(c).append(' ');
        }
        List<Object[]> recent = new ArrayList<>();
        for (AttendanceRecord r : attendanceDao.findByStudent(id)) {
            if (recent.size() == 4) break;
            recent.add(new Object[]{shortDate(r.attendanceDate()), time(r.timeIn()), time(r.timeOut()), nice(r.status())});
        }
        Optional<AttendanceRecord> now = attendanceDao.findByStudentAndDate(id, today);
        String status = now.isEmpty() ? "Not in yet" : now.get().timeOut() != null ? "Left campus" : now.get().timeIn() != null ? "On campus" : nice(now.get().status());
        Names nm = new Names();
        Student me = nm.student(id);
        String classLine = nm.section(me.sectionId());
        String sched = "No class today";
        if (me.sectionId() != null) {
            List<ClassSchedule> todays = scheduleDao.findBySectionAndDay(me.sectionId(), today.getDayOfWeek());
            if (!todays.isEmpty()) sched = "Class schedule  " + range(todays.get(0).startTime(), todays.get(todays.size() - 1).endTime());
        }
        String card = cardDao.findActiveByStudent(id).map(c -> "RFID card ending in " + last4(c.cardUid()) + " · Active").orElse("No active RFID card");
        List<ExcusedAbsence> ex = excusedDao.findByStudent(id);
        return new StudentOverview(attended, sum.absentDays(), sum.excusedDays(), total,
                total == 0 ? "0%" : Math.round(100.0 * attended / total) + "%", states.toString().trim(), recent, status,
                now.map(r -> time(r.timeIn())).orElse("—"), now.map(r -> r.timeOut() == null ? "Not yet" : time(r.timeOut())).orElse("Not yet"),
                classLine, sched, card, ex.isEmpty() ? null : longDate(ex.get(0).absenceDate()), ex.isEmpty() ? null : ex.get(0).reason(), ex.size());
    }

    /** Row key = date. statusFilter: "All statuses","Present","Late","Absent","Excused". monthsBack: -1 = all time. */
    public static List<Row> myRecords(String statusFilter, int monthsBack) {
        int id = myStudentId();
        Map<LocalDate, String> reasons = new HashMap<>();
        for (ExcusedAbsence e : excusedDao.findByStudent(id)) reasons.put(e.absenceDate(), e.reason());
        List<AttendanceRecord> src = attendanceDao.findByStudent(id);
        List<Row> rows = new ArrayList<>();
        LocalDate first = monthsBack < 0 ? null : LocalDate.now().withDayOfMonth(1).minusMonths(monthsBack);
        for (AttendanceRecord r : src) {
            LocalDate d = r.attendanceDate();
            if (first != null && (d.isBefore(first) || !d.isBefore(first.plusMonths(1)))) continue;
            String status = nice(r.status());
            if (statusFilter != null && !statusFilter.startsWith("All") && !statusFilter.equalsIgnoreCase(status)) continue;
            String remarks = reasons.containsKey(d) ? reasons.get(d) : r.remarks() == null || r.remarks().isBlank()
                    ? ("Absent".equals(status) ? "No tap recorded" : "—") : r.remarks();
            rows.add(new Row(d, new Object[]{shortDate(d), time(r.timeIn()), time(r.timeOut()), status, remarks}));
        }
        return rows;
    }

    public static List<Object[]> myTaps(int limit) {
        List<Object[]> out = new ArrayList<>();
        for (ScanLog l : scanDao.findByStudent(myStudentId(), limit))
            out.add(new Object[]{shortDate(l.scanTime().toLocalDate()) + " · " + time(l.scanTime().toLocalTime()), nice(l.result())});
        return out;
    }

    public record MyExcused(int count, int unexcused, String latest, String latestReason, List<Row> rows) {}

    /** Row key = {date, reason, markedBy}. */
    public static MyExcused myExcused() {
        int id = myStudentId();
        List<Row> rows = new ArrayList<>();
        List<ExcusedAbsence> list = excusedDao.findByStudent(id);
        for (ExcusedAbsence e : list) {
            String by = e.markedBy() == null ? "—" : teacherDao.findByUserId(e.markedBy()).map(Teacher::fullName).orElse("—");
            rows.add(new Row(new Object[]{e.absenceDate(), e.reason(), by},
                    new Object[]{longDate(e.absenceDate()), e.reason(), by, "Approved", "View"}));
        }
        int absent = attendanceDao.findSummary(id).map(StudentAttendanceSummary::absentDays).orElse(0);
        return new MyExcused(list.size(), absent, list.isEmpty() ? "—" : list.get(0).absenceDate().format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)),
                list.isEmpty() ? "No excused days yet" : list.get(0).reason(), rows);
    }
}
