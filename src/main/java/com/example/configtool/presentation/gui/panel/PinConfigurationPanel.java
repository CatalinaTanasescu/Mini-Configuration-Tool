package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.presentation.gui.table.FunctionCellEditor;
import com.example.configtool.presentation.gui.table.PinTableModel;
import com.example.configtool.presentation.gui.theme.UiTheme;

public final class PinConfigurationPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final PinTableModel model;
    private final JTable table;
    private final JTextField searchField = new JTextField();
    private final TableRowSorter<PinTableModel> sorter;

    public PinConfigurationPanel(
            ConfigurationService service,
            Runnable changeListener,
            java.util.function.Consumer<RuntimeException> errorHandler) {
        super(new BorderLayout(0, 14));
        setBackground(UiTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));

        JPanel top = new JPanel(new BorderLayout(20, 0));
        top.setOpaque(false);
        top.add(PanelSupport.header(
                "Pin configuration",
                "Select one supported function per physical pin. Changes are validated by the shared engine."),
                BorderLayout.CENTER);
        JPanel search = new JPanel(new BorderLayout(8, 0));
        search.setOpaque(false);
        search.add(new JLabel("Filter"), BorderLayout.WEST);
        searchField.putClientProperty("JTextField.placeholderText", "Pin, function or peripheral");
        searchField.setPreferredSize(new Dimension(250, 34));
        search.add(searchField, BorderLayout.CENTER);
        top.add(search, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        model = new PinTableModel(service, changeListener, errorHandler);
        table = new JTable(model);
        table.setAutoCreateRowSorter(false);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(75);
        table.getColumnModel().getColumn(2).setPreferredWidth(330);
        table.getColumnModel().getColumn(3).setPreferredWidth(155);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setCellEditor(new FunctionCellEditor(model));
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent event) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent event) { applyFilter(); }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
    }

    public void refresh() {
        model.refresh();
    }

    public void focusSearch() {
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    public void focusPin(String pinName) {
        searchField.setText(pinName);
        applyFilter();
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            table.scrollRectToVisible(table.getCellRect(0, 0, true));
        }
    }

    private void applyFilter() {
        String text = searchField.getText().trim();
        sorter.setRowFilter(text.isEmpty()
                ? null
                : RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
    }
}
