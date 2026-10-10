package com.example.configtool.domain.validation;

import java.util.Objects;

public record ValidationIssue(
        String code,
        ValidationSeverity severity,
        String message,
        String suggestion,
        String pinName,
        String peripheralId) {

    public ValidationIssue {
        code = requireText(code, "Validation code");
        severity = Objects.requireNonNull(severity, "Validation severity cannot be null");
        message = requireText(message, "Validation message");
        suggestion = suggestion == null ? "" : suggestion;
    }

    public String location() {
        if (pinName != null && peripheralId != null) {
            return pinName + " / " + peripheralId;
        }
        if (pinName != null) {
            return pinName;
        }
        return peripheralId == null ? "Configuration" : peripheralId;
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
