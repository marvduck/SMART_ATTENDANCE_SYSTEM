package com.ui;

import javax.swing.*;
import java.awt.*;

public class AdminDashboard extends Ui.Page {

    /** Admin overview preview; database wiring is pending. */

    public AdminDashboard() {
        // TODO(db): load enrolled/section counts and today's attendance counts.
        // TODO(db): load weekly rates, recent scans, unread alerts and threshold count.
        // TODO(db): show JDBC.testConnection() status.

        block(Ui.header("Workspace / Overview", "Attendance overview", "A real-time view of your school, in one place."), 0);
        block(Ui.equal(16,
                Ui.statCard("Enrolled students", "1,248", "Across 28 sections", Ui.Kind.PEOPLE),
                Ui.statCard("Present today", "1,156", "92.6% attendance rate", Ui.Kind.CHECK),
                Ui.statCard("Absent today", "68", "5.4% of enrolled students", Ui.Kind.CLOCK),
                Ui.statCard("Excused today", "24", "1.9% of enrolled students", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, weeklyCard(), liveCard()), attentionCard()), 16);
        finish();
    }

    private JComponent weeklyCard() {
        Ui.Card c = Ui.card("Weekly attendance", "Attendance rate across all sections", Ui.pill("This week", Ui.Tone.NEUTRAL));
        JPanel legend = Ui.transparent(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        legend.add(Ui.legend(Ui.p().accent(), "Present"));
        legend.add(Ui.legend(Ui.p().muted(), "Absent / excused"));
        // TODO(db): load attendanceDao.dailyRates(...)
        Ui.BarChart chart = new Ui.BarChart(new String[]{"Mon 16", "Tue 17", "Wed 18", "Thu 19", "Fri"},
                new Double[]{94.1, 91.8, 93.4, 92.6, null}, 190);
        c.body.add(Ui.stack(4, legend, chart));
        return c;
    }

    private JComponent liveCard() {
        Ui.Card c = Ui.card("Live RFID activity", "Latest student taps · Main gate", Ui.pill("Live", Ui.Tone.ACCENT));
        // TODO(db): load scanLogDao.findRecent(4)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Section", "Time", "Event"}, new Object[][]{
                {"Alex M. Reyes", "11 TechPro C", "08:42:16", "Time in"},
                {"Bea L. Mendoza", "11 TechPro A", "08:41:52", "Time in"},
                {"Carlo S. Garcia", "12 TechPro B", "08:40:31", "Out of bounds"},
                {"Dana R. Cruz", "11 TechPro C", "08:39:08", "Time in"}}).pills(3);
        c.body.add(Ui.stack(0, Ui.tableView(t), Ui.footer("Showing 4 latest events", Ui.link("View all activity →", "RFID cards"))));
        return c;
    }

    private JComponent attentionCard() {
        Ui.Card c = Ui.card("Needs attention", "Review exceptions before the day ends.", Ui.pill("8 open", Ui.Tone.WARN));
        JButton reg = Ui.primary("Register RFID card");
        reg.setIcon(Ui.icon(Ui.Kind.CARD, 16, Ui.p().onAccent()));
        reg.addActionListener(e -> AppShell.go("RFID cards"));
        JButton rep = Ui.outline("Generate report");
        rep.setIcon(Ui.icon(Ui.Kind.DOC, 16, Ui.p().text()));
        rep.addActionListener(e -> AppShell.go("Reports"));
        c.body.add(Ui.stack(12,
                item(Ui.Kind.WARN, "Absence threshold", "5 students need review", "Approaching the configured absence limit.", "View threshold list →", "Alerts & thresholds"),
                Ui.sep(),
                item(Ui.Kind.WARN, "Out-of-bounds activity", "2 schedule exceptions", "Taps recorded outside assigned schedules.", "Review activity →", "RFID cards"),
                Ui.sep(),
                item(Ui.Kind.CARD, "Unregistered RFID card", "1 invalid card attempt", "No student linked to UID 8A:4F:21:09.", "Review card attempt →", "RFID cards"),
                Ui.gap(6), Ui.caps("Quick actions"), reg, rep), BorderLayout.NORTH);
        return c;
    }

    private JComponent item(Ui.Kind kind, String title, String line, String desc, String linkText, String target) {
        JPanel row = Ui.transparent(new BorderLayout(10, 0));
        JPanel icon = Ui.transparent(new BorderLayout());
        icon.add(new JLabel(Ui.icon(kind, 18, Ui.p().warning())), BorderLayout.NORTH);
        row.add(icon, BorderLayout.WEST);
        row.add(Ui.stack(3, Ui.bold(title, 13f), Ui.plain(line), Ui.small(desc), Ui.link(linkText, target)), BorderLayout.CENTER);
        return row;
    }
}
