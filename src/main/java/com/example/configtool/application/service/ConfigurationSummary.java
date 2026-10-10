package com.example.configtool.application.service;

import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationResult;

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
