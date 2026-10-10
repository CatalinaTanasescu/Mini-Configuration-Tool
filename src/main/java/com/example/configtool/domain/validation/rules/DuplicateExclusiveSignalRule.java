package com.example.configtool.domain.validation.rules;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class DuplicateExclusiveSignalRule implements ValidationRule {

    public static final String CODE = "DUPLICATE_EXCLUSIVE_SIGNAL";

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        Map<String, List<PinAssignment>> byFunction = context.configuration().pinAssignments().stream()
                .filter(assignment -> context.processor().isSignalExclusive(assignment.function()))
                .collect(Collectors.groupingBy(PinAssignment::function));
        return byFunction.entrySet().stream()
                .filter(entry -> entry.getValue().stream().map(PinAssignment::pinName).distinct().count() > 1)
                .map(entry -> new ValidationIssue(
                        CODE,
                        ValidationSeverity.ERROR,
                        "Exclusive signal " + entry.getKey() + " is assigned to multiple pins: "
                                + entry.getValue().stream().map(PinAssignment::pinName).distinct().sorted().toList(),
                        "Keep the signal on a single physical pin.",
                        null,
                        context.processor().resolveSignal(entry.getKey())
                                .map(signal -> signal.peripheralId()).orElse(null)))
                .toList();
    }
}
