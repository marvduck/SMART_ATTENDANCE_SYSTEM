package com.ui;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    /** Preview-only login; real authentication and database wiring are pending. */

    private int roleIndex = 0;
    private final JTextField username = Ui.field("Username", 18);
    private final JPasswordField password = Ui.password("Password");

    public LoginFrame() {
        setTitle("DEIHS / Smart Attendance");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 660);
        setMinimumSize(new Dimension(760, 600));
        setLocationRelativeTo(null);
        build();
    }

    private void build() {
        AttendanceTheme.Palette p = Ui.p();
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(p.background());

        Ui.Round card = new Ui.Round(new BorderLayout(), p.surface(), p.border(), 16);
        card.setBorder(new javax.swing.border.EmptyBorder(32, 36, 28, 36));

        JLabel title = Ui.label("Welcome back", Font.BOLD, 22f, p.text());
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel sub = Ui.muted("Sign in to your Smart Attendance workspace.");
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel brand = Ui.label("DEIHS", Font.BOLD, 13f, p.muted());
        brand.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel logo = Ui.transparent(new FlowLayout(FlowLayout.CENTER, 0, 0));
        logo.add(Ui.avatar("D", 56));

        JButton signIn = Ui.primary("Sign in");
        signIn.addActionListener(e -> signIn());

        JLabel status = Ui.label("SQL connected", Font.PLAIN, 11f, p.accent());
        status.setIcon(Ui.dot(p.accent()));
        status.setIconTextGap(6);
        status.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(Ui.stack(12, logo, brand, title, sub, Ui.gap(4),
                Ui.labeled("Username", username),
                Ui.labeled("Password", password),
                Ui.labeled("Preview as", Ui.segmented(new String[]{"Administrator", "Teacher", "Student"}, roleIndex, i -> {
                    roleIndex = i;
                    build();
                })),
                Ui.gap(2), signIn, Ui.gap(2), Ui.sep(), status));
        card.setPreferredSize(new Dimension(420, card.getPreferredSize().height));
        root.add(card);

        setContentPane(root);
        getRootPane().setDefaultButton(signIn);
        revalidate();
        repaint();
    }

    private void signIn() {
        // TODO(db): authenticate an active user with BCrypt, load the role profile and update last login.
        // Preview only: "Preview as" substitutes for the authenticated role.
        AppShell.Role[] roles = {AppShell.Role.ADMIN, AppShell.Role.TEACHER, AppShell.Role.STUDENT};
        String[][] who = {
                {"School administrator", "Administrator"},
                {"Ryan D. Delos Santos", "Teacher · Grade 11"},
                {"Alex M. Reyes", "Student · 11 TechPro C"}};
        dispose();
        new AppShell(roles[roleIndex], who[roleIndex][0], who[roleIndex][1]).setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AttendanceTheme.install(false);
            // TODO(db): check JDBC.testConnection(), show failures and close the pool on exit.
            new LoginFrame().setVisible(true);
        });
    }
}
