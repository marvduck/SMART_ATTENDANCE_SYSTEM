package com.ui;

import javax.swing.*;
import java.awt.*;
import java.time.YearMonth;

public class StudentDashboard extends Ui.Page {

    /** Student dashboard preview; database wiring is pending. */

    public StudentDashboard() {
        // TODO(db): resolve the student; load summary, calendar and excused days.
        // TODO(db): load today's attendance, schedule, active card and absence limit.

        block(Ui.header("Workspace / Overview", "My attendance", "Every school day counts. Here's how you're doing."), 0);
        block(Ui.equal(16,
                Ui.statCard("Attendance rate", "92%", "46 of 50 school days", Ui.Kind.DOC),
                Ui.statCard("Present days", "46", "This grading period", Ui.Kind.CHECK),
                Ui.statCard("Absent days", "3", "Unexcused absences", Ui.Kind.CLOCK),
                Ui.statCard("Excused days", "1", "Approved absence", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, calendarCard(), recentCard()), Ui.stack(16, todayCard(), summaryCard())), 16);
        finish();
    }

    private JComponent calendarCard() {
        JPanel trailing = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        trailing.add(Ui.legend(Ui.p().accent(), "Present"));
        trailing.add(Ui.legend(Ui.p().warning(), "Absent"));
        trailing.add(Ui.legend(Ui.p().muted(), "Excused"));
        trailing.add(Ui.label("‹", Font.PLAIN, 16f, Ui.p().muted()));
        trailing.add(Ui.label("›", Font.PLAIN, 16f, Ui.p().muted()));
        Ui.Card c = Ui.card("Attendance calendar", "November 2026", trailing);
        // TODO(db): load the student's records for the month
        JComponent cal = Ui.monthCalendar(YearMonth.of(2026, 11),
                Ui.states("2P 3P 4P 5A 6P 9P 10A 11P 12E 13P 16A 17P 18P 19P"), 19);
        JLabel rfid = Ui.small("Attendance is recorded automatically via RFID.");
        c.body.add(Ui.stack(8, cal, Ui.footer("Today: 19 Nov 2026", rfid)));
        return c;
    }

    private JComponent recentCard() {
        Ui.Card c = Ui.card("Recent attendance", null, Ui.link("View full record →", "My attendance"));
        // TODO(db): load attendanceDao.findByStudent(studentId) (first rows)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Date", "Time in", "Time out", "Status"}, new Object[][]{
                {"Thu, 19 Nov", "08:42 AM", "—", "Present"},
                {"Wed, 18 Nov", "07:48 AM", "04:02 PM", "Present"},
                {"Tue, 17 Nov", "07:52 AM", "04:05 PM", "Present"},
                {"Mon, 16 Nov", "—", "—", "Absent"}}).pills(3);
        c.body.add(Ui.tableView(t));
        return c;
    }

    private JComponent todayCard() {
        Ui.Card c = Ui.card("Today's status", null, null);
        JComponent times = Ui.equal(10,
                Ui.stack(3, Ui.small("Time in"), Ui.label("08:42 AM", Font.BOLD, 22f, Ui.p().text())),
                Ui.stack(3, Ui.small("Time out"), Ui.label("Not yet", Font.PLAIN, 18f, Ui.p().muted())));
        c.body.add(Ui.stack(12, Ui.flow(0, Ui.pill("●  On campus", Ui.Tone.ACCENT)), times, Ui.sep(),
                Ui.plain("11 TechPro C · CompPro"), Ui.small("Class schedule  08:00 AM–04:00 PM"),
                Ui.label("RFID card ending in 2109 · Active", Font.PLAIN, 11f, Ui.p().accent())), BorderLayout.NORTH);
        return c;
    }

    private JComponent summaryCard() {
        Ui.Card c = Ui.card("Your absence summary", null, null);
        JPanel excusedHead = Ui.transparent(new BorderLayout());
        excusedHead.add(Ui.bold("Excused days", 15f), BorderLayout.WEST);
        excusedHead.add(Ui.pill("1 day", Ui.Tone.NEUTRAL), BorderLayout.EAST);
        c.body.add(Ui.stack(8,
                Ui.label("3 unexcused days this grading period", Font.BOLD, 12.5f, Ui.p().warning()),
                Ui.small("Stay on track with regular attendance."), Ui.sep(),
                excusedHead, Ui.bold("12 November 2026", 13f), Ui.muted("Medical appointment"),
                Ui.label("Approved by your teacher", Font.PLAIN, 11f, Ui.p().accent()), Ui.gap(4),
                Ui.note("Need a correction? Contact your class adviser.", 250)), BorderLayout.NORTH);
        return c;
    }
}
