package com.example.configtool.domain.validation.rules;

import java.util.List;

import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class PeripheralClockRule implements ValidationRule {

    public static final String CODE = "PERIPHERAL_CLOCK";

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        return context.configuration().peripheralConfigurations().values().stream()
                .filter(state -> state.enabled() && !state.clockEnabled())
                .map(state -> new ValidationIssue(
                        CODE,
                        ValidationSeverity.ERROR,
                        "Peripheral " + state.peripheralId() + " is enabled while its clock is disabled.",
                        "Enable the associated clock or disable the peripheral.",
                        null,
                        state.peripheralId()))
                .toList();
    }
}
