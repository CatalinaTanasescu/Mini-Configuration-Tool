package com.example.configtool.domain.validation.rules;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.example.configtool.domain.model.PeripheralCategory;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class PowerModeCompatibilityRule implements ValidationRule {

    public static final String CODE = "POWER_MODE";
    private static final Set<PeripheralCategory> BLOCKED_IN_LOW_POWER =
            EnumSet.of(PeripheralCategory.CAN, PeripheralCategory.SPI, PeripheralCategory.ETHERNET);

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        if (context.configuration().powerMode() != PowerMode.LOW_POWER) {
            return List.of();
        }
        return context.processor().peripherals().stream()
                .filter(peripheral -> BLOCKED_IN_LOW_POWER.contains(peripheral.category()))
                .filter(peripheral -> context.configuration().peripheralConfigurations()
                        .get(peripheral.id()).enabled())
                .map(peripheral -> new ValidationIssue(
                        CODE,
                        ValidationSeverity.ERROR,
                        peripheral.id() + " (" + peripheral.category() + ") is not supported in LOW_POWER.",
                        "Switch to RUN mode or disable the peripheral.",
                        null,
                        peripheral.id()))
                .toList();
    }
}
