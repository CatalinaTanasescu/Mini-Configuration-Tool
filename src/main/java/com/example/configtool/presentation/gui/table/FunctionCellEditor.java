package com.example.configtool.presentation.gui.table;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

/**
 * Pin-table editor whose combo box is populated from the capabilities of the row being edited.
 * Restricting choices in the UI improves usability, while service and validation checks remain the
 * authoritative safety boundary.
 */
public final class FunctionCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = 1L;
    private final PinTableModel model;
    private final JComboBox<String> selector = new JComboBox<>();

    /** @param model model used to resolve the pin and its supported functions */
    public FunctionCellEditor(PinTableModel model) {
        this.model = model;
    }

    @Override
    public Object getCellEditorValue() {
        return selector.getSelectedItem();
    }

    @Override
    public Component getTableCellEditorComponent(
            JTable table,
            Object value,
            boolean isSelected,
            int row,
            int column) {
        selector.removeAllItems();
        selector.addItem("");
        int modelRow = table.convertRowIndexToModel(row);
        model.pinAt(modelRow).availableFunctions().forEach(selector::addItem);
        selector.setSelectedItem(value == null ? "" : value.toString());
        return selector;
    }
}
