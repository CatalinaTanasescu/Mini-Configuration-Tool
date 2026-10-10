package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationSeverity;
import com.example.configtool.presentation.gui.table.ValidationTableModel;
import com.example.configtool.presentation.gui.theme.UiTheme;

public final class ValidationPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final ConfigurationService service;
    private final ValidationTableModel model = new ValidationTableModel();
    private final JTable table = new JTable(model);
    private final JLabel resultLabel = new JLabel();

    public ValidationPanel(ConfigurationService service, Consumer<ValidationIssue> navigationHandler) {
        super(new BorderLayout(0, 14));
        this.service = service;
        setBackground(UiTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(PanelSupport.header(
                "Validation",
                "Errors block export. Warnings are advisory and remain visible in exported-ready configurations."),
                BorderLayout.CENTER);
        resultLabel.setFont(resultLabel.getFont().deriveFont(java.awt.Font.BOLD));
        header.add(resultLabel, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        table.setAutoCreateRowSorter(true);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(75);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(360);
        table.getColumnModel().getColumn(4).setPreferredWidth(360);
        table.getColumnModel().getColumn(0).setCellRenderer(new SeverityRenderer());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                    navigationHandler.accept(model.issueAt(modelRow));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
        JLabel hint = new JLabel("Tip: double-click an issue to navigate to the affected pin or peripheral.");
        hint.setForeground(UiTheme.MUTED_TEXT);
        add(hint, BorderLayout.SOUTH);
    }

    public void refresh() {
        var result = service.validate();
        model.setIssues(result.issues());
        if (result.errorCount() > 0) {
            resultLabel.setText(result.errorCount() + " error(s) and " + result.warningCount() + " warning(s)");
            resultLabel.setForeground(UiTheme.ERROR);
        } else if (result.warningCount() > 0) {
            resultLabel.setText("No errors; " + result.warningCount() + " warning(s)");
            resultLabel.setForeground(UiTheme.WARNING);
        } else {
            resultLabel.setText("No validation issues. Configuration is ready for export.");
            resultLabel.setForeground(UiTheme.SUCCESS);
        }
    }

    private static final class SeverityRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            Component component = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                component.setForeground(ValidationSeverity.ERROR.name().equals(value)
                        ? UiTheme.ERROR : UiTheme.WARNING);
            }
            setFont(getFont().deriveFont(java.awt.Font.BOLD));
            return component;
        }
    }
}
