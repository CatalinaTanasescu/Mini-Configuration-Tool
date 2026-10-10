package com.example.configtool.domain.validation.rules;

import java.util.ArrayList;
import java.util.List;

import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

public final class SupportedFunctionRule implements ValidationRule {

    public static final String CODE = "SUPPORTED_FUNCTION";

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        List<ValidationIssue> issues = new ArrayList<>();
        context.configuration().pinAssignments().forEach(assignment -> context.processor()
                .findPin(assignment.pinName())
                .ifPresentOrElse(
                        pin -> {
                            if (!pin.supportsFunction(assignment.function())) {
                                issues.add(new ValidationIssue(
                                        CODE,
                                        ValidationSeverity.ERROR,
                                        assignment.function() + " is not available on " + assignment.pinName() + ".",
                                        "Choose one of: " + String.join(", ", pin.availableFunctions()),
                                        assignment.pinName(),
                                        null));
                            }
                        },
                        () -> issues.add(new ValidationIssue(
                                CODE,
                                ValidationSeverity.ERROR,
                                "Assigned pin " + assignment.pinName() + " does not exist on the selected processor.",
                                "Remove the stale assignment or select the correct processor.",
                                assignment.pinName(),
                                null))));
        return List.copyOf(issues);
    }
}
