package com.example.configtool.domain.model;

import java.util.Objects;

/**
 * Resolved relationship between a pin function and its owning peripheral.
 *
 * @param peripheralId owning peripheral identifier
 * @param category owning peripheral category
 * @param signalName original function identifier
 */
public record SignalDescriptor(String peripheralId, PeripheralCategory category, String signalName) {

    /** Validates the resolved signal relationship. */
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
