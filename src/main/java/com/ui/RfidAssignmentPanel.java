package com.ui;

import javax.swing.*;
import java.awt.*;

public class RfidAssignmentPanel extends Ui.Page {

    /** RFID assignment preview; database wiring is pending. */

    public RfidAssignmentPanel() {
        // TODO(db): load card/student counts and recent invalid scans.
        // TODO(db): load/search cards and the latest invalid attempt.
        // TODO(db): wire registration, replacement, status changes and deletion.

        block(Ui.header("Workspace / RFID cards", "RFID card management", "Link cards to students, replace lost cards and review invalid scans."), 0);
        block(Ui.equal(16,
                Ui.statCard("Linked cards", "1,236", "Active and in use", Ui.Kind.CARD),
                Ui.statCard("Unregistered", "12", "Students without a card", Ui.Kind.WARN),
                Ui.statCard("Lost / disabled", "8", "Not accepted at the gate", Ui.Kind.CLOCK),
                Ui.statCard("Invalid scans", "1", "Today", Ui.Kind.CALENDAR)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, assignmentsCard(), invalidCard()), registerCard()), 16);
        finish();
    }

    private JComponent assignmentsCard() {
        Ui.Card c = Ui.card("Student card assignments", "Every student and the card linked to them.", Ui.outline("Export list ↓"));
        // TODO(db): load rfidCardDao.search(keyword)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Section", "Card UID", "Status", "Last tap", "Action"}, new Object[][]{
                {"Alex M. Reyes", "11 TechPro C", "04:A1:7C:2109", "Active", "08:42 AM", "Manage"},
                {"Bea L. Mendoza", "11 TechPro A", "04:B3:19:7741", "Active", "08:41 AM", "Manage"},
                {"Carlo S. Garcia", "12 TechPro B", "04:C8:5E:3320", "Active", "08:40 AM", "Manage"},
                {"Ethan P. Santos", "11 TechPro C", "—", "Unregistered", "—", "Register"},
                {"Gio A. Torres", "10 Sci A", "04:D2:90:1186", "Lost", "Yesterday", "Replace"},
                {"Hana B. Lopez", "10 Sci A", "04:E6:44:5902", "Disabled", "12 Nov", "Manage"}}).pills(3).links(5).widths(120, 100, 120, 90, 80, 70).select(3);
        c.body.add(Ui.stack(12, Ui.filters(Ui.search("Search student or card UID", 18), Ui.combo("All cards", "Active", "Lost", "Disabled", "Unregistered")),
                Ui.tableView(t), Ui.footer("Showing 1–6 of 1,248 students", Ui.pager())));
        return c;
    }

    private JComponent invalidCard() {
        Ui.Card c = Ui.card("Latest invalid card attempt", "Unregistered card tapped at the main gate.", Ui.pill("Invalid card", Ui.Tone.DANGER));
        JPanel row = Ui.transparent(new BorderLayout(12, 0));
        row.add(new JLabel(Ui.icon(Ui.Kind.CARD, 22, Ui.p().danger())), BorderLayout.WEST);
        row.add(Ui.stack(3, Ui.bold("UID 8A:4F:21:09", 13f), Ui.small("No student linked · 08:41:07 AM")), BorderLayout.CENTER);
        row.add(Ui.flow(16, Ui.link("Review attempt →", "Alerts & thresholds"), Ui.link("Register card →", null)), BorderLayout.EAST);
        c.body.add(row);
        return c;
    }

    private JComponent registerCard() {
        Ui.Card c = Ui.card("Register RFID card", "Assign a card to a student who has none.", null);
        JPanel status = Ui.transparent(new BorderLayout(8, 0));
        status.add(new JLabel(Ui.icon(Ui.Kind.CLOCK, 16, Ui.p().accent())), BorderLayout.WEST);
        status.add(Ui.label("Waiting for card tap…", Font.BOLD, 12.5f, Ui.p().accent()), BorderLayout.CENTER);
        c.body.add(Ui.stack(12,
                Ui.labeled("1 · Student", Ui.combo("Ethan P. Santos · 11 TechPro C", "Students without a card…")),
                Ui.labeled("2 · Card UID", Ui.field("Ready to scan", 16)),
                Ui.box(status),
                Ui.note("Hold the card against the reader on the desk. The UID fills in automatically.", 250),
                Ui.primary("Register card"), Ui.outline("Cancel registration")), BorderLayout.NORTH);
        return c;
    }
}
