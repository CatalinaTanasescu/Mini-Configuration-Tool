package com.example.configtool.presentation.gui.table;

import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.model.PeripheralDefinition;
import com.example.configtool.domain.model.SignalResolver;

public final class PeripheralTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final String[] COLUMNS = {
            "Enabled", "Clock", "Peripheral", "Category", "Assigned resources", "Signals"
    };

    private final ConfigurationService service;
    private final Runnable changeListener;
    private final Consumer<RuntimeException> errorHandler;

    public PeripheralTableModel(
            ConfigurationService service,
            Runnable changeListener,
            Consumer<RuntimeException> errorHandler) {
        this.service = service;
        this.changeListener = changeListener;
        this.errorHandler = errorHandler;
    }

    @Override
    public int getRowCount() {
        return service.currentProcessor().peripherals().size();
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
        return columnIndex < 2 ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex < 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        PeripheralDefinition peripheral = peripheralAt(rowIndex);
        var state = service.currentConfiguration().peripheralConfigurations().get(peripheral.id());
        return switch (columnIndex) {
            case 0 -> state.enabled();
            case 1 -> state.clockEnabled();
            case 2 -> peripheral.id();
            case 3 -> peripheral.category().name();
            case 4 -> assignedPins(peripheral.id());
            case 5 -> String.join(", ", peripheral.signals());
            default -> throw new IndexOutOfBoundsException("Unknown column " + columnIndex);
        };
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        PeripheralDefinition peripheral = peripheralAt(rowIndex);
        try {
            if (columnIndex == 0) {
                service.setPeripheralEnabled(peripheral.id(), Boolean.TRUE.equals(value));
            } else if (columnIndex == 1) {
                service.setClockEnabled(peripheral.id(), Boolean.TRUE.equals(value));
            }
            changeListener.run();
        } catch (RuntimeException exception) {
            errorHandler.accept(exception);
        }
    }

    public PeripheralDefinition peripheralAt(int modelRow) {
        return service.currentProcessor().peripherals().get(modelRow);
    }

    public void refresh() {
        fireTableDataChanged();
    }

    private String assignedPins(String peripheralId) {
        String pins = service.currentConfiguration().pinAssignments().stream()
                .filter(assignment -> SignalResolver.resolve(assignment.function())
                        .map(signal -> signal.peripheralId().equals(peripheralId)).orElse(false))
                .map(assignment -> assignment.pinName())
                .sorted()
                .collect(Collectors.joining(", "));
        return pins.isBlank() ? "-" : pins;
    }
}
