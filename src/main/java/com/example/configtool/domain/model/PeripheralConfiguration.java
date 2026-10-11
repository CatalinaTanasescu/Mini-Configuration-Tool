package com.example.configtool.domain.model;

import java.util.Objects;

/**
 * Immutable selected state for one peripheral.
 *
 * @param peripheralId identifier matching a {@link PeripheralDefinition}
 * @param enabled whether the peripheral is enabled
 * @param clockEnabled whether its associated clock is enabled
 */
public record PeripheralConfiguration(String peripheralId, boolean enabled, boolean clockEnabled) {

    /** Validates and normalizes the immutable peripheral state. */
    public PeripheralConfiguration {
        Objects.requireNonNull(peripheralId, "Peripheral id cannot be null");
        if (peripheralId.isBlank()) {
            throw new IllegalArgumentException("Peripheral id cannot be blank");
        }
    }

    /** @return a copy with the requested peripheral enable state */
    public PeripheralConfiguration withEnabled(boolean value) {
        return new PeripheralConfiguration(peripheralId, value, clockEnabled);
    }

    /** @return a copy with the requested clock state */
    public PeripheralConfiguration withClockEnabled(boolean value) {
        return new PeripheralConfiguration(peripheralId, enabled, value);
    }
}
