package com.example.configtool.application.service;

import java.util.Objects;

/**
 * Describes one field-level difference between the current configuration and a compared snapshot.
 * Missing values are normalized to a readable marker for both presentation adapters.
 *
 * @param field stable field path, for example {@code pin.PTA2}
 * @param currentValue value in the active session
 * @param comparedValue value in the imported comparison snapshot
 */
public record ConfigurationDifference(
        String field,
        String currentValue,
        String comparedValue) {

    /** Validates the field name and normalizes absent values. */
    public ConfigurationDifference {
        field = Objects.requireNonNull(field, "Diff field cannot be null");
        currentValue = currentValue == null ? "<absent>" : currentValue;
        comparedValue = comparedValue == null ? "<absent>" : comparedValue;
    }
}
