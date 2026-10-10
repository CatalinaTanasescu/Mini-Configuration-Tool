package com.example.configtool.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record ProcessorDefinition(
        String id,
        String displayName,
        List<PinDefinition> pins,
        List<PeripheralDefinition> peripherals) {

    public ProcessorDefinition {
        id = requireText(id, "Processor id");
        displayName = requireText(displayName, "Processor display name");
        Objects.requireNonNull(pins, "Pins cannot be null");
        Objects.requireNonNull(peripherals, "Peripherals cannot be null");
        pins = List.copyOf(pins);
        peripherals = List.copyOf(peripherals);
        if (pins.isEmpty()) {
            throw new IllegalArgumentException("Processor must contain at least one pin");
        }
        ensureUnique(pins.stream().map(PinDefinition::name).toList(), "pin names");
        ensureUnique(pins.stream().map(PinDefinition::physicalNumber).toList(), "physical pin numbers");
        ensureUnique(peripherals.stream().map(PeripheralDefinition::id).toList(), "peripheral ids");
        ensureUnique(peripherals.stream().flatMap(peripheral -> peripheral.signals().stream()).toList(),
                "peripheral signal owners");
    }

    public int pinCount() {
        return pins.size();
    }

    public Optional<PinDefinition> findPin(String pinName) {
        return pins.stream().filter(pin -> pin.name().equals(pinName)).findFirst();
    }

    public Optional<PeripheralDefinition> findPeripheral(String peripheralId) {
        return peripherals.stream().filter(peripheral -> peripheral.id().equals(peripheralId)).findFirst();
    }

    public Optional<PeripheralDefinition> findPeripheralForSignal(String signalName) {
        return peripherals.stream().filter(peripheral -> peripheral.ownsSignal(signalName)).findFirst();
    }

    /** Resolves signal ownership from this processor's validated catalog. */
    public Optional<SignalDescriptor> resolveSignal(String signalName) {
        return findPeripheralForSignal(signalName)
                .map(peripheral -> new SignalDescriptor(
                        peripheral.id(), peripheral.category(), signalName));
    }

    public boolean isSignalExclusive(String signalName) {
        return findPeripheralForSignal(signalName)
                .map(peripheral -> peripheral.isSignalExclusive(signalName))
                .orElse(false);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    private static void ensureUnique(List<?> values, String label) {
        Set<?> unique = new HashSet<>(values);
        if (unique.size() != values.size()) {
            throw new IllegalArgumentException("Duplicate " + label + " are not allowed");
        }
    }
}
