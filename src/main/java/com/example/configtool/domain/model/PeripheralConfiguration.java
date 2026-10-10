package com.example.configtool.domain.model;

import java.util.Objects;

public record PeripheralConfiguration(String peripheralId, boolean enabled, boolean clockEnabled) {

    public PeripheralConfiguration {
        Objects.requireNonNull(peripheralId, "Peripheral id cannot be null");
        if (peripheralId.isBlank()) {
            throw new IllegalArgumentException("Peripheral id cannot be blank");
        }
    }

    public PeripheralConfiguration withEnabled(boolean value) {
        return new PeripheralConfiguration(peripheralId, value, clockEnabled);
    }

    public PeripheralConfiguration withClockEnabled(boolean value) {
        return new PeripheralConfiguration(peripheralId, enabled, value);
    }
}
