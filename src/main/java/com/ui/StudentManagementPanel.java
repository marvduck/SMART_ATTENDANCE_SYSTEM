package com.ui;

import javax.swing.*;
import java.awt.*;

public class StudentManagementPanel extends Ui.Page {

    /** Student management preview; database wiring is pending. */

    public StudentManagementPanel() {
        // TODO(db): load student/card/section counts and search/filter students.
        // TODO(db): load student, card and attendance details.
        // TODO(db): wire account creation, profile edits and deletion.

        block(Ui.header("Workspace / Students", "Student directory", "Manage student records, login accounts and RFID card links."), 0);
        block(Ui.equal(16,
                Ui.statCard("Enrolled students", "1,248", "Across 28 sections", Ui.Kind.PEOPLE),
                Ui.statCard("RFID cards active", "1,236", "99.0% of enrolled", Ui.Kind.CARD),
                Ui.statCard("Cards to assign", "12", "Students without a card", Ui.Kind.WARN),
                Ui.statCard("Sections", "28", "This school year", Ui.Kind.BOOK)), 18);
        block(Ui.cols(16, new double[]{2, 1}, listCard(), profileCard()), 16);
        finish();
    }

    private JComponent listCard() {
        Ui.Card c = Ui.card("Enrolled students", "Search, filter and open a student record.", Ui.primary("+  Add student"));
        // TODO(db): load studentDao.search(...)
        Ui.DataTable t = new Ui.DataTable(new String[]{"Student", "Section", "RFID", "Status", "Action"}, new Object[][]{
                {"Alex M. Reyes", "11 TechPro C", "Linked", "Active", "View"},
                {"Bea L. Mendoza", "11 TechPro A", "Linked", "Active", "View"},
                {"Carlo S. Garcia", "12 TechPro B", "Linked", "Active", "View"},
                {"Dana R. Cruz", "11 TechPro C", "Linked", "Active", "View"},
                {"Ethan P. Santos", "11 TechPro C", "No card", "Active", "View"},
                {"Faith D. Ramos", "12 TechPro B", "Linked", "Active", "View"},
                {"Gio A. Torres", "10 Sci A", "Lost", "Active", "View"},
                {"Hana B. Lopez", "10 Sci A", "Linked", "Active", "View"}}).pills(2, 3).links(4).select(0);
        JPanel bar = Ui.transparent(new BorderLayout());
        bar.add(Ui.filters(Ui.search("Search name or LRN", 16), Ui.combo("All grades", "Grade 7", "Grade 8", "Grade 9", "Grade 10", "Grade 11", "Grade 12"),
                Ui.combo("All RFID status", "Linked", "No card", "Lost", "Disabled")), BorderLayout.WEST);
        bar.add(Ui.small("1,248 results"), BorderLayout.EAST);
        c.body.add(Ui.stack(12, bar, Ui.tableView(t), Ui.footer("Showing 1–8 of 1,248 students", Ui.pager())));
        return c;
    }

    private JComponent profileCard() {
        Ui.Card c = Ui.card("Student profile", null, null);
        JPanel head = Ui.transparent(new BorderLayout(12, 0));
        head.add(Ui.avatar("AR", 46), BorderLayout.WEST);
        head.add(Ui.stack(2, Ui.bold("Alex M. Reyes", 16f), Ui.small("LRN 109283746510")), BorderLayout.CENTER);
        JButton edit = Ui.primary("Edit student");
        JButton card = Ui.outline("Manage RFID card →");
        card.addActionListener(e -> AppShell.go("RFID cards"));
        c.body.add(Ui.stack(12, head, Ui.sep(),
                Ui.field("Section", "11 TechPro C · CompPro"),
                Ui.field("Adviser", "Ryan D. Delos Santos"),
                Ui.field("RFID card", "Card ending 2109 · Linked"),
                Ui.field("Login account", "alex.reyes · Active"),
                Ui.note("RFID card is active. Attendance is recorded automatically at the gate.", 250),
                edit, card), BorderLayout.NORTH);
        return c;
    }
}
