package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.presentation.gui.table.PeripheralTableModel;
import com.example.configtool.presentation.gui.theme.UiTheme;

/**
 * Editable peripheral and clock table.
 * Mutations flow through its table model to the shared service; callbacks let the owning window
 * refresh related panels and present errors without coupling this panel to navigation logic.
 */
public final class PeripheralConfigurationPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final PeripheralTableModel model;
    private final JTable table;

    /**
     * @param service shared application facade
     * @param changeListener invoked after a successful edit
     * @param errorHandler presentation-level error callback
     * @param autoAssignListener callback that applies the window's explicit auto-assignment policy
     */
    public PeripheralConfigurationPanel(
            ConfigurationService service,
            Runnable changeListener,
            java.util.function.Consumer<RuntimeException> errorHandler,
            Runnable autoAssignListener) {
        super(new BorderLayout(0, 14));
        setBackground(UiTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        JPanel top = new JPanel(new BorderLayout(16, 0));
        top.setOpaque(false);
        top.add(PanelSupport.header(
                "Peripherals and clocks",
                "Enable peripherals and their associated clocks, then inspect assigned pin resources."),
                BorderLayout.CENTER);
        JButton autoAssign = new JButton("Auto-assign selected");
        autoAssign.addActionListener(event -> autoAssignListener.run());
        top.add(autoAssign, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        model = new PeripheralTableModel(service, changeListener, errorHandler);
        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(65);
        table.getColumnModel().getColumn(1).setPreferredWidth(65);
        table.getColumnModel().getColumn(2).setPreferredWidth(115);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(160);
        table.getColumnModel().getColumn(5).setPreferredWidth(360);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
    }

    /** Reloads table rows from the selected processor and current configuration. */
    public void refresh() {
        model.refresh();
    }

    /** @param peripheralId peripheral to select and scroll into view */
    public void focusPeripheral(String peripheralId) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (model.peripheralAt(row).id().equals(peripheralId)) {
                int viewRow = table.convertRowIndexToView(row);
                table.setRowSelectionInterval(viewRow, viewRow);
                table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                break;
            }
        }
    }

    /** @return selected peripheral identifier, or {@code null} when no row is selected */
    public String selectedPeripheralId() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0
                ? null
                : model.peripheralAt(table.convertRowIndexToModel(viewRow)).id();
    }
}
