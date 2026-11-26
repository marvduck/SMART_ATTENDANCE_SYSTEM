package com.ui;

import javax.swing.*;
import java.awt.*;

public class MyAttendancePanel extends Ui.Page {

    /** Student attendance preview; database wiring is pending. */

    public MyAttendancePanel() {
        // TODO(db): resolve the student; load summary, history, today's record and active card.
        // TODO(db): load excused days and the recent tap log.

        block(Ui.header("Workspace / My attendance", "My attendance history", "See your daily record and how your attendance is doing this grading period."), 0);
        block(Ui.equal(16,
                Ui.statCard("Attendance rate", "92%", "46 of 50 school days", Ui.Kind.DOC),
                Ui.statCard("Present days", "46", "This grading period", Ui.Kind.CHECK),
                Ui.statCard("Absent days", "3", "Unexcused absences", Ui.Kind.CLOCK),
                Ui.statCard("Excused days", "1", "Approved absence", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, recordCard(), Ui.stack(16, todayCard(), periodCard(), tapsCard())), 16);
        finish();
    }

    private JComponent recordCard() {
        Ui.Card c = Ui.card("Daily attendance record", "Every school day for the selected period.", Ui.outline("Download record ↓"));
        // TODO(db): load attendanceDao.findByStudent(studentId)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Date", "Time in", "Time out", "Status", "Remarks"}, new Object[][]{
                {"Thu, 19 Nov", "08:42 AM", "—", "Present", "—"},
                {"Wed, 18 Nov", "07:48 AM", "04:02 PM", "Present", "—"},
                {"Tue, 17 Nov", "07:52 AM", "04:05 PM", "Present", "—"},
                {"Mon, 16 Nov", "—", "—", "Absent", "No tap recorded"},
                {"Fri, 13 Nov", "07:50 AM", "04:01 PM", "Present", "—"},
                {"Thu, 12 Nov", "—", "—", "Excused", "Medical appointment"},
                {"Wed, 11 Nov", "07:55 AM", "04:03 PM", "Present", "—"},
                {"Tue, 10 Nov", "—", "—", "Absent", "No tap recorded"}}).pills(3).widths(100, 90, 90, 90, 150);
        c.body.add(Ui.stack(12, Ui.filters(Ui.combo("This grading period", "Last grading period"), Ui.combo("November 2026", "October 2026"),
                Ui.combo("All statuses", "Present", "Absent", "Excused")),
                Ui.tableView(t), Ui.footer("Showing 1–8 of 50 school days", Ui.pager())));
        return c;
    }

    private JComponent todayCard() {
        Ui.Card c = Ui.card("Today's status", null, null);
        JComponent times = Ui.equal(10,
                Ui.stack(3, Ui.small("Time in"), Ui.label("08:42 AM", Font.BOLD, 22f, Ui.p().text())),
                Ui.stack(3, Ui.small("Time out"), Ui.label("Not yet", Font.PLAIN, 18f, Ui.p().muted())));
        c.body.add(Ui.stack(12, Ui.flow(0, Ui.pill("●  On campus", Ui.Tone.ACCENT)), times), BorderLayout.NORTH);
        return c;
    }

    private JComponent periodCard() {
        Ui.Card c = Ui.card("This grading period", null, null);
        c.body.add(Ui.stack(10, Ui.progress(0.92, Ui.p().accent()),
                Ui.kv("Present", "46 days"), Ui.kv("Absent", "3 days"), Ui.kv("Excused", "1 day"),
                Ui.link("View excused days →", "Excused days")), BorderLayout.NORTH);
        return c;
    }

    private JComponent tapsCard() {
        Ui.Card c = Ui.card("Recent RFID taps", "Your latest card activity.", null);
        // TODO(db): load scanLogDao.findByStudent(studentId, 5)
        c.body.add(Ui.stack(8,
                Ui.kv("Thu, 19 Nov · 08:42 AM", "Time in"),
                Ui.kv("Wed, 18 Nov · 04:02 PM", "Time out"),
                Ui.kv("Wed, 18 Nov · 07:48 AM", "Time in")), BorderLayout.NORTH);
        return c;
    }
}
