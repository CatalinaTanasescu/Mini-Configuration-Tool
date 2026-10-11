package com.example.configtool.presentation.gui.theme;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.Border;

import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Central Swing look-and-feel and color-palette configuration.
 * Panels consume these shared semantic colors so light and dark modes remain visually consistent.
 */
public final class UiTheme {

    public static Color CANVAS;
    public static Color SURFACE;
    public static Color NAVIGATION;
    public static Color NAVIGATION_SELECTED;
    public static Color NAVIGATION_TEXT;
    public static Color ACCENT;
    public static Color ACCENT_DARK;
    public static Color TEXT;
    public static Color MUTED_TEXT;
    public static Color BORDER;
    public static Color SUCCESS;
    public static Color WARNING;
    public static Color ERROR;
    public static Color SUCCESS_BACKGROUND;
    public static Color WARNING_BACKGROUND;
    public static Color ERROR_BACKGROUND;

    private static boolean dark;

    static {
        applyPalette(false);
    }

    private UiTheme() {
    }

    /** Installs the default light theme. */
    public static void install() {
        install(false);
    }

    /**
     * @param darkMode {@code true} for FlatLaf Dark, {@code false} for FlatLaf IntelliJ
     */
    public static void install(boolean darkMode) {
        dark = darkMode;
        applyPalette(darkMode);
        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatIntelliJLaf.setup();
        }
        UIManager.put("Component.arc", 6);
        UIManager.put("Button.arc", 6);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.gridColor", darkMode ? new Color(0x2B3946) : new Color(0xE8EDF1));
        UIManager.put("TableHeader.height", 34);
        UIManager.put("TableHeader.background", darkMode ? new Color(0x25313D) : new Color(0xEDF2F6));
        UIManager.put("TableHeader.foreground", TEXT);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
    }

    /** @return whether the currently installed palette is dark */
    public static boolean isDark() {
        return dark;
    }

    /** @return standard border and padding used for surface panels */
    public static Border panelBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(16, 18, 16, 18));
    }

    /** @param component component to style as a primary heading */
    public static void applyHeading(JComponent component) {
        component.setFont(new Font("Segoe UI", Font.BOLD, 20));
        component.setForeground(TEXT);
    }

    private static void applyPalette(boolean darkMode) {
        if (darkMode) {
            CANVAS = new Color(0x10171F);
            SURFACE = new Color(0x18222D);
            NAVIGATION = new Color(0x0B1219);
            NAVIGATION_SELECTED = new Color(0x24435A);
            NAVIGATION_TEXT = new Color(0xD5E0E9);
            ACCENT = new Color(0x2997D6);
            ACCENT_DARK = new Color(0x1677AD);
            TEXT = new Color(0xE6EDF3);
            MUTED_TEXT = new Color(0xA8B4C0);
            BORDER = new Color(0x334250);
            SUCCESS = new Color(0x58C993);
            WARNING = new Color(0xF0C75E);
            ERROR = new Color(0xFF8279);
            SUCCESS_BACKGROUND = new Color(0x17382A);
            WARNING_BACKGROUND = new Color(0x3D3218);
            ERROR_BACKGROUND = new Color(0x44201F);
        } else {
            CANVAS = new Color(0xF4F6F8);
            SURFACE = Color.WHITE;
            NAVIGATION = new Color(0x182230);
            NAVIGATION_SELECTED = new Color(0x26384F);
            NAVIGATION_TEXT = new Color(0xCBD5DF);
            ACCENT = new Color(0x0875B5);
            ACCENT_DARK = new Color(0x075F91);
            TEXT = new Color(0x1C2834);
            MUTED_TEXT = new Color(0x5F6B76);
            BORDER = new Color(0xD9E0E6);
            SUCCESS = new Color(0x18794E);
            WARNING = new Color(0x9A6700);
            ERROR = new Color(0xB42318);
            SUCCESS_BACKGROUND = new Color(0xE9F7F0);
            WARNING_BACKGROUND = new Color(0xFFF6D8);
            ERROR_BACKGROUND = new Color(0xFEECEB);
        }
    }
}
