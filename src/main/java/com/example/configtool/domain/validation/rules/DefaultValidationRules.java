package com.example.configtool.domain.validation.rules;

import java.util.List;

import com.example.configtool.domain.validation.ValidationRule;

/** Factory defining the built-in rule composition and its deterministic execution order. */
public final class DefaultValidationRules {

    private DefaultValidationRules() {
    }

    /** @return a new immutable list containing all built-in validation policies */
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
