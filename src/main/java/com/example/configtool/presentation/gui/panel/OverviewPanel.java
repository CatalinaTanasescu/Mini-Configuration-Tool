package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.presentation.gui.theme.UiTheme;

public final class OverviewPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final ConfigurationService service;
    private final JLabel processorValue = metricValue();
    private final JLabel pinsValue = metricValue();
    private final JLabel assignedValue = metricValue();
    private final JLabel validationValue = metricValue();

    public OverviewPanel(ConfigurationService service) {
        super(new BorderLayout(0, 18));
        this.service = service;
        setBackground(UiTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        add(PanelSupport.header(
                "Configuration overview",
                "A live engineering summary of the selected processor and configuration state."),
                BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setOpaque(false);
        metrics.add(metric("Processor", processorValue));
        metrics.add(metric("Physical pins", pinsValue));
        metrics.add(metric("Assigned pins", assignedValue));
        metrics.add(metric("Validation", validationValue));
        body.add(metrics);
        body.add(Box.createVerticalStrut(18));
        body.add(workflowPanel());
        add(body, BorderLayout.CENTER);
    }

    public void refresh() {
        var summary = service.summary();
        processorValue.setText(summary.processorId());
        pinsValue.setText(Integer.toString(summary.pinCount()));
        assignedValue.setText(Integer.toString(summary.assignedPins()));
        if (summary.validation().errorCount() > 0) {
            validationValue.setText(summary.validation().errorCount() + " errors");
            validationValue.setForeground(UiTheme.ERROR);
        } else if (summary.validation().warningCount() > 0) {
            validationValue.setText(summary.validation().warningCount() + " warnings");
            validationValue.setForeground(UiTheme.WARNING);
        } else {
            validationValue.setText("Ready");
            validationValue.setForeground(UiTheme.SUCCESS);
        }
    }

    private static JPanel metric(String label, JLabel value) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(UiTheme.panelBorder());
        JLabel title = new JLabel(label.toUpperCase());
        title.setForeground(UiTheme.MUTED_TEXT);
        title.setFont(title.getFont().deriveFont(11f));
        panel.add(title, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel metricValue() {
        JLabel label = new JLabel("-");
        label.setForeground(UiTheme.TEXT);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 22f));
        return label;
    }

    private static JPanel workflowPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(UiTheme.SURFACE);
        panel.setBorder(UiTheme.panelBorder());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Recommended workflow");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 15f));
        title.setForeground(UiTheme.TEXT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(12));
        addStep(panel, "1", "Choose a processor and power mode from the toolbar.");
        addStep(panel, "2", "Enable peripherals and their clocks in Peripherals & Clocks.");
        addStep(panel, "3", "Assign supported functions in the Pin Configuration table.");
        addStep(panel, "4", "Resolve validation issues, inspect the summary, then export JSON or XML.");
        return panel;
    }

    private static void addStep(JPanel panel, String number, String text) {
        JLabel label = new JLabel("  " + number + "    " + text);
        label.setForeground(UiTheme.TEXT);
        label.setBorder(BorderFactory.createEmptyBorder(7, 0, 7, 0));
        panel.add(label);
    }
}
