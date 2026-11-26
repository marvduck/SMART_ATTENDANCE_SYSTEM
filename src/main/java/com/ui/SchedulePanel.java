package com.ui;

import javax.swing.*;
import java.awt.*;

public class SchedulePanel extends Ui.Page {

    /** Class-schedule preview; database wiring is pending. */

    public SchedulePanel() {
        // TODO(db): load schedule, section and teacher counts.
        // TODO(db): load/filter schedules and section/teacher choices.
        // TODO(db): wire overlap checks and schedule create/update/delete.

        block(Ui.header("Workspace / Class schedules", "Class schedules", "These schedules are used to verify RFID scans."), 0);
        block(Ui.equal(16,
                Ui.statCard("Schedules", "84", "Across all sections", Ui.Kind.CALENDAR),
                Ui.statCard("Sections covered", "28", "Every section has classes", Ui.Kind.CHECK),
                Ui.statCard("Teachers assigned", "36", "Teaching at least one class", Ui.Kind.PEOPLE),
                Ui.statCard("Early tap window", "15 min", "Before class start", Ui.Kind.CLOCK)), 18);
        block(Ui.cols(16, new double[]{2, 1}, listCard(), formCard()), 16);
        finish();
    }

    private JComponent listCard() {
        Ui.Card c = Ui.card("Schedules", "Taps outside these times are flagged as out of bounds.", Ui.primary("+  Add schedule"));
        // TODO(db): load scheduleDao.findAll()
        Ui.DataTable t = new Ui.DataTable(new String[]{"Section", "Subject", "Day", "Time", "Teacher", "Action"}, new Object[][]{
                {"11 TechPro C", "CompPro", "Mon–Fri", "08:00–10:00 AM", "R. Delos Santos", "Edit"},
                {"11 TechPro C", "Empowerment Tech", "Mon–Fri", "10:15–12:00 PM", "M. Villanueva", "Edit"},
                {"11 TechPro A", "CompPro", "Mon–Fri", "08:00–10:00 AM", "M. Villanueva", "Edit"},
                {"12 TechPro B", "Capstone", "Tue, Thu", "01:00–04:00 PM", "J. Navarro", "Edit"},
                {"10 Sci A", "Biology", "Mon, Wed", "09:00–10:30 AM", "A. Bautista", "Edit"},
                {"10 Sci A", "Chemistry", "Tue, Thu", "09:00–10:30 AM", "A. Bautista", "Edit"}}).links(5).widths(110, 130, 80, 130, 120, 60);
        c.body.add(Ui.stack(12, Ui.filters(Ui.combo("All sections", "11 TechPro C", "11 TechPro A", "12 TechPro B", "10 Sci A"),
                Ui.combo("All days", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")),
                Ui.tableView(t), Ui.footer("Showing 1–6 of 84 schedules", Ui.pager())));
        return c;
    }

    private JComponent formCard() {
        Ui.Card c = Ui.card("Schedule details", "Add a class or edit the selected one.", null);
        c.body.add(Ui.stack(12,
                Ui.labeled("Section", Ui.combo("11 TechPro C", "11 TechPro A", "12 TechPro B", "10 Sci A")),
                Ui.labeled("Teacher", Ui.combo("Ryan D. Delos Santos", "Maria L. Villanueva", "Jose P. Navarro")),
                Ui.labeled("Day", Ui.combo("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")),
                Ui.equal(10, Ui.labeled("Start", Ui.field("08:00", 6)), Ui.labeled("End", Ui.field("10:00", 6))),
                Ui.note("A section cannot have two classes that overlap on the same day.", 250),
                Ui.primary("Save schedule"), Ui.outline("Clear")), BorderLayout.NORTH);
        return c;
    }
}
