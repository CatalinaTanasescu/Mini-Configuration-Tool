package com.example.configtool.domain.validation;

import java.util.List;
import java.util.Objects;

/**
 * Executes an injected collection of independent validation rules.
 * The engine contains no rule-specific branching, so adding a policy means composing another
 * {@link ValidationRule}, not modifying this orchestration class.
 */
public final class ValidationEngine {

    private final List<ValidationRule> rules;

    /** @param rules ordered rules to execute for every validation request */
    public ValidationEngine(List<ValidationRule> rules) {
        Objects.requireNonNull(rules, "Validation rules cannot be null");
        this.rules = List.copyOf(rules);
    }

    /**
     * @param context processor definition and configuration snapshot
     * @return deterministic aggregate of all issues reported by all rules
     */
    public ValidationResult validate(ValidationContext context) {
        List<ValidationIssue> issues = rules.stream()
                .flatMap(rule -> rule.validate(context).stream())
                .toList();
        return new ValidationResult(issues);
    }

    /** @return immutable configured rule list */
    public List<ValidationRule> rules() {
        return rules;
    }
}
