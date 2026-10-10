package com.example.configtool.domain.validation.rules;

import java.util.List;

import com.example.configtool.domain.model.SignalResolver;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class DisabledPeripheralAssignmentRule implements ValidationRule {

    public static final String CODE = "DISABLED_PERIPHERAL_ASSIGNMENT";

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        return context.configuration().pinAssignments().stream()
                .flatMap(assignment -> SignalResolver.resolve(assignment.function()).stream()
                        .filter(signal -> {
                            var state = context.configuration().peripheralConfigurations()
                                    .get(signal.peripheralId());
                            return state == null || !state.enabled();
                        })
                        .map(signal -> new ValidationIssue(
                                CODE,
                                ValidationSeverity.ERROR,
                                assignment.function() + " is assigned to " + assignment.pinName()
                                        + " while " + signal.peripheralId() + " is disabled.",
                                "Enable " + signal.peripheralId() + " or clear the pin assignment.",
                                assignment.pinName(),
                                signal.peripheralId())))
                .toList();
    }
}
