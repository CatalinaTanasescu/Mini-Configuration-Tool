package com.example.configtool.domain.validation;

import java.util.Comparator;
import java.util.List;

public record ValidationResult(List<ValidationIssue> issues) {

    public ValidationResult {
        issues = issues.stream()
                .sorted(Comparator.comparing(ValidationIssue::severity)
                        .thenComparing(ValidationIssue::code)
                        .thenComparing(ValidationIssue::location))
                .toList();
    }

    public boolean isValid() {
        return errorCount() == 0;
    }

    public long errorCount() {
        return issues.stream().filter(issue -> issue.severity() == ValidationSeverity.ERROR).count();
    }

    public long warningCount() {
        return issues.stream().filter(issue -> issue.severity() == ValidationSeverity.WARNING).count();
    }
}
