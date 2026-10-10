package com.example.configtool.presentation.gui.table;

import java.awt.Component;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public final class FunctionCellEditor extends AbstractCellEditor implements TableCellEditor {

    private static final long serialVersionUID = 1L;
    private final PinTableModel model;
    private final JComboBox<String> selector = new JComboBox<>();

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
