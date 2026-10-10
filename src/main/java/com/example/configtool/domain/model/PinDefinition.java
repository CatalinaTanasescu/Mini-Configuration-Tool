package com.example.configtool.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record PinDefinition(String name, int physicalNumber, List<String> availableFunctions) {

    public PinDefinition {
        name = requireIdentifier(name, "Pin name");
        if (physicalNumber <= 0) {
            throw new IllegalArgumentException("Physical pin number must be positive");
        }
        Objects.requireNonNull(availableFunctions, "Available functions cannot be null");
        if (availableFunctions.isEmpty()) {
            throw new IllegalArgumentException("A pin must expose at least one function");
        }
        availableFunctions = List.copyOf(availableFunctions);
        if (availableFunctions.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("Pin functions cannot be null or blank");
        }
        if (new HashSet<>(availableFunctions).size() != availableFunctions.size()) {
            throw new IllegalArgumentException("Duplicate pin functions are not allowed on " + name);
        }
    }

    public boolean supportsFunction(String function) {
        return availableFunctions.contains(function);
    }

    private static String requireIdentifier(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        if (!value.equals(value.trim())) {
            throw new IllegalArgumentException(label + " cannot contain surrounding whitespace");
        }
        return value;
    }
}
