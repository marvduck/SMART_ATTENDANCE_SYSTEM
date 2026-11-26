package com.ui;

import javax.swing.*;
import java.awt.*;

public class ExcusedAbsencePanel extends Ui.Page {

    /** Teacher excused-absence preview; database wiring is pending. */

    public ExcusedAbsencePanel() {
        // TODO(db): load advised sections, excused counts and filtered excuses.
        // TODO(db): load section students and absent attendance rows.
        // TODO(db): wire excuse creation, editing and removal.

        block(Ui.header("Workspace / Excused absences", "Excused absences", "Mark, review and edit approved absences for your class."), 0);
        block(Ui.equal(16,
                Ui.statCard("Excused today", "1", "Approved absence", Ui.Kind.CALENDAR),
                Ui.statCard("This week", "3", "16–20 November", Ui.Kind.CHECK),
                Ui.statCard("This month", "6", "November 2026", Ui.Kind.DOC),
                Ui.statCard("Needs an excuse", "2", "Absent, not yet reviewed", Ui.Kind.WARN)), 18);
        block(Ui.cols(16, new double[]{2, 1}, listCard(), formCard()), 16);
        finish();
    }

    private JComponent listCard() {
        Ui.Card c = Ui.card("Excused absence review", "Keep attendance records accurate.", Ui.outline("Export records ↓"));
        // TODO(db): load excusedAbsenceDao.findBySection(...)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Date", "Reason", "Marked by", "Status", "Action"}, new Object[][]{
                {"Dana R. Cruz", "19 Nov 2026", "Medical appointment", "R. Delos Santos", "Approved", "Edit"},
                {"Ethan P. Santos", "17 Nov 2026", "Family emergency", "R. Delos Santos", "Approved", "Edit"},
                {"Bea L. Mendoza", "12 Nov 2026", "Hospital visit", "R. Delos Santos", "Approved", "Edit"},
                {"Hana B. Lopez", "05 Nov 2026", "School activity", "R. Delos Santos", "Approved", "Edit"}}).pills(4).links(5).widths(110, 90, 150, 110, 80, 50);
        c.body.add(Ui.stack(12, Ui.filters(Ui.combo("11 TechPro C"), Ui.combo("November 2026", "October 2026"), Ui.search("Search student", 14)),
                Ui.tableView(t), Ui.footer("Showing 1–4 of 6 excused days", Ui.pager())));
        return c;
    }

    private JComponent formCard() {
        Ui.Card c = Ui.card("Mark excused absence", "Choose an absent student and give a reason.", null);
        JTextArea reason = new JTextArea(4, 18);
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        reason.putClientProperty("JTextField.placeholderText", "Reason for the absence");
        reason.setMargin(new Insets(6, 8, 6, 8));
        c.body.add(Ui.stack(12,
                Ui.labeled("Student", Ui.combo("Carlo S. Garcia", "Gio A. Torres")),
                Ui.labeled("Date", Ui.field("2026-11-19", 12)),
                Ui.labeled("Reason", new JScrollPane(reason)),
                Ui.note("The absence will no longer count toward the school limit.", 250),
                Ui.primary("Mark as excused"), Ui.outline("Clear")), BorderLayout.NORTH);
        return c;
    }
}
