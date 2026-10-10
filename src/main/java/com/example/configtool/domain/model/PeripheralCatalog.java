package com.example.configtool.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PeripheralCatalog {

    private PeripheralCatalog() {
    }

    public static List<PeripheralDefinition> fromPins(List<PinDefinition> pins) {
        return fromPins(pins, List.of());
    }

    /**
     * Merges optional explicit metadata with convention-derived legacy metadata.
     * Explicit signal ownership and exclusivity always win; the resolver is used
     * only for functions not covered by the JSON metadata.
     */
    public static List<PeripheralDefinition> fromPins(
            List<PinDefinition> pins,
            List<PeripheralDefinition> explicitPeripherals) {
        Map<String, MutablePeripheral> peripherals = new LinkedHashMap<>();
        Set<String> availableFunctions = pins.stream()
                .flatMap(pin -> pin.availableFunctions().stream())
                .filter(function -> !"GPIO".equals(function))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> explicitlyOwnedSignals = new HashSet<>();

        for (PeripheralDefinition explicit : explicitPeripherals) {
            MutablePeripheral existing = peripherals.putIfAbsent(
                    explicit.id(), new MutablePeripheral(explicit.category()));
            if (existing != null) {
                throw new IllegalArgumentException("Duplicate peripheral id: " + explicit.id());
            }
            for (String signal : explicit.signals()) {
                if (!availableFunctions.contains(signal)) {
                    throw new IllegalArgumentException(
                            "Peripheral " + explicit.id() + " references unavailable signal " + signal);
                }
                if (!explicitlyOwnedSignals.add(signal)) {
                    throw new IllegalArgumentException(
                            "Signal " + signal + " belongs to multiple peripherals");
                }
                peripherals.get(explicit.id()).add(signal, explicit.isSignalExclusive(signal));
            }
        }

        pins.stream()
                .flatMap(pin -> pin.availableFunctions().stream())
                .filter(function -> !explicitlyOwnedSignals.contains(function))
                .map(SignalResolver::resolve)
                .flatMap(java.util.Optional::stream)
                .forEach(signal -> {
                    MutablePeripheral peripheral = peripherals.computeIfAbsent(
                            signal.peripheralId(), ignored -> new MutablePeripheral(signal.category()));
                    if (peripheral.category != signal.category()) {
                        throw new IllegalArgumentException(
                                "Conflicting categories for peripheral " + signal.peripheralId());
                    }
                    peripheral.add(signal.signalName(), true);
                });

        return peripherals.entrySet().stream()
                .map(entry -> new PeripheralDefinition(
                        entry.getKey(),
                        entry.getValue().category,
                        entry.getValue().signals,
                        entry.getValue().exclusiveSignals))
                .sorted(Comparator.comparing(PeripheralDefinition::id))
                .toList();
    }

    private static final class MutablePeripheral {
        private final PeripheralCategory category;
        private final List<String> signals = new ArrayList<>();
        private final Set<String> exclusiveSignals = new HashSet<>();

        private MutablePeripheral(PeripheralCategory category) {
            this.category = category;
        }

        private void add(String signal, boolean exclusive) {
            if (!signals.contains(signal)) {
                signals.add(signal);
            }
            if (exclusive) {
                exclusiveSignals.add(signal);
            }
        }
    }
}
