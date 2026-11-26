package com.ui;

import javax.swing.*;
import java.awt.*;

public class ReportsPanel extends Ui.Page {

    /** Attendance report preview; database wiring is pending. */

    public ReportsPanel() {
        // TODO(db): load counts, threshold list, summaries, rows and weekly rates.
        // TODO(db): load role-appropriate sections.
        // TODO(db): export attendance, excuse and scan rows to CSV/PDF.

        block(Ui.header("Workspace / Reports", "Reports", "Daily and weekly summaries you can export."), 0);
        block(Ui.equal(16,
                Ui.statCard("Average attendance", "93.0%", "This week", Ui.Kind.CHECK),
                Ui.statCard("Total absences", "68", "Today", Ui.Kind.CLOCK),
                Ui.statCard("Excused absences", "24", "Today", Ui.Kind.CALENDAR),
                Ui.statCard("Over the limit", "5", "Students to review", Ui.Kind.WARN)), 18);
        block(Ui.cols(16, new double[]{2, 1}, summaryCard(), trendCard()), 16);
        finish();
    }

    private JComponent summaryCard() {
        Ui.Card c = Ui.card("Daily summary", "Attendance per section for the selected day.", Ui.primary("Export report ↓"));
        // TODO(db): load attendanceDao.countsForDate(date, sectionId) per section
        Ui.DataTable t = new Ui.DataTable(new String[]{"Section", "Enrolled", "Present", "Absent", "Excused", "Rate"}, new Object[][]{
                {"11 TechPro A", "44", "41", "2", "1", "93.2%"},
                {"11 TechPro C", "42", "38", "3", "1", "90.5%"},
                {"12 TechPro B", "40", "37", "2", "1", "92.5%"},
                {"10 Sci A", "38", "36", "1", "1", "94.7%"},
                {"10 Sci B", "39", "35", "3", "1", "89.7%"}});
        c.body.add(Ui.stack(12, Ui.filters(Ui.combo("Daily summary", "Weekly summary", "Absence list", "Excused absences"),
                Ui.combo("All sections", "11 TechPro A", "11 TechPro C", "12 TechPro B"), Ui.field("Thu, 19 Nov 2026", 12)),
                Ui.tableView(t), Ui.footer("Showing 5 sections", Ui.link("Export as CSV →", null))));
        return c;
    }

    private JComponent trendCard() {
        Ui.Card c = Ui.card("Weekly trend", "Attendance rate, Monday to Friday.", Ui.pill("This week", Ui.Tone.NEUTRAL));
        c.body.add(new Ui.BarChart(new String[]{"Mon", "Tue", "Wed", "Thu", "Fri"}, new Double[]{94.1, 91.8, 93.4, 92.6, null}, 230));
        return c;
    }
}
