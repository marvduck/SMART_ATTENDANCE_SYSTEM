package com.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

public final class AttendanceTheme {

    private AttendanceTheme () {}

    public record Palette (
            Color background, Color surface, Color subtle, Color border,
            Color text, Color muted, Color accent, Color onAccent,
            Color accentSoft, Color warning, Color warningSoft, Color danger,
            Color dangerSoft
    ) {}

    public static final Palette Light = new Palette(
            new Color(0xf5f7f6), new Color(0xffffff), new Color(0xf0f4f2), new Color(0xdfe7e2),
            new Color(0x1c3028), new Color(0x60756B), new Color(0x237A52), new Color(0xffffff),
            new Color(0xe6f3eb), new Color(0x985c18), new Color(0xfbf1df), new Color(0xaf3636),
            new Color(0xfbe8e8)
    );

    public static final Palette Dark = new Palette(
            new Color(0x0d1721), new Color(0x15232f), new Color(0x1c2d3b), new Color(0x263744),
            new Color(0xdfe8ed), new Color(0x8da4af), new Color(0x5dcee8), new Color(0x0d1721),
            new Color(0x173a48), new Color(0xe2af68), new Color(0x3a3022), new Color(0xf29b9b),
            new Color(0x432a30)
    );

    private static Palette current = Light;

    public static Palette current(){
        return current;
    }

    public static boolean isDark(){
        return current == Dark;
    }

    public static Color c(String hex){
        return Color.decode("#" + hex );
    }

    public static void install(boolean dark) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Use Swing EDT");
        }

        if (dark) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
        current = dark ? Dark : Light;
        Palette p = current;

        // Exact FlatLaf keys matter; misspellings and duplicates silently do nothing.
        UIManager.put("Panel.background", p.background());
        UIManager.put("Label.foreground", p.text());
        UIManager.put("Label.disabledForeground", p.muted());
        UIManager.put("Separator.foreground", p.border());
        UIManager.put("Component.borderColor", p.border());
        UIManager.put("Component.focusColor", p.accent());
        UIManager.put("Component.focusedBorderColor", p.accent());
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 4);
        UIManager.put("Button.background", p.surface());
        UIManager.put("Button.foreground", p.text());
        UIManager.put("Button.default.background", p.accent());
        UIManager.put("Button.default.foreground", p.onAccent());
        UIManager.put("TextField.background", p.surface());
        UIManager.put("TextField.foreground", p.text());
        UIManager.put("PasswordField.background", p.surface());
        UIManager.put("PasswordField.foreground", p.text());
        UIManager.put("TextComponent.selectionBackground", p.accentSoft());
        UIManager.put("TextComponent.selectionForeground", p.text());
        UIManager.put("ComboBox.background", p.surface());
        UIManager.put("ComboBox.foreground", p.text());
        UIManager.put("Table.background", p.surface());
        UIManager.put("Table.foreground", p.text());
        UIManager.put("Table.gridColor", p.border());
        UIManager.put("Table.alternateRowColor", p.subtle());
        UIManager.put("Table.selectionBackground", p.accentSoft());
        UIManager.put("Table.selectionForeground", p.accent());
        UIManager.put("Table.rowHeight", 44);
        UIManager.put("TableHeader.background", p.subtle());
        UIManager.put("TableHeader.foreground", p.muted());
        UIManager.put("ScrollPane.background", p.background());
        FlatLaf.updateUI();
    }
}
