package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.example.configtool.presentation.gui.theme.UiTheme;

/** Package-local factory for consistent headings, metric cards, and panel styling. */
final class PanelSupport {

    private PanelSupport() {
    }

    static JPanel header(String title, String description) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        JLabel heading = new JLabel(title);
        UiTheme.applyHeading(heading);
        JLabel detail = new JLabel(description);
        detail.setForeground(UiTheme.MUTED_TEXT);
        panel.add(heading, BorderLayout.NORTH);
        panel.add(detail, BorderLayout.SOUTH);
        return panel;
    }
}
