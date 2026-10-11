package com.example.configtool.domain.model;

import java.util.Objects;

/**
 * Associates one logical pin with one selected function.
 *
 * @param pinName logical pin name
 * @param function selected function identifier
 */
public record PinAssignment(String pinName, String function) {

    /** Validates the immutable pin/function pair. */
    public PinAssignment {
        pinName = requireText(pinName, "Pin name");
        function = requireText(function, "Assigned function");
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }
}
