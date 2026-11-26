package com.ui;

import javax.swing.*;
import java.awt.*;

public class ThresholdPanel extends Ui.Page {

    /** Teacher threshold preview; database wiring is pending. */

    public ThresholdPanel() {
        // TODO(db): load limits, advised sections and the absence watchlist.
        // TODO(db): load student attendance and excuses for follow-up.

        block(Ui.header("Workspace / Threshold watch", "Threshold watch", "Spot students who are approaching or over the absence limit."), 0);
        block(Ui.equal(16,
                Ui.statCard("Students on watch", "3", "Assigned sections", Ui.Kind.PEOPLE),
                Ui.statCard("At review level", "2", "4 unexcused absences", Ui.Kind.WARN),
                Ui.statCard("At watch level", "1", "3 unexcused absences", Ui.Kind.CLOCK),
                Ui.statCard("Excused days", "2", "Not counted", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, watchCard(), settingsCard()), followUpCard()), 16);
        finish();
    }

    private JComponent watchCard() {
        Ui.Card c = Ui.card("Absence watchlist", "Unexcused absences this grading period.", Ui.outline("Export list ↓"));
        // TODO(db): load attendanceDao.findByMinAbsences(...)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Section", "Unexcused", "Level", "Action"}, new Object[][]{
                {"Carlo S. Garcia", "11 TechPro C", "4", "Review", "Review →"},
                {"Faith D. Ramos", "11 TechPro C", "4", "Review", "Review →"},
                {"Gio A. Torres", "11 TechPro C", "3", "Watch", "Review →"}}).pills(3).links(4).select(0);
        c.body.add(Ui.stack(12, Ui.filters(Ui.combo("Assigned sections", "11 TechPro C"), Ui.combo("All levels", "Watch", "Review", "School limit")),
                Ui.tableView(t)));
        return c;
    }

    private JComponent settingsCard() {
        Ui.Card c = Ui.card("School threshold settings", "Example limits. Admins set the real ones.", null);
        c.body.add(Ui.equal(12,
                level("3 days", "Watch", "Student appears on your list."),
                level("4 days", "Review", "Follow up with the student."),
                level("5 days", "School limit", "Adviser and admin are alerted.")));
        return c;
    }

    private JComponent level(String days, String name, String desc) {
        return Ui.box(Ui.stack(3, Ui.label(days, Font.BOLD, 20f, Ui.p().text()), Ui.bold(name, 12.5f), Ui.wrap(desc, 120, Ui.p().muted(), 11f)));
    }

    private JComponent followUpCard() {
        Ui.Card c = Ui.card("Student follow-up", null, null);
        JPanel head = Ui.transparent(new BorderLayout(12, 0));
        head.add(Ui.avatar("CG", 42), BorderLayout.WEST);
        head.add(Ui.stack(2, Ui.bold("Carlo S. Garcia", 15f), Ui.small("11 TechPro C")), BorderLayout.CENTER);
        JPanel pillRow = Ui.transparent(new BorderLayout());
        pillRow.add(Ui.pill("4 unexcused absences", Ui.Tone.WARN), BorderLayout.WEST);
        c.body.add(Ui.stack(12, head, pillRow, Ui.sep(),
                Ui.caps("Recent absences"),
                Ui.kv("Thu, 19 Nov", "Absent"), Ui.kv("Tue, 17 Nov", "Absent"),
                Ui.kv("Tue, 10 Nov", "Absent"), Ui.kv("Thu, 5 Nov", "Absent"),
                Ui.box(Ui.stack(2, Ui.caps("Parent / guardian contact"), Ui.bold("Not yet contacted", 12.5f))),
                Ui.primary("Log follow-up"), Ui.outline("View attendance record →")), BorderLayout.NORTH);
        return c;
    }
}
