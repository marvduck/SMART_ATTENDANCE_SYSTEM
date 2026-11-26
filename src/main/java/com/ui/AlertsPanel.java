package com.ui;

import javax.swing.*;
import java.awt.*;

public class AlertsPanel extends Ui.Page {

    /** Admin alerts and threshold preview; database wiring is pending. */

    public AlertsPanel() {
        // TODO(db): load alert counts, the threshold list and filtered alerts.
        // TODO(db): wire mark-read, delete and cleanup actions.
        // TODO(db): load and save threshold/warning settings.

        block(Ui.header("Workspace / Alerts & thresholds", "Alerts & thresholds", "Review exceptions and set the school absence limit."), 0);
        block(Ui.equal(16,
                Ui.statCard("Open alerts", "8", "Waiting for review", Ui.Kind.WARN),
                Ui.statCard("Invalid cards", "1", "Unregistered UID today", Ui.Kind.CARD),
                Ui.statCard("Out of bounds", "2", "Schedule exceptions", Ui.Kind.CLOCK),
                Ui.statCard("Near the limit", "5", "Students to review", Ui.Kind.PEOPLE)), 18);
        block(Ui.cols(16, new double[]{2, 1}, alertsCard(), settingsCard()), 16);
        finish();
    }

    private JComponent alertsCard() {
        Ui.Card c = Ui.card("Alerts", "Newest first. Mark an alert read once it is handled.", Ui.outline("Mark all read"));
        // TODO(db): load alertDao.findRecent(50)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Type", "Details", "Time", "Status", "Action"}, new Object[][]{
                {"Invalid card", "No student linked to UID 8A:4F:21:09", "08:41 AM", "Unread", "Mark read"},
                {"Out of bounds", "Carlo S. Garcia tapped outside the 12 TechPro B schedule", "08:40 AM", "Unread", "Mark read"},
                {"Out of bounds", "Gio A. Torres tapped outside the 10 Sci A schedule", "07:12 AM", "Unread", "Mark read"},
                {"Absence limit", "Faith D. Ramos is 2 absences from the limit", "Yesterday", "Unread", "Mark read"},
                {"Absence limit", "Carlo S. Garcia is 2 absences from the limit", "Yesterday", "Read", "Delete"}}).pills(0, 3).links(4).widths(110, 300, 80, 80, 80);
        c.body.add(Ui.stack(12, Ui.filters(Ui.combo("All types", "Invalid card", "Out of bounds", "Absence limit"), Ui.combo("Unread first", "Unread only", "All")),
                Ui.tableView(t), Ui.footer("Showing 5 of 8 alerts", Ui.link("Clear read alerts", null))));
        return c;
    }

    private JComponent settingsCard() {
        Ui.Card c = Ui.card("Absence limit settings", "Applies to every section.", null);
        c.body.add(Ui.stack(12,
                Ui.labeled("Absence threshold (days)", Ui.field("10", 8)),
                Ui.small("Unexcused absences allowed before the drop flag."),
                Ui.labeled("Warning offset (days)", Ui.field("2", 8)),
                Ui.small("Students are flagged this many days before the threshold."),
                Ui.note("Teachers see these limits on their Threshold watch screen.", 250),
                Ui.primary("Save settings")), BorderLayout.NORTH);
        return c;
    }
}
