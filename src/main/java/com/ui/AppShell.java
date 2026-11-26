package com.ui;

import com.ui.AttendanceTheme.Palette;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

/** Role-based shell for navigation, themed screens and status indicators. */
public class AppShell extends JFrame {

    public enum Role {
        ADMIN("Admin workspace"), TEACHER("Teacher workspace"), STUDENT("Student workspace");
        final String title;
        Role(String title) { this.title = title; }
    }

    private record Nav(String label, Ui.Kind icon, Supplier<JComponent> page) {}

    private static AppShell instance;

    private final Role role;
    private final String userName, userSub;
    private final List<Nav> navs;
    private String selected;
    private JPanel content;

    public AppShell(Role role, String userName, String userSub) {
        this.role = role; this.userName = userName; this.userSub = userSub;
        this.navs = navsFor(role);
        this.selected = navs.get(0).label();
        instance = this;
        setTitle("DEIHS / Smart Attendance");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1360, 860);
        setLocationRelativeTo(null);
        build();
    }

    /** Selects a sidebar entry by label. */
    public static void go(String label) {
        if (instance != null) instance.select(label);
    }

    private static List<Nav> navsFor(Role role) {
        return switch (role) {
            case ADMIN -> List.of(
                    new Nav("Overview", Ui.Kind.GRID, AdminDashboard::new),
                    new Nav("Students", Ui.Kind.PEOPLE, StudentManagementPanel::new),
                    new Nav("Teachers & sections", Ui.Kind.BOOK, TeacherSectionPanel::new),
                    new Nav("Class schedules", Ui.Kind.CALENDAR, SchedulePanel::new),
                    new Nav("RFID cards", Ui.Kind.CARD, RfidAssignmentPanel::new),
                    new Nav("Alerts & thresholds", Ui.Kind.WARN, AlertsPanel::new),
                    new Nav("Reports", Ui.Kind.DOC, ReportsPanel::new));
            case TEACHER -> List.of(
                    new Nav("Overview", Ui.Kind.GRID, TeacherDashboard::new),
                    new Nav("Class attendance", Ui.Kind.PEOPLE, ClassAttendancePanel::new),
                    new Nav("Excused absences", Ui.Kind.CHECK, ExcusedAbsencePanel::new),
                    new Nav("Threshold watch", Ui.Kind.WARN, ThresholdPanel::new),
                    new Nav("Reports", Ui.Kind.DOC, ReportsPanel::new));
            case STUDENT -> List.of(
                    new Nav("Overview", Ui.Kind.GRID, StudentDashboard::new),
                    new Nav("My attendance", Ui.Kind.CALENDAR, MyAttendancePanel::new),
                    new Nav("Excused days", Ui.Kind.CHECK, MyExcusedDaysPanel::new));
        };
    }

    private void select(String label) {
        selected = label;
        build();
    }

    /** Rebuilds the shell after navigation or theme changes. */
    private void build() {
        Palette p = Ui.p();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(p.background());
        root.add(sidebar(), BorderLayout.WEST);

        content = new JPanel(new BorderLayout());
        content.setBackground(p.background());
        Nav nav = navs.stream().filter(n -> n.label().equals(selected)).findFirst().orElse(navs.get(0));
        JScrollPane sp = new JScrollPane(nav.page().get());
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        sp.getViewport().setBackground(p.background());
        content.add(sp, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);

        setContentPane(root);
        revalidate();
        repaint();
    }

    private JComponent sidebar() {
        Palette p = Ui.p();
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(p.surface());
        side.setBorder(new MatteBorder(0, 0, 0, 1, p.border()));
        side.setPreferredSize(new Dimension(236, 10));

        JPanel top = Ui.transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTH;
        int y = 0;
        c.gridy = y++; c.insets = new Insets(18, 16, 18, 16); top.add(brand(), c);
        c.gridy = y++; c.insets = new Insets(0, 16, 8, 16); top.add(Ui.caps(role.title), c);
        for (Nav n : navs) {
            c.gridy = y++; c.insets = new Insets(2, 10, 0, 10);
            top.add(navItem(n), c);
        }
        side.add(top, BorderLayout.NORTH);

        JPanel bottom = Ui.transparent(new GridBagLayout());
        c.gridy = 0; c.insets = new Insets(0, 16, 8, 16); bottom.add(Ui.caps("Preferences"), c);
        c.gridy = 1; c.insets = new Insets(0, 14, 14, 14);
        bottom.add(Ui.segmented(new String[]{"Light", "Dark"}, AttendanceTheme.isDark() ? 1 : 0, i -> {
            AttendanceTheme.install(i == 1);
            build();
        }), c);
        JLabel out = Ui.label("Sign out", Font.PLAIN, 12.5f, p.muted());
        out.setIcon(Ui.icon(Ui.Kind.ARROW, 14, p.muted()));
        out.setIconTextGap(10);
        out.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        out.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                dispose();
                new LoginFrame().setVisible(true);
            }
        });
        c.gridy = 2; c.insets = new Insets(0, 16, 14, 16); bottom.add(out, c);
        JPanel user = Ui.stack(2, Ui.bold(userName, 12.5f), Ui.small(userSub));
        user.setBorder(new EmptyBorder(14, 16, 16, 16));
        JPanel userWrap = new JPanel(new BorderLayout());
        userWrap.setOpaque(false);
        userWrap.setBorder(new MatteBorder(1, 0, 0, 0, p.border()));
        userWrap.add(user);
        c.gridy = 3; c.insets = new Insets(0, 14, 0, 14); bottom.add(userWrap, c);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private JComponent brand() {
        Palette p = Ui.p();
        JPanel b = Ui.transparent(new BorderLayout(10, 0));
        JComponent logo;
        java.net.URL url = AppShell.class.getResource("/logo.png");   // Optional classpath logo.
        if (url != null) {
            Image img = new ImageIcon(url).getImage().getScaledInstance(38, 38, Image.SCALE_SMOOTH);
            logo = new JLabel(new ImageIcon(img));
        } else {
            logo = new JLabel(new Icon() {
                public int getIconWidth() { return 38; }
                public int getIconHeight() { return 38; }
                public void paintIcon(Component cmp, Graphics g, int x, int yy) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(p.accent()); g2.fillOval(x, yy, 38, 38);
                    g2.setColor(p.onAccent()); g2.setFont(Ui.font(Font.BOLD, 16f));
                    g2.drawString("D", x + 19 - g2.getFontMetrics().stringWidth("D") / 2, yy + 25);
                    g2.dispose();
                }
            });
        }
        b.add(logo, BorderLayout.WEST);
        b.add(Ui.stack(0, Ui.bold("DEIHS", 17f), Ui.small("Smart Attendance")), BorderLayout.CENTER);
        return b;
    }

    private JComponent navItem(Nav n) {
        Palette p = Ui.p();
        boolean on = n.label().equals(selected);
        Color fg = on ? p.accent() : p.text();
        Ui.Round r = new Ui.Round(new BorderLayout(), on ? p.accentSoft() : null, null, 8);
        r.setBorder(new EmptyBorder(9, 12, 9, 12));
        JLabel l = Ui.label(n.label(), on ? Font.BOLD : Font.PLAIN, 13f, fg);
        l.setIcon(Ui.icon(n.icon(), 16, on ? p.accent() : p.muted()));
        l.setIconTextGap(11);
        r.add(l);
        r.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        r.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { select(n.label()); }
        });
        return r;
    }

    private JComponent statusBar() {
        Palette p = Ui.p();
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBackground(p.surface());
        bar.setBorder(new javax.swing.border.CompoundBorder(
                new MatteBorder(1, 0, 0, 0, p.border()), new EmptyBorder(5, 12, 5, 12)));

        // TODO(db): use JDBC.testConnection() for the SQL status label.
        JLabel sql = status("SQL connected", true);
        JLabel mqtt = status("MQTT listener active", true);
        JLabel rc = status("RC522 reader online", false);
        bar.add(Ui.flow(18, sql, mqtt, rc), BorderLayout.WEST);

        JLabel mid = Ui.small("Design preview · Sample records");
        mid.setHorizontalAlignment(SwingConstants.CENTER);
        bar.add(mid, BorderLayout.CENTER);

        String now = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
        bar.add(Ui.flow(18, Ui.small("Last synced  " + now), Ui.small("v1.0")), BorderLayout.EAST);
        return bar;
    }

    private JLabel status(String text, boolean dot) {
        JLabel l = Ui.label(text, Font.PLAIN, 11f, dot ? Ui.p().accent() : Ui.p().muted());
        if (dot) { l.setIcon(Ui.dot(Ui.p().accent())); l.setIconTextGap(6); }
        return l;
    }
}
