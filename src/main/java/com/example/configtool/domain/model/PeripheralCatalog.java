package com.example.configtool.domain.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PeripheralCatalog {

    private PeripheralCatalog() {
    }

    public static List<PeripheralDefinition> fromPins(List<PinDefinition> pins) {
        Map<String, MutablePeripheral> peripherals = new LinkedHashMap<>();
        pins.stream()
                .flatMap(pin -> pin.availableFunctions().stream())
                .map(SignalResolver::resolve)
                .flatMap(java.util.Optional::stream)
                .forEach(signal -> peripherals
                        .computeIfAbsent(signal.peripheralId(),
                                ignored -> new MutablePeripheral(signal.category()))
                        .signals.add(signal.signalName()));

        return peripherals.entrySet().stream()
                .map(entry -> new PeripheralDefinition(
                        entry.getKey(), entry.getValue().category, entry.getValue().signals))
                .sorted(Comparator.comparing(PeripheralDefinition::id))
                .toList();
    }

    private static final class MutablePeripheral {
        private final PeripheralCategory category;
        private final List<String> signals = new ArrayList<>();

        private MutablePeripheral(PeripheralCategory category) {
            this.category = category;
        }
    }
}
