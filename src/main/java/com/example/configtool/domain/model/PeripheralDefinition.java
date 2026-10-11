package com.example.configtool.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable hardware metadata describing a peripheral and the signals it owns.
 *
 * @param id stable peripheral identifier
 * @param category category used by cross-cutting policies such as power-mode validation
 * @param signals functions owned by this peripheral
 * @param exclusiveSignals owned signals that may appear on at most one physical pin
 */
public record PeripheralDefinition(
        String id,
        PeripheralCategory category,
        List<String> signals,
        Set<String> exclusiveSignals) {

    /** Preserves the original convention-derived behavior for legacy definitions. */
    /**
     * Creates a legacy-compatible definition in which every owned signal is exclusive.
     *
     * @param id stable peripheral identifier
     * @param category peripheral category
     * @param signals owned signals
     */
    public PeripheralDefinition(String id, PeripheralCategory category, List<String> signals) {
        this(id, category, signals, Set.copyOf(signals));
    }

    /** Validates identifiers, defensive copies, and the exclusivity subset invariant. */
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

    /** @return whether this peripheral owns {@code signalName} */
    public boolean ownsSignal(String signalName) {
        return signals.contains(signalName);
    }

    /** @return whether {@code signalName} has single-pin exclusivity */
    public boolean isSignalExclusive(String signalName) {
        return exclusiveSignals.contains(signalName);
    }
}
