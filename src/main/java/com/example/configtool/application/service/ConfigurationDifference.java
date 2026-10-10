package com.example.configtool.application.service;

import java.util.Objects;

public record ConfigurationDifference(
        String field,
        String currentValue,
        String comparedValue) {

    public ConfigurationDifference {
        field = Objects.requireNonNull(field, "Diff field cannot be null");
        currentValue = currentValue == null ? "<absent>" : currentValue;
        comparedValue = comparedValue == null ? "<absent>" : comparedValue;
    }
}
