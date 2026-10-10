package com.example.configtool.domain.validation;

import java.util.List;
import java.util.Objects;

public final class ValidationEngine {

    private final List<ValidationRule> rules;

    public ValidationEngine(List<ValidationRule> rules) {
        Objects.requireNonNull(rules, "Validation rules cannot be null");
        this.rules = List.copyOf(rules);
    }

    public ValidationResult validate(ValidationContext context) {
        List<ValidationIssue> issues = rules.stream()
                .flatMap(rule -> rule.validate(context).stream())
                .toList();
        return new ValidationResult(issues);
    }

    public List<ValidationRule> rules() {
        return rules;
    }
}
