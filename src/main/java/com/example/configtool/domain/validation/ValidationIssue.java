package com.example.configtool.domain.validation;

import java.util.Objects;

/**
 * Structured, presentation-independent validation diagnostic.
 *
 * @param code stable diagnostic identifier
 * @param severity error or warning classification
 * @param message explanation of the detected condition
 * @param suggestion actionable corrective guidance
 * @param pinName related pin, when applicable
 * @param peripheralId related peripheral, when applicable
 */
public record ValidationIssue(
        String code,
        ValidationSeverity severity,
        String message,
        String suggestion,
        String pinName,
        String peripheralId) {

    /** Validates required diagnostic fields and normalizes the optional suggestion. */
    public ValidationIssue {
        code = requireText(code, "Validation code");
        severity = Objects.requireNonNull(severity, "Validation severity cannot be null");
        message = requireText(message, "Validation message");
        suggestion = suggestion == null ? "" : suggestion;
    }

    /** @return compact human-readable location assembled from optional context fields */
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
