package com.example.configtool.domain.validation;

import java.util.List;

@FunctionalInterface
/**
 * Extension point for one focused business validation policy.
 * Rules are stateless and presentation-independent, which allows the GUI and CLI to share them.
 */
public interface ValidationRule {

    /**
     * @param context immutable processor and configuration state
     * @return issues detected by this rule; never {@code null}
     */
    List<ValidationIssue> validate(ValidationContext context);
}
