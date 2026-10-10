package com.example.configtool.presentation.gui.theme;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.formdev.flatlaf.FlatIntelliJLaf;

public final class UiTheme {

    public static final Color CANVAS = new Color(0xF4F6F8);
    public static final Color SURFACE = Color.WHITE;
    public static final Color NAVIGATION = new Color(0x182230);
    public static final Color NAVIGATION_SELECTED = new Color(0x26384F);
    public static final Color ACCENT = new Color(0x0875B5);
    public static final Color ACCENT_DARK = new Color(0x075F91);
    public static final Color TEXT = new Color(0x1C2834);
    public static final Color MUTED_TEXT = new Color(0x5F6B76);
    public static final Color BORDER = new Color(0xD9E0E6);
    public static final Color SUCCESS = new Color(0x18794E);
    public static final Color WARNING = new Color(0x9A6700);
    public static final Color ERROR = new Color(0xB42318);

    private UiTheme() {
    }

    public static void install() {
        FlatIntelliJLaf.setup();
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", new Color(0xE8EDF1));
        UIManager.put("TableHeader.height", 34);
        UIManager.put("TableHeader.background", new Color(0xEDF2F6));
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    }

    public static Border panelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    public static void applyHeading(JComponent component) {
        component.setFont(new Font("Segoe UI", Font.BOLD, 20));
        component.setForeground(TEXT);
    }
}
