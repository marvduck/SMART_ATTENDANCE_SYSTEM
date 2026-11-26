package com.ui;

import javax.swing.*;
import java.awt.*;

public class TeacherDashboard extends Ui.Page {

    /** Teacher dashboard preview; database wiring is pending. */

    public TeacherDashboard() {
        // TODO(db): resolve the teacher, advised sections and today's schedule.
        // TODO(db): load class counts, rows and weekly rates.
        // TODO(db): load threshold watch and excused-absence review.

        block(Ui.header("Workspace / Overview", "Your class at a glance", "Monitor attendance and support students who need it."), 0);
        block(Ui.equal(16,
                Ui.statCard("Class strength", "42", "11 TechPro C · CompPro", Ui.Kind.PEOPLE),
                Ui.statCard("Present today", "38", "90.5% attendance rate", Ui.Kind.CHECK),
                Ui.statCard("Absent today", "3", "Review class attendance", Ui.Kind.CLOCK),
                Ui.statCard("Excused today", "1", "Approved absence", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, classCard(), weeklyCard()), sideCard()), 16);
        finish();
    }

    private JComponent classCard() {
        Ui.Card c = Ui.card("Class attendance", "Thursday, 19 November · 08:00–10:00 AM", Ui.outline("Export records ↓"));
        // TODO(db): load attendanceDao.findRows(...)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Time in", "Time out", "Status", "Action"}, new Object[][]{
                {"Alex M. Reyes", "08:42 AM", "—", "Present", "View"},
                {"Bea L. Mendoza", "07:51 AM", "—", "Present", "View"},
                {"Carlo S. Garcia", "—", "—", "Absent", "Mark excused"},
                {"Dana R. Cruz", "—", "—", "Excused", "View reason"},
                {"Ethan P. Santos", "07:49 AM", "—", "Present", "View"}}).pills(3).links(4);
        JPanel bar = Ui.transparent(new BorderLayout());
        bar.add(Ui.filters(Ui.combo("11 TechPro C"), Ui.combo("Today", "Yesterday", "This week"), Ui.search("Search student", 14)), BorderLayout.WEST);
        bar.add(Ui.pill("38 / 42 present", Ui.Tone.ACCENT), BorderLayout.EAST);
        c.body.add(Ui.stack(12, bar, Ui.tableView(t), Ui.footer("Showing 1–5 of 42 students", Ui.pager())));
        return c;
    }

    private JComponent weeklyCard() {
        Ui.Card c = Ui.card("Weekly class summary", "16–20 November 2026", null);
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri"};
        String[] vals = {"95.2%", "92.9%", "97.6%", "90.5%", null};
        double[] fr = {0.952, 0.929, 0.976, 0.905, 0};
        JComponent[] cols = new JComponent[5];
        for (int i = 0; i < 5; i++) {
            JLabel v = vals[i] == null ? Ui.label("Upcoming", Font.PLAIN, 17f, Ui.p().muted()) : Ui.label(vals[i], Font.BOLD, 22f, Ui.p().text());
            cols[i] = Ui.stack(6, Ui.small(days[i]), v, Ui.progress(fr[i], Ui.p().accent()));
        }
        c.body.add(Ui.equal(22, cols));
        return c;
    }

    private JComponent sideCard() {
        Ui.Card c = Ui.card("Threshold watch", "Students approaching the absence limit.", Ui.pill("3 students", Ui.Tone.WARN));
        JPanel excused = Ui.stack(3, Ui.bold("Dana R. Cruz", 13f), Ui.muted("19 Nov 2026 · Medical appointment"),
                Ui.label("Approved · Recorded today", Font.PLAIN, 11f, Ui.p().accent()));
        JButton manage = Ui.primary("Manage excused absences →");
        manage.addActionListener(e -> AppShell.go("Excused absences"));
        c.body.add(Ui.stack(10,
                watchRow("Carlo S. Garcia", "4 unexcused absences"), Ui.sep(),
                watchRow("Faith D. Ramos", "4 unexcused absences"), Ui.sep(),
                watchRow("Gio A. Torres", "3 unexcused absences"), Ui.sep(),
                Ui.small("Limits follow school settings."), Ui.sep(),
                Ui.bold("Excused absence review", 15f), Ui.muted("Keep attendance records accurate."),
                Ui.box(excused), manage), BorderLayout.NORTH);
        return c;
    }

    private JComponent watchRow(String name, String detail) {
        JPanel r = Ui.transparent(new BorderLayout(8, 0));
        r.add(Ui.stack(2, Ui.bold(name, 13f), Ui.label(detail, Font.PLAIN, 12f, Ui.p().warning())), BorderLayout.CENTER);
        JPanel right = Ui.transparent(new GridBagLayout());
        right.add(Ui.link("Review →", "Threshold watch"));
        r.add(right, BorderLayout.EAST);
        return r;
    }
}
