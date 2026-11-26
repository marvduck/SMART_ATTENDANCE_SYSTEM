package com.ui;

import javax.swing.*;
import java.awt.*;

public class ClassAttendancePanel extends Ui.Page {

    /** Teacher class-attendance preview; database wiring is pending. */

    public ClassAttendancePanel() {
        // TODO(db): load advised sections, today's schedule, counts and class rows.
        // TODO(db): load selected attendance/excuse details.
        // TODO(db): wire excuses and manual attendance corrections.

        block(Ui.header("Workspace / Class attendance", "Class attendance", "Review today's class list and keep records accurate."), 0);
        block(Ui.equal(16,
                Ui.statCard("Class strength", "42", "11 TechPro C · CompPro", Ui.Kind.PEOPLE),
                Ui.statCard("Present today", "38", "90.5% attendance rate", Ui.Kind.CHECK),
                Ui.statCard("Absent today", "3", "Review class attendance", Ui.Kind.CLOCK),
                Ui.statCard("Excused today", "1", "Approved absence", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, listCard(), detailCard()), 16);
        finish();
    }

    private JComponent listCard() {
        Ui.Card c = Ui.card("11 TechPro C · CompPro", "Thursday, 19 November · 08:00–10:00 AM", Ui.outline("Export records ↓"));
        // TODO(db): load attendanceDao.findRows(...)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Time in", "Time out", "Status", "Action"}, new Object[][]{
                {"Alex M. Reyes", "08:42 AM", "—", "Present", "View"},
                {"Bea L. Mendoza", "07:51 AM", "—", "Present", "View"},
                {"Carlo S. Garcia", "—", "—", "Absent", "Mark excused"},
                {"Dana R. Cruz", "—", "—", "Excused", "View reason"},
                {"Ethan P. Santos", "07:49 AM", "—", "Present", "View"},
                {"Faith D. Ramos", "08:05 AM", "—", "Late", "View"},
                {"Gio A. Torres", "—", "—", "Absent", "Mark excused"}}).pills(3).links(4).select(3);
        JPanel bar = Ui.transparent(new BorderLayout());
        bar.add(Ui.filters(Ui.combo("11 TechPro C"), Ui.combo("Today", "Yesterday", "This week"), Ui.search("Search student", 12),
                Ui.combo("All statuses", "Present", "Late", "Absent", "Excused")), BorderLayout.WEST);
        bar.add(Ui.pill("38 / 42 present", Ui.Tone.ACCENT), BorderLayout.EAST);
        c.body.add(Ui.stack(12, bar, Ui.tableView(t), Ui.footer("Showing 1–7 of 42 students", Ui.pager())));
        return c;
    }

    private JComponent detailCard() {
        Ui.Card c = Ui.card("Attendance details", null, null);
        JPanel head = Ui.transparent(new BorderLayout(12, 0));
        head.add(Ui.avatar("DC", 42), BorderLayout.WEST);
        head.add(Ui.stack(2, Ui.bold("Dana R. Cruz", 15f), Ui.small("11 TechPro C · Thu, 19 Nov 2026")), BorderLayout.CENTER);
        JPanel st = Ui.transparent(new BorderLayout());
        st.add(Ui.pill("Excused", Ui.Tone.ACCENT), BorderLayout.WEST);
        JButton manage = Ui.primary("Manage excused absence →");
        manage.addActionListener(e -> AppShell.go("Excused absences"));
        c.body.add(Ui.stack(12, head, Ui.sep(),
                Ui.caps("Status"), st,
                Ui.field("Reason", "Medical appointment"),
                Ui.field("Marked by", "Ryan D. Delos Santos"),
                Ui.note("Keep the class record accurate. Excused days do not count toward the absence limit.", 250),
                manage), BorderLayout.NORTH);
        return c;
    }
}
