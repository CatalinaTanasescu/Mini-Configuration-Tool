package com.example.configtool.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public record PeripheralDefinition(
        String id,
        PeripheralCategory category,
        List<String> signals,
        Set<String> exclusiveSignals) {

    /** Preserves the original convention-derived behavior for legacy definitions. */
    public PeripheralDefinition(String id, PeripheralCategory category, List<String> signals) {
        this(id, category, signals, Set.copyOf(signals));
    }

    public PeripheralDefinition {
        Objects.requireNonNull(id, "Peripheral id cannot be null");
        category = Objects.requireNonNull(category, "Peripheral category cannot be null");
        Objects.requireNonNull(signals, "Peripheral signals cannot be null");
        Objects.requireNonNull(exclusiveSignals, "Exclusive signals cannot be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Peripheral id cannot be blank");
        }
        signals = signals.stream().distinct().sorted().toList();
        if (signals.isEmpty()) {
            throw new IllegalArgumentException("Peripheral must expose at least one signal");
        }
        exclusiveSignals = Set.copyOf(exclusiveSignals);
        if (!signals.containsAll(exclusiveSignals)) {
            throw new IllegalArgumentException(
                    "Exclusive signals must belong to peripheral " + id);
        }
    }

    public boolean ownsSignal(String signalName) {
        return signals.contains(signalName);
    }

    public boolean isSignalExclusive(String signalName) {
        return exclusiveSignals.contains(signalName);
    }
}
