package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Comparator;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.presentation.gui.theme.UiTheme;

public final class SummaryPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final ConfigurationService service;
    private final JTextArea summary = new JTextArea();

    public SummaryPanel(ConfigurationService service) {
        super(new BorderLayout(0, 14));
        this.service = service;
        setBackground(UiTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        add(PanelSupport.header(
                "Configuration summary",
                "Review the complete selected state before generating the versioned JSON output."),
                BorderLayout.NORTH);

        summary.setEditable(false);
        summary.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        summary.setForeground(UiTheme.TEXT);
        summary.setBackground(UiTheme.SURFACE);
        summary.setMargin(new java.awt.Insets(18, 20, 18, 20));
        JScrollPane scrollPane = new JScrollPane(summary);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refresh() {
        var processor = service.currentProcessor();
        var configuration = service.currentConfiguration();
        var metrics = service.summary();
        StringBuilder text = new StringBuilder();
        text.append("CONFIGURATION\n");
        text.append("=============\n\n");
        text.append(String.format("Processor          %s%n", processor.id()));
        text.append(String.format("Physical pins      %d%n", processor.pinCount()));
        text.append(String.format("Power mode         %s%n", configuration.powerMode()));
        text.append(String.format("Assigned pins      %d%n", configuration.pinAssignments().size()));
        text.append(String.format("Enabled peripherals %d%n", metrics.enabledPeripherals()));
        text.append(String.format("Enabled clocks     %d%n", metrics.enabledClocks()));
        text.append(String.format("Validation         %d error(s), %d warning(s)%n",
                metrics.validation().errorCount(), metrics.validation().warningCount()));
        text.append(String.format("Export readiness   %s%n",
                metrics.validation().isValid() ? "READY" : "BLOCKED"));

        text.append("\nPERIPHERALS AND CLOCKS\n");
        text.append("----------------------\n");
        configuration.peripheralConfigurations().values().stream()
                .filter(state -> state.enabled() || state.clockEnabled())
                .sorted(Comparator.comparing(state -> state.peripheralId()))
                .forEach(state -> text.append(String.format("%-14s enabled=%-5s clock=%-5s%n",
                        state.peripheralId(), state.enabled(), state.clockEnabled())));
        if (configuration.peripheralConfigurations().values().stream()
                .noneMatch(state -> state.enabled() || state.clockEnabled())) {
            text.append("No peripheral or clock is enabled.\n");
        }

        text.append("\nPIN ASSIGNMENTS\n");
        text.append("---------------\n");
        configuration.pinAssignments().stream()
                .sorted(Comparator.comparingInt(assignment -> processor
                        .findPin(assignment.pinName()).orElseThrow().physicalNumber()))
                .forEach(assignment -> text.append(String.format("%3d  %-7s %-18s %s%n",
                        processor.findPin(assignment.pinName()).orElseThrow().physicalNumber(),
                        assignment.pinName(),
                        assignment.function(),
                        processor.resolveSignal(assignment.function())
                                .map(signal -> signal.peripheralId()).orElse("GPIO"))));
        if (configuration.pinAssignments().isEmpty()) {
            text.append("No pins are assigned.\n");
        }

        if (!metrics.validation().issues().isEmpty()) {
            text.append("\nVALIDATION ISSUES\n");
            text.append("-----------------\n");
            text.append(metrics.validation().issues().stream()
                    .map(issue -> "[" + issue.severity() + "] " + issue.code() + " - " + issue.message())
                    .collect(Collectors.joining(System.lineSeparator())));
            text.append(System.lineSeparator());
        }
        summary.setText(text.toString());
        summary.setCaretPosition(0);
    }
}
