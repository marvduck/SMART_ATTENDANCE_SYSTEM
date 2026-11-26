package com.ui;

import com.ui.AttendanceTheme.Palette;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

/** Shared themed Swing components; AppShell rebuilds screens after theme changes. */
public final class Ui {

    private Ui() {}

    public static Palette p() { return AttendanceTheme.current(); }

    public enum Tone { ACCENT, WARN, DANGER, NEUTRAL }

    public enum Kind { PEOPLE, CHECK, CLOCK, CALENDAR, DOC, WARN, CARD, GRID, BOOK, SEARCH, ARROW }

    public static Font font(int style, float size) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null) base = new JLabel().getFont();
        return base.deriveFont(style, size);
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JLabel bold(String text, float size) { return label(text, Font.BOLD, size, p().text()); }
    public static JLabel plain(String text) { return label(text, Font.PLAIN, 13f, p().text()); }
    public static JLabel muted(String text) { return label(text, Font.PLAIN, 12f, p().muted()); }
    public static JLabel small(String text) { return label(text, Font.PLAIN, 11f, p().muted()); }

    /** Small uppercase label. */
    public static JLabel caps(String text) {
        JLabel l = label(text.toUpperCase(Locale.ROOT), Font.BOLD, 10.5f, p().muted());
        return l;
    }

    /** Word-wraps to container width; widthPx is only a hint. */
    public static JComponent wrap(String text, int widthPx, Color color, float size) {
        return new WrapLabel(text, widthPx, color, font(Font.PLAIN, size));
    }

    private static final class WrapLabel extends JComponent {
        private final String text; private final int guess; private final Font font;
        WrapLabel(String text, int guess, Color color, Font font) {
            this.text = text; this.guess = guess; this.font = font;
            setForeground(color); setFont(font); setOpaque(false);
        }
        private java.util.List<String> lines(int w) {
            FontMetrics fm = getFontMetrics(font);
            java.util.List<String> out = new java.util.ArrayList<>();
            String line = "";
            for (String word : text.split(" ")) {
                String t = line.isEmpty() ? word : line + " " + word;
                if (fm.stringWidth(t) > w && !line.isEmpty()) { out.add(line); line = word; } else line = t;
            }
            if (!line.isEmpty()) out.add(line);
            return out;
        }
        @Override public Dimension getPreferredSize() {
            int w = getWidth() > 0 ? getWidth() : guess;
            return new Dimension(10, lines(w).size() * getFontMetrics(font).getHeight() + 2);
        }
        @Override public Dimension getMinimumSize() { return new Dimension(10, getPreferredSize().height); }
        @Override public void setBounds(int x, int y, int w, int h) {
            int old = getWidth();
            super.setBounds(x, y, w, h);
            if (w != old) revalidate();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(font); g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int y = fm.getAscent() + 1;
            for (String l : lines(getWidth())) { g2.drawString(l, 0, y); y += fm.getHeight(); }
            g2.dispose();
        }
    }

    public static String today() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH));
    }

    public static JPanel transparent(LayoutManager lm) {
        JPanel j = new JPanel(lm);
        j.setOpaque(false);
        return j;
    }

    /** Vertical full-width stack. */
    public static JPanel stack(int gap, JComponent... items) {
        JPanel pnl = transparent(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST;
        for (int i = 0; i < items.length; i++) {
            c.gridy = i;
            c.insets = new Insets(i == 0 ? 0 : gap, 0, 0, 0);
            pnl.add(items[i], c);
        }
        // Keep items pinned to the top.
        c.gridy = items.length; c.weighty = 1; c.fill = GridBagConstraints.BOTH; c.insets = new Insets(0, 0, 0, 0);
        pnl.add(transparent(null), c);
        return pnl;
    }

    /** Left-aligned, natural-size flow. */
    public static JPanel flow(int gap, JComponent... items) {
        JPanel pnl = transparent(new FlowLayout(FlowLayout.LEFT, gap, 0));
        for (JComponent i : items) pnl.add(i);
        return pnl;
    }

    /** Weighted, equal-height columns. */
    public static JPanel cols(int gap, double[] weights, JComponent... items) {
        JPanel pnl = transparent(new Cols(gap, weights));
        for (JComponent i : items) pnl.add(i);
        return pnl;
    }

    /** Equal-width columns. */
    public static JPanel equal(int gap, JComponent... items) {
        double[] w = new double[items.length];
        java.util.Arrays.fill(w, 1);
        return cols(gap, w, items);
    }

    private static final class Cols implements LayoutManager {
        private final int gap; private final double[] w;
        Cols(int gap, double[] w) { this.gap = gap; this.w = w; }
        public void addLayoutComponent(String n, Component c) {}
        public void removeLayoutComponent(Component c) {}
        public Dimension preferredLayoutSize(Container t) {
            int width = 0, h = 0; Component[] cs = t.getComponents();
            for (Component c : cs) { Dimension d = c.getPreferredSize(); width += d.width; h = Math.max(h, d.height); }
            Insets in = t.getInsets();
            return new Dimension(width + gap * Math.max(0, cs.length - 1) + in.left + in.right, h + in.top + in.bottom);
        }
        public Dimension minimumLayoutSize(Container t) { return new Dimension(0, preferredLayoutSize(t).height); }
        public void layoutContainer(Container t) {
            Component[] cs = t.getComponents(); if (cs.length == 0) return;
            Insets in = t.getInsets();
            double sum = 0; for (int i = 0; i < cs.length; i++) sum += w[i];
            double avail = t.getWidth() - in.left - in.right - gap * (cs.length - 1);
            double x = in.left;
            int h = t.getHeight() - in.top - in.bottom;
            for (int i = 0; i < cs.length; i++) {
                double cw = avail * w[i] / sum;
                cs[i].setBounds((int) Math.round(x), in.top, (int) Math.round(cw), h);
                x += cw + gap;
            }
        }
    }

    public static class Round extends JPanel {
        private final Color fill, line; private final int arc;
        public Round(LayoutManager lm, Color fill, Color line, int arc) {
            super(lm); this.fill = fill; this.line = line; this.arc = arc; setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (fill != null) { g2.setColor(fill); g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc); }
            if (line != null) { g2.setColor(line); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc); }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Rounded card; add content to {@code body}. */
    public static class Card extends Round {
        public final JPanel body = transparent(new BorderLayout());
        Card(String title, String subtitle, JComponent trailing) {
            super(new BorderLayout(0, 14), p().surface(), p().border(), 12);
            setBorder(new EmptyBorder(16, 18, 16, 18));
            if (title != null) {
                JPanel head = transparent(new BorderLayout(12, 0));
                JLabel t = bold(title, 15f);
                boolean inline = trailing != null && !(trailing instanceof AbstractButton);
                if (inline) {
                    // Inline badges preserve full subtitle width.
                    JPanel titleRow = transparent(new BorderLayout(12, 0));
                    titleRow.add(t, BorderLayout.CENTER);
                    titleRow.add(trailing, BorderLayout.EAST);
                    head.add(subtitle == null ? stack(0, titleRow) : stack(2, titleRow, muted(subtitle)), BorderLayout.CENTER);
                } else {
                    head.add(subtitle == null ? stack(0, t) : stack(2, t, muted(subtitle)), BorderLayout.CENTER);
                    if (trailing != null) {
                        JPanel right = transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                        right.add(trailing);
                        head.add(right, BorderLayout.EAST);
                    }
                }
                add(head, BorderLayout.NORTH);
            }
            add(body, BorderLayout.CENTER);
        }
    }

    public static Card card(String title, String subtitle, JComponent trailing) { return new Card(title, subtitle, trailing); }

    /** Vertically scrolling, full-width page. */
    public static class Page extends JPanel implements Scrollable {
        private final GridBagConstraints gc = new GridBagConstraints();
        private int row;
        public Page() {
            super(new GridBagLayout());
            setBackground(p().background());
            setBorder(new EmptyBorder(22, 28, 22, 28));
        }
        /** Adds a full-width page block. */
        protected void block(JComponent c, int gapTop) {
            gc.gridx = 0; gc.gridy = row++; gc.weightx = 1; gc.weighty = 0;
            gc.fill = GridBagConstraints.HORIZONTAL; gc.anchor = GridBagConstraints.NORTH;
            gc.insets = new Insets(gapTop, 0, 0, 0);
            add(c, gc);
        }
        protected void finish() {
            gc.gridx = 0; gc.gridy = row++; gc.weightx = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
            gc.insets = new Insets(0, 0, 0, 0);
            add(transparent(null), gc);
        }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 24; }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(40, r.height - 40); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() { return false; }
    }

    public static JComponent sep() {
        JPanel s = new JPanel();
        s.setBackground(p().border());
        s.setPreferredSize(new Dimension(1, 1));
        s.setMinimumSize(new Dimension(1, 1));
        return s;
    }

    public static JComponent gap(int h) {
        JPanel s = transparent(null);
        s.setPreferredSize(new Dimension(1, h));
        s.setMinimumSize(new Dimension(1, h));
        return s;
    }

    public static JComponent header(String crumb, String title, String subtitle) {
        JPanel h = transparent(new BorderLayout(16, 0));
        h.add(stack(4, caps(crumb), label(title, Font.BOLD, 26f, p().text()), muted(subtitle)), BorderLayout.CENTER);
        Round date = new Round(new GridBagLayout(), p().surface(), p().border(), 8);
        date.setBorder(new EmptyBorder(11, 36, 11, 36));
        date.add(bold(today(), 12.5f));
        JPanel right = transparent(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.add(date);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    public static Color[] toneColors(Tone t) {
        Palette p = p();
        return switch (t) {
            case ACCENT -> new Color[]{p.accentSoft(), p.accent()};
            case WARN -> new Color[]{p.warningSoft(), p.warning()};
            case DANGER -> new Color[]{p.dangerSoft(), p.danger()};
            case NEUTRAL -> new Color[]{p.subtle(), p.muted()};
        };
    }

    private static final Map<String, Tone> TONES = new HashMap<>();
    static {
        for (String s : new String[]{"present", "time in", "time out", "active", "approved", "linked", "excused",
                "live", "registered", "on campus", "online", "ok"}) TONES.put(s, Tone.ACCENT);
        for (String s : new String[]{"absent", "late", "out of bounds", "pending", "review", "watch", "unread",
                "near limit", "replaced", "absence limit"}) TONES.put(s, Tone.WARN);
        for (String s : new String[]{"invalid card", "invalid", "school limit", "over limit", "lost", "at limit",
                "unregistered", "disabled"}) TONES.put(s, Tone.DANGER);
    }

    public static Tone toneFor(String text) { return TONES.getOrDefault(text.toLowerCase(Locale.ROOT), Tone.NEUTRAL); }

    public static JComponent pill(String text) { return pill(text, toneFor(text)); }

    public static JComponent pill(String text, Tone tone) {
        Color[] c = toneColors(tone);
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c[0]);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setFont(font(Font.BOLD, 11f));
        l.setForeground(c[1]);
        l.setBorder(new EmptyBorder(4, 12, 4, 12));
        return l;
    }

    private static Line2D ln(double s, double x1, double y1, double x2, double y2) {
        return new Line2D.Double(x1 * s, y1 * s, x2 * s, y2 * s);
    }

    public static Icon icon(Kind kind, int size, Color color) {
        return new Icon() {
            public int getIconWidth() { return size; }
            public int getIconHeight() { return size; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(Math.max(1.3f, size / 11f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double s = size;
                switch (kind) {
                    case CHECK -> {
                        Path2D.Double q = new Path2D.Double();
                        q.moveTo(.2 * s, .55 * s); q.lineTo(.42 * s, .76 * s); q.lineTo(.82 * s, .28 * s);
                        g2.draw(q);
                    }
                    case CLOCK -> {
                        g2.draw(new Ellipse2D.Double(.1 * s, .1 * s, .8 * s, .8 * s));
                        g2.draw(ln(s, .5, .28, .5, .5)); g2.draw(ln(s, .5, .5, .66, .6));
                    }
                    case CALENDAR -> {
                        g2.draw(new RoundRectangle2D.Double(.12 * s, .2 * s, .76 * s, .68 * s, .12 * s, .12 * s));
                        g2.draw(ln(s, .12, .42, .88, .42)); g2.draw(ln(s, .32, .08, .32, .3)); g2.draw(ln(s, .68, .08, .68, .3));
                    }
                    case PEOPLE -> {
                        g2.draw(new Ellipse2D.Double(.3 * s, .1 * s, .4 * s, .4 * s));
                        g2.draw(new Arc2D.Double(.14 * s, .58 * s, .72 * s, .7 * s, 0, 180, Arc2D.OPEN));
                    }
                    case DOC -> {
                        g2.draw(new RoundRectangle2D.Double(.22 * s, .1 * s, .56 * s, .8 * s, .1 * s, .1 * s));
                        g2.draw(ln(s, .34, .38, .66, .38)); g2.draw(ln(s, .34, .54, .66, .54)); g2.draw(ln(s, .34, .7, .56, .7));
                    }
                    case WARN -> {
                        Path2D.Double q = new Path2D.Double();
                        q.moveTo(.5 * s, .1 * s); q.lineTo(.92 * s, .86 * s); q.lineTo(.08 * s, .86 * s); q.closePath();
                        g2.draw(q); g2.draw(ln(s, .5, .4, .5, .6)); g2.draw(ln(s, .5, .73, .5, .74));
                    }
                    case CARD -> {
                        g2.draw(new RoundRectangle2D.Double(.08 * s, .2 * s, .84 * s, .6 * s, .12 * s, .12 * s));
                        g2.draw(ln(s, .08, .42, .92, .42)); g2.draw(ln(s, .22, .64, .42, .64));
                    }
                    case GRID -> {
                        for (double gx : new double[]{.12, .56}) for (double gy : new double[]{.12, .56})
                            g2.draw(new RoundRectangle2D.Double(gx * s, gy * s, .32 * s, .32 * s, .08 * s, .08 * s));
                    }
                    case BOOK -> {
                        g2.draw(new RoundRectangle2D.Double(.14 * s, .14 * s, .72 * s, .72 * s, .1 * s, .1 * s));
                        g2.draw(ln(s, .5, .14, .5, .86));
                    }
                    case SEARCH -> {
                        g2.draw(new Ellipse2D.Double(.12 * s, .12 * s, .5 * s, .5 * s)); g2.draw(ln(s, .56, .56, .88, .88));
                    }
                    case ARROW -> {
                        g2.draw(ln(s, .15, .5, .85, .5));
                        Path2D.Double q = new Path2D.Double();
                        q.moveTo(.58 * s, .25 * s); q.lineTo(.85 * s, .5 * s); q.lineTo(.58 * s, .75 * s);
                        g2.draw(q);
                    }
                }
                g2.dispose();
            }
        };
    }

    public static Icon dot(Color color) {
        return new Icon() {
            public int getIconWidth() { return 8; }
            public int getIconHeight() { return 8; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color); g2.fillOval(x, y, 8, 8); g2.dispose();
            }
        };
    }

    public static JLabel legend(Color color, String text) {
        JLabel l = small(text);
        l.setIcon(dot(color));
        l.setIconTextGap(6);
        return l;
    }

    public static JComponent statCard(String label, String value, String sub, Kind kind) {
        Card c = card(null, null, null);
        JPanel top = transparent(new BorderLayout());
        top.add(muted(label), BorderLayout.WEST);
        top.add(new JLabel(icon(kind, 16, p().muted())), BorderLayout.EAST);
        c.body.add(stack(4, top, label(value, Font.BOLD, 30f, p().text()), small(sub)), BorderLayout.CENTER);
        return c;
    }

    public static <T extends JComponent> T fixH(T c, int h) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(d.width, h));
        return c;
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(p().accent()); b.setForeground(p().onAccent());
        b.setFont(font(Font.BOLD, 12.5f)); b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return fixH(b, 38);
    }

    public static JButton outline(String text) {
        JButton b = new JButton(text);
        b.setBackground(p().surface()); b.setForeground(p().text());
        b.setFont(font(Font.BOLD, 12.5f)); b.setFocusPainted(false);
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return fixH(b, 38);
    }

    public static JLabel link(String text, String navTarget) {
        JLabel l = label(text, Font.BOLD, 12f, p().accent());
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (navTarget != null) l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { AppShell.go(navTarget); }
        });
        return l;
    }

    public static JComboBox<String> combo(String... items) {
        return fixH(new JComboBox<>(items), 34);
    }

    public static JTextField field(String placeholder, int cols) {
        JTextField f = new JTextField(cols);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return fixH(f, 34);
    }

    public static JTextField search(String placeholder, int cols) {
        JTextField f = field(placeholder, cols);
        f.putClientProperty("JTextField.leadingIcon", icon(Kind.SEARCH, 14, p().muted()));
        return f;
    }

    public static JPasswordField password(String placeholder) {
        JPasswordField f = new JPasswordField(18);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return fixH(f, 34);
    }

    public static JComponent labeled(String label, JComponent input) { return stack(5, caps(label), input); }

    public static JPanel filters(JComponent... items) {
        JPanel f = transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        for (JComponent i : items) f.add(i);
        return f;
    }

    public static JComponent note(String text, int widthPx) {
        Round r = new Round(new BorderLayout(), p().subtle(), p().border(), 8);
        r.setBorder(new EmptyBorder(11, 14, 11, 14));
        r.add(wrap(text, widthPx, p().muted(), 12f), BorderLayout.CENTER);
        return r;
    }

    /** Muted content box. */
    public static JComponent box(JComponent inner) {
        Round r = new Round(new BorderLayout(), p().subtle(), p().border(), 8);
        r.setBorder(new EmptyBorder(12, 14, 12, 14));
        r.add(inner);
        return r;
    }

    /** Parses day/state pairs such as "2P 3A" for {@link #monthCalendar}. */
    public static Map<Integer, Character> states(String spec) {
        Map<Integer, Character> m = new HashMap<>();
        for (String t : spec.trim().split("\\s+"))
            m.put(Integer.parseInt(t.substring(0, t.length() - 1)), t.charAt(t.length() - 1));
        return m;
    }

    public static JComponent kv(String key, String value) {
        JPanel r = transparent(new BorderLayout(12, 0));
        r.add(muted(key), BorderLayout.WEST);
        JLabel v = bold(value, 12.5f);
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        r.add(v, BorderLayout.CENTER);
        return r;
    }

    public static JComponent field(String label, String value) { return stack(2, caps(label), bold(value, 13f)); }

    public static JComponent footer(String left, JComponent right) {
        JPanel f = transparent(new BorderLayout());
        f.setBorder(new EmptyBorder(10, 0, 0, 0));
        f.add(small(left), BorderLayout.WEST);
        if (right != null) f.add(right, BorderLayout.EAST);
        return f;
    }

    public static JComponent pager() { return label("‹    1    2    3    …    9    ›", Font.PLAIN, 11f, p().muted()); }

    public static JComponent avatar(String initials, int size) {
        JLabel l = new JLabel(initials, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int d = Math.min(getWidth(), getHeight());
                g2.setColor(p().accentSoft());
                g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(font(Font.BOLD, size * 0.36f));
        l.setForeground(p().accent());
        l.setPreferredSize(new Dimension(size, size));
        return l;
    }

    public static JComponent progress(double fraction, Color color) {
        JComponent c = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(p().border());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.setColor(color);
                g2.fillRoundRect(0, 0, (int) Math.round(getWidth() * fraction), getHeight(), getHeight(), getHeight());
                g2.dispose();
            }
        };
        c.setPreferredSize(new Dimension(60, 4));
        return c;
    }

    /** Segmented selector. */
    public static JComponent segmented(String[] options, int selected, IntConsumer onChange) {
        Round box = new Round(new GridLayout(1, options.length, 2, 0), p().subtle(), null, 10);
        box.setBorder(new EmptyBorder(3, 3, 3, 3));
        for (int i = 0; i < options.length; i++) {
            final int idx = i; final boolean on = i == selected;
            JLabel l = new JLabel(options[i], SwingConstants.CENTER) {
                @Override protected void paintComponent(Graphics g) {
                    if (on) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(p().surface()); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.setColor(p().border()); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                        g2.dispose();
                    }
                    super.paintComponent(g);
                }
            };
            l.setFont(font(on ? Font.BOLD : Font.PLAIN, 12f));
            l.setForeground(on ? p().text() : p().muted());
            l.setBorder(new EmptyBorder(7, 12, 7, 12));
            l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            l.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { if (!on) onChange.accept(idx); }
            });
            box.add(l);
        }
        return box;
    }

    public static class DataTable extends JTable {
        private final Set<Integer> pills = new HashSet<>(), links = new HashSet<>();

        public DataTable(String[] cols, Object[][] rows) {
            super(new DefaultTableModel(rows, cols) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            });
            Palette p = p();
            setRowHeight(44);
            setShowVerticalLines(false);
            setShowHorizontalLines(true);
            setGridColor(p.border());
            setIntercellSpacing(new Dimension(0, 1));
            setBackground(p.surface());
            setForeground(p.text());
            setSelectionBackground(p.accentSoft());
            setSelectionForeground(p.text());
            setFocusable(false);
            setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
            JTableHeader h = getTableHeader();
            h.setReorderingAllowed(false);
            h.setResizingAllowed(false);
            h.setPreferredSize(new Dimension(10, 34));
            h.setDefaultRenderer(headerRenderer());
            setDefaultRenderer(Object.class, cellRenderer());
        }

        /** Status-pill columns. */
        public DataTable pills(int... c) { for (int i : c) pills.add(i); return this; }
        /** Accent-link columns. */
        public DataTable links(int... c) { for (int i : c) links.add(i); return this; }
        /** Preferred column widths. */
        public DataTable widths(int... w) {
            for (int i = 0; i < w.length; i++) getColumnModel().getColumn(i).setPreferredWidth(w[i]);
            return this;
        }
        public DataTable select(int row) { setRowSelectionInterval(row, row); return this; }

        private TableCellRenderer headerRenderer() {
            return (t, v, sel, foc, r, c) -> {
                JLabel l = caps(String.valueOf(v));
                l.setOpaque(true);
                l.setBackground(p().subtle());
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                return l;
            };
        }

        private TableCellRenderer cellRenderer() {
            return (t, v, sel, foc, r, c) -> {
                Palette p = p();
                String s = v == null ? "" : v.toString();
                Color bg = sel ? p.accentSoft() : p.surface();
                if (pills.contains(c) && !s.isEmpty() && !s.equals("—")) return new PillCell(s, bg, toneColors(toneFor(s)));
                JLabel l = new JLabel(s);
                l.setOpaque(true);
                l.setBackground(bg);
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                boolean dash = s.equals("—");
                if (links.contains(c)) { l.setFont(font(Font.BOLD, 12f)); l.setForeground(p.accent()); }
                else { l.setFont(font(Font.PLAIN, 12.5f)); l.setForeground(dash ? p.muted() : p.text()); }
                return l;
            };
        }
    }

    /** Paints its own pill independently of table layout/validation. */
    private static final class PillCell extends JLabel {
        private final Color cellBg; private final Color[] tone;
        PillCell(String text, Color cellBg, Color[] tone) {
            super(text); this.cellBg = cellBg; this.tone = tone;
            setFont(font(Font.BOLD, 11f)); setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(cellBg); g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int pw = fm.stringWidth(getText()) + 24, ph = 22, x = 12, y = (getHeight() - ph) / 2;
            g2.setColor(tone[0]); g2.fillRoundRect(x, y, pw, ph, 8, 8);
            g2.setColor(tone[1]);
            g2.drawString(getText(), x + 12, y + (ph - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    /** Header and rows without inner scrolling, so cards grow with content. */
    public static JComponent tableView(DataTable t) {
        JPanel w = transparent(new BorderLayout());
        w.add(t.getTableHeader(), BorderLayout.NORTH);
        w.add(t, BorderLayout.CENTER);
        return w;
    }

    /** Vertical bars; null values render as upcoming. */
    public static class BarChart extends JComponent {
        private final String[] labels; private final Double[] values;
        public BarChart(String[] labels, Double[] values, int height) {
            this.labels = labels; this.values = values;
            setPreferredSize(new Dimension(300, height));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Palette p = p();
            int left = 40, top = 18, bottom = 22;
            int w = getWidth() - left, h = getHeight() - top - bottom;
            g2.setFont(font(Font.PLAIN, 10.5f));
            FontMetrics fm = g2.getFontMetrics();
            for (int v : new int[]{25, 50, 75, 100}) {
                int y = top + (int) Math.round(h * (1 - v / 100.0));
                g2.setColor(p.border()); g2.drawLine(left, y, getWidth(), y);
                g2.setColor(p.muted()); g2.drawString(v + "%", 0, y + fm.getAscent() / 2 - 1);
            }
            int n = labels.length; double slot = (double) w / n;
            double bw = Math.min(56, slot * 0.5);
            for (int i = 0; i < n; i++) {
                double cx = left + slot * i + slot / 2;
                int x = (int) Math.round(cx - bw / 2);
                if (values[i] != null) {
                    int bh = (int) Math.round(h * values[i] / 100.0);
                    Shape old = g2.getClip();
                    g2.clipRect(0, top, getWidth(), h);
                    g2.setColor(p.accent());
                    g2.fillRoundRect(x, top + h - bh, (int) Math.round(bw), bh + 8, 6, 6);
                    g2.setClip(old);
                    String t = String.format("%.1f%%", values[i]);
                    g2.setFont(font(Font.BOLD, 10.5f)); g2.setColor(p.text());
                    g2.drawString(t, (int) (cx - g2.getFontMetrics().stringWidth(t) / 2.0), top + h - bh - 5);
                } else {
                    g2.setColor(p.subtle());
                    g2.fillRoundRect(x, top + h - 14, (int) Math.round(bw), 22, 6, 6);
                }
                g2.setFont(font(Font.PLAIN, 10.5f)); g2.setColor(p.muted());
                g2.drawString(labels[i], (int) (cx - g2.getFontMetrics().stringWidth(labels[i]) / 2.0), getHeight() - 5);
            }
            g2.dispose();
        }
    }

    /** Monday-first grid; states: P present, A absent, E excused. */
    public static JComponent monthCalendar(YearMonth ym, Map<Integer, Character> states, int today) {
        JPanel g = transparent(new GridLayout(0, 7, 6, 6));
        for (String d : new String[]{"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"}) {
            JLabel l = caps(d); l.setHorizontalAlignment(SwingConstants.CENTER); g.add(l);
        }
        int lead = ym.atDay(1).getDayOfWeek().getValue() - 1;
        YearMonth prev = ym.minusMonths(1);
        int total = (int) Math.ceil((lead + ym.lengthOfMonth()) / 7.0) * 7;
        for (int i = 0; i < total; i++) {
            int day = i - lead + 1;
            if (day < 1) g.add(dayCell(String.valueOf(prev.lengthOfMonth() + day), 'x', false));
            else if (day > ym.lengthOfMonth()) g.add(dayCell(String.valueOf(day - ym.lengthOfMonth()), 'x', false));
            else g.add(dayCell(String.valueOf(day), states.getOrDefault(day, ' '), day == today));
        }
        return g;
    }

    private static JComponent dayCell(String text, char state, boolean today) {
        Palette p = p();
        Color bg = null, fg = p.text(), line = null;
        switch (state) {
            case 'P' -> { bg = p.accentSoft(); fg = p.accent(); }
            case 'A' -> { bg = p.warningSoft(); fg = p.warning(); }
            case 'E' -> { bg = p.subtle(); fg = p.accent(); line = p.accent(); }
            case 'x' -> fg = new Color(p.muted().getRed(), p.muted().getGreen(), p.muted().getBlue(), 120);
            default -> { }
        }
        if (today) { bg = p.accent(); fg = p.onAccent(); line = null; }
        final Color fbg = bg, fline = line;
        JLabel l = new JLabel(text, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (fbg != null) { g2.setColor(fbg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6); }
                if (fline != null) { g2.setColor(fline); g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6); }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(font(today ? Font.BOLD : Font.PLAIN, 12f));
        l.setForeground(fg);
        l.setPreferredSize(new Dimension(40, 30));
        return l;
    }
}
