package com.example.configtool.domain.validation.rules;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class PinMultiplexingRule implements ValidationRule {

    public static final String CODE = "PIN_MULTIPLEXING";

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        Map<String, List<PinAssignment>> byPin = context.configuration().pinAssignments().stream()
                .collect(Collectors.groupingBy(PinAssignment::pinName));
        return byPin.entrySet().stream()
                .filter(entry -> entry.getValue().stream().map(PinAssignment::function).distinct().count() > 1)
                .map(entry -> new ValidationIssue(
                        CODE,
                        ValidationSeverity.ERROR,
                        "Pin " + entry.getKey() + " has multiple active functions: "
                                + entry.getValue().stream().map(PinAssignment::function).distinct().sorted().toList(),
                        "Keep exactly one active function on the physical pin.",
                        entry.getKey(),
                        null))
                .toList();
    }
}
