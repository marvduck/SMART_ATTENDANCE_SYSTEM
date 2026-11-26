package com.ui;

import javax.swing.*;
import java.awt.*;

public class TeacherSectionPanel extends Ui.Page {

    /** Teacher and section management preview; database wiring is pending. */

    public TeacherSectionPanel() {
        // TODO(db): load teacher/section counts, adviser gaps and filtered lists.
        // TODO(db): wire teacher accounts, profile edits and section/adviser actions.

        block(Ui.header("Workspace / Teachers & sections", "Teachers & sections", "Manage teachers, sections and advisers."), 0);
        block(Ui.equal(16,
                Ui.statCard("Teachers", "36", "Active accounts", Ui.Kind.PEOPLE),
                Ui.statCard("Sections", "28", "This school year", Ui.Kind.BOOK),
                Ui.statCard("Advisers assigned", "26", "Sections with an adviser", Ui.Kind.CHECK),
                Ui.statCard("Without adviser", "2", "Assign before classes start", Ui.Kind.WARN)), 18);
        block(Ui.cols(16, new double[]{2, 1}, Ui.stack(16, teachersCard(), sectionsCard()), formCard()), 16);
        finish();
    }

    private JComponent teachersCard() {
        Ui.Card c = Ui.card("Teachers", "Teacher profiles and their login accounts.", Ui.primary("+  Add teacher"));
        Ui.DataTable t = new Ui.DataTable(new String[]{"Teacher", "Employee no.", "Email", "Advisory", "Action"}, new Object[][]{
                {"Ryan D. Delos Santos", "T-0142", "r.delossantos@deihs.edu", "11 TechPro C", "Edit"},
                {"Maria L. Villanueva", "T-0087", "m.villanueva@deihs.edu", "11 TechPro A", "Edit"},
                {"Jose P. Navarro", "T-0213", "j.navarro@deihs.edu", "12 TechPro B", "Edit"},
                {"Ana C. Bautista", "T-0066", "a.bautista@deihs.edu", "—", "Edit"}}).links(4);
        c.body.add(Ui.stack(12, Ui.filters(Ui.search("Search name, employee no. or email", 22)), Ui.tableView(t)));
        return c;
    }

    private JComponent sectionsCard() {
        Ui.Card c = Ui.card("Sections", "Class sections and who advises them.", Ui.primary("+  Add section"));
        Ui.DataTable t = new Ui.DataTable(new String[]{"Section", "Grade", "School year", "Adviser", "Students", "Action"}, new Object[][]{
                {"TechPro C", "11", "2026–2027", "Ryan D. Delos Santos", "42", "Edit"},
                {"TechPro A", "11", "2026–2027", "Maria L. Villanueva", "44", "Edit"},
                {"TechPro B", "12", "2026–2027", "Jose P. Navarro", "40", "Edit"},
                {"Sci A", "10", "2026–2027", "—", "38", "Edit"}}).links(5);
        c.body.add(Ui.stack(12, Ui.filters(Ui.search("Search section", 22), Ui.combo("All grades", "Grade 10", "Grade 11", "Grade 12")), Ui.tableView(t)));
        return c;
    }

    private JComponent formCard() {
        Ui.Card c = Ui.card("Section details", "Create a section or change its adviser.", null);
        c.body.add(Ui.stack(12,
                Ui.labeled("Section name", Ui.field("e.g. TechPro C", 16)),
                Ui.labeled("Grade level", Ui.combo("Grade 10", "Grade 11", "Grade 12")),
                Ui.labeled("School year", Ui.field("2026–2027", 16)),
                Ui.labeled("Adviser", Ui.combo("Ryan D. Delos Santos", "Maria L. Villanueva", "Jose P. Navarro", "No adviser")),
                Ui.note("Removing an adviser keeps the section and its students.", 250),
                Ui.primary("Save section"), Ui.outline("Cancel")), BorderLayout.NORTH);
        return c;
    }
}
