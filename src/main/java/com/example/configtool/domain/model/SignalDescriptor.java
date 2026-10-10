package com.example.configtool.domain.model;

import java.util.Objects;

public record SignalDescriptor(String peripheralId, PeripheralCategory category, String signalName) {

    public SignalDescriptor {
        peripheralId = requireText(peripheralId, "Peripheral id");
        category = Objects.requireNonNull(category, "Peripheral category cannot be null");
        signalName = requireText(signalName, "Signal name");
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
