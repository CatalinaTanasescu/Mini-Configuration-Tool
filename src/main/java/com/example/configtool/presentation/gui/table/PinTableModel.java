package com.example.configtool.presentation.gui.table;

import java.util.function.Consumer;

import javax.swing.table.AbstractTableModel;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.model.PinDefinition;

public final class PinTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "Physical", "Pin", "Available functions", "Selected function", "Peripheral", "Status"
    };

    private final ConfigurationService service;
    private final Runnable changeListener;
    private final Consumer<RuntimeException> errorHandler;

    public PinTableModel(
            ConfigurationService service,
            Runnable changeListener,
            Consumer<RuntimeException> errorHandler) {
        this.service = service;
        this.changeListener = changeListener;
        this.errorHandler = errorHandler;
    }

    @Override
    public int getRowCount() {
        return service.currentProcessor().pins().size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnIndex == 0 ? Integer.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 3;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PinDefinition pin = pinAt(rowIndex);
        String assignment = service.currentConfiguration().assignmentFor(pin.name())
                .map(value -> value.function()).orElse("");
        return switch (columnIndex) {
            case 0 -> pin.physicalNumber();
            case 1 -> pin.name();
            case 2 -> String.join(", ", pin.availableFunctions());
            case 3 -> assignment;
            case 4 -> service.currentProcessor().resolveSignal(assignment)
                    .map(value -> value.peripheralId()).orElse("-");
            case 5 -> assignment.isBlank() ? "Unassigned" : "Assigned";
            default -> throw new IndexOutOfBoundsException("Unknown column " + columnIndex);
        };
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex != 3) {
            return;
        }
        PinDefinition pin = pinAt(rowIndex);
        try {
            String function = value == null ? "" : value.toString();
            if (function.isBlank()) {
                service.clearPin(pin.name());
            } else {
                service.assignPin(pin.name(), function);
            }
            changeListener.run();
        } catch (RuntimeException exception) {
            errorHandler.accept(exception);
        }
    }

    public PinDefinition pinAt(int modelRow) {
        return service.currentProcessor().pins().get(modelRow);
    }

    public void refresh() {
        fireTableDataChanged();
    }
}
