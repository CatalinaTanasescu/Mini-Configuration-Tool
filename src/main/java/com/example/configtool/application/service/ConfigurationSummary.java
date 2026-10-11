package com.example.configtool.application.service;

import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationResult;

/**
 * Read-only projection used by GUI and CLI summary views.
 *
 * @param processorId selected processor identifier
 * @param pinCount physical pins exposed by the processor
 * @param assignedPins number of configured pin assignments
 * @param enabledPeripherals number of enabled peripherals
 * @param enabledClocks number of enabled peripheral clocks
 * @param powerMode selected power mode
 * @param validation current validation result
 * @param dirty whether the session contains unsaved changes
 */
public record ConfigurationSummary(
        String processorId,
        int pinCount,
        int assignedPins,
        long enabledPeripherals,
        long enabledClocks,
        PowerMode powerMode,
        ValidationResult validation,
        boolean dirty) {
}
