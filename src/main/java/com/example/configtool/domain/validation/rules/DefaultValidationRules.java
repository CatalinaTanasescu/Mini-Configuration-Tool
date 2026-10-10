package com.example.configtool.domain.validation.rules;

import java.util.List;

import com.example.configtool.domain.validation.ValidationRule;

public final class DefaultValidationRules {

    private DefaultValidationRules() {
    }

    public static List<ValidationRule> create() {
        return List.of(
                new PinMultiplexingRule(),
                new PeripheralClockRule(),
                new PowerModeCompatibilityRule(),
                new SupportedFunctionRule(),
                new DuplicateExclusiveSignalRule(),
                new DisabledPeripheralAssignmentRule(),
                new UnassignedEnabledPeripheralRule());
    }
}
