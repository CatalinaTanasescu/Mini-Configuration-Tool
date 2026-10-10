package com.example.configtool.domain.validation.rules;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.configtool.domain.model.SignalResolver;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class UnassignedEnabledPeripheralRule implements ValidationRule {

    public static final String CODE = "PERIPHERAL_WITHOUT_RESOURCES";

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        Set<String> assignedPeripherals = context.configuration().pinAssignments().stream()
                .flatMap(assignment -> SignalResolver.resolve(assignment.function()).stream())
                .map(signal -> signal.peripheralId())
                .collect(Collectors.toSet());
        return context.configuration().peripheralConfigurations().values().stream()
                .filter(state -> state.enabled() && !assignedPeripherals.contains(state.peripheralId()))
                .map(state -> new ValidationIssue(
                        CODE,
                        ValidationSeverity.WARNING,
                        state.peripheralId() + " is enabled but has no assigned pin resources.",
                        "Assign at least one signal or disable the peripheral if it is not needed.",
                        null,
                        state.peripheralId()))
                .toList();
    }
}
