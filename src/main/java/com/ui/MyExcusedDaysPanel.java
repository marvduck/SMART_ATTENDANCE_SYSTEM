package com.ui;

import javax.swing.*;
import java.awt.*;

/** Student excused-days preview; database wiring is pending. */
public class MyExcusedDaysPanel extends Ui.Page {

    public MyExcusedDaysPanel() {
        // TODO(db): resolve the student; load excused counts and attendance summary.
        // TODO(db): load the student's excused days.

        block(Ui.header("Workspace / Excused days", "My excused days", "Approved absences recorded by your teacher."), 0);
        block(Ui.equal(16,
                Ui.statCard("Excused days", "1", "This grading period", Ui.Kind.CALENDAR),
                Ui.statCard("Approved requests", "1", "Recorded by your teacher", Ui.Kind.CHECK),
                Ui.statCard("Latest excused date", "12 Nov", "Medical appointment", Ui.Kind.DOC),
                Ui.statCard("Unexcused days", "3", "Not excused", Ui.Kind.WARN)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, recordCard(), howCard()), detailCard()), 16);
        finish();
    }

    private JComponent recordCard() {
        Ui.Card c = Ui.card("Excused absence record", "Only approved excuses are listed here.", null);
        // TODO(db): load excusedAbsenceDao.findByStudent(studentId)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Date", "Reason", "Marked by", "Status", "Action"}, new Object[][]{
                {"12 Nov 2026", "Medical appointment", "R. Delos Santos", "Approved", "View"}})
                .pills(3).links(4).select(0);
        c.body.add(Ui.stack(12,
                Ui.filters(Ui.combo("This grading period", "Last grading period"), Ui.combo("November 2026", "October 2026"),
                        Ui.combo("Approved", "All")),
                Ui.tableView(t), Ui.footer("Showing 1 of 1 excused day", null)));
        return c;
    }

    private JComponent howCard() {
        Ui.Card c = Ui.card("How excused days are recorded", null, null);
        c.body.add(Ui.stack(12,
                step("1", "Tell your class adviser", "Give your reason to your adviser as soon as you can."),
                step("2", "Your teacher reviews it", "The adviser approves the excuse and records the date."),
                step("3", "Check the record here", "Approved days appear on this screen and do not count as unexcused.")),
                BorderLayout.NORTH);
        return c;
    }

    private JComponent step(String n, String title, String desc) {
        JPanel row = Ui.transparent(new BorderLayout(12, 0));
        row.add(Ui.avatar(n, 28), BorderLayout.WEST);
        row.add(Ui.stack(2, Ui.bold(title, 13f), Ui.muted(desc)), BorderLayout.CENTER);
        return row;
    }

    private JComponent detailCard() {
        Ui.Card c = Ui.card("Record details", null, null);
        JButton history = Ui.outline("View attendance history →");
        history.addActionListener(e -> AppShell.go("My attendance"));
        c.body.add(Ui.stack(12,
                Ui.bold("12 November 2026", 16f), Ui.small("Thursday"), Ui.sep(),
                Ui.field("Reason", "Medical appointment"),
                Ui.field("Recorded by", "Ryan D. Delos Santos"),
                Ui.caps("Status"), Ui.flow(0, Ui.pill("Approved", Ui.Tone.ACCENT)),
                Ui.note("Not counted as unexcused. This day does not count toward the school absence limit.", 250),
                Ui.note("Need to update the reason? Contact your class adviser.", 250),
                history), BorderLayout.NORTH);
        return c;
    }
}
