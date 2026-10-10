package com.example.configtool.domain.model;

import java.util.List;
import java.util.Objects;

public record PeripheralDefinition(String id, PeripheralCategory category, List<String> signals) {

    public PeripheralDefinition {
        Objects.requireNonNull(id, "Peripheral id cannot be null");
        category = Objects.requireNonNull(category, "Peripheral category cannot be null");
        Objects.requireNonNull(signals, "Peripheral signals cannot be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Peripheral id cannot be blank");
        }
        signals = signals.stream().distinct().sorted().toList();
        if (signals.isEmpty()) {
            throw new IllegalArgumentException("Peripheral must expose at least one signal");
        }
    }
}
