package com.example.configtool.domain.validation;

import java.util.Comparator;
import java.util.List;

/**
 * Immutable aggregate returned by {@link ValidationEngine}.
 *
 * @param issues ordered validation diagnostics
 */
public record ValidationResult(List<ValidationIssue> issues) {

    /** Defensively copies the reported issues. */
    public ValidationResult {
        issues = issues.stream()
                .sorted(Comparator.comparing(ValidationIssue::severity)
                        .thenComparing(ValidationIssue::code)
                        .thenComparing(ValidationIssue::location))
                .toList();
    }

    /** @return {@code true} when no error-severity issue exists */
    public boolean isValid() {
        return errorCount() == 0;
    }

    /** @return number of error-severity issues */
    public long errorCount() {
        return issues.stream().filter(issue -> issue.severity() == ValidationSeverity.ERROR).count();
    }

    /** @return number of warning-severity issues */
    public long warningCount() {
        return issues.stream().filter(issue -> issue.severity() == ValidationSeverity.WARNING).count();
    }
}
