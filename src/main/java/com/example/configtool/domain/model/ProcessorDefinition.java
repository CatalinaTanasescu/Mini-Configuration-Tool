package com.example.configtool.domain.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable aggregate containing the static capabilities of one processor.
 * It resolves pins, peripheral ownership, and signal policy for services and validation rules,
 * avoiding processor-specific conditionals in either presentation adapter.
 *
 * @param id stable machine-readable identifier
 * @param displayName user-facing processor name
 * @param pins physical pin capabilities
 * @param peripherals explicit or compatibility-derived peripheral metadata
 * @param independentSignals non-GPIO functions that intentionally have no peripheral owner
 */
public record ProcessorDefinition(
        String id,
        String displayName,
        List<PinDefinition> pins,
        List<PeripheralDefinition> peripherals,
        Set<String> independentSignals) {

    /**
     * Creates a definition and derives legacy peripheral metadata from pin functions.
     *
     * @param id stable processor identifier
     * @param displayName user-facing name
     * @param pins processor pins
     */
    public ProcessorDefinition(
            String id,
            String displayName,
            List<PinDefinition> pins,
            List<PeripheralDefinition> peripherals) {
        this(id, displayName, pins, peripherals, Set.of());
    }

    /** Validates aggregate-wide uniqueness and signal-ownership invariants. */
    public ProcessorDefinition {
        id = requireText(id, "Processor id");
        displayName = requireText(displayName, "Processor display name");
        Objects.requireNonNull(pins, "Pins cannot be null");
        Objects.requireNonNull(peripherals, "Peripherals cannot be null");
        Objects.requireNonNull(independentSignals, "Independent signals cannot be null");
        pins = List.copyOf(pins);
        peripherals = List.copyOf(peripherals);
        independentSignals = Set.copyOf(independentSignals);
        if (pins.isEmpty()) {
            throw new IllegalArgumentException("Processor must contain at least one pin");
        }
        ensureUnique(pins.stream().map(PinDefinition::name).toList(), "pin names");
        ensureUnique(pins.stream().map(PinDefinition::physicalNumber).toList(), "physical pin numbers");
        ensureUnique(peripherals.stream().map(PeripheralDefinition::id).toList(), "peripheral ids");
        ensureUnique(peripherals.stream().flatMap(peripheral -> peripheral.signals().stream()).toList(),
                "peripheral signal owners");
        validateSignalCoverage(pins, peripherals, independentSignals);
    }

    /** @return number of physical pins in the definition */
    public int pinCount() {
        return pins.size();
    }

    /** @return matching pin, or an empty optional when the name is unknown */
    public Optional<PinDefinition> findPin(String pinName) {
        return pins.stream().filter(pin -> pin.name().equals(pinName)).findFirst();
    }

    /** @return matching peripheral, or an empty optional when the identifier is unknown */
    public Optional<PeripheralDefinition> findPeripheral(String peripheralId) {
        return peripherals.stream().filter(peripheral -> peripheral.id().equals(peripheralId)).findFirst();
    }

    /** @return the peripheral that owns the signal, if any */
    public Optional<PeripheralDefinition> findPeripheralForSignal(String signalName) {
        return peripherals.stream().filter(peripheral -> peripheral.ownsSignal(signalName)).findFirst();
    }

    /** Resolves signal ownership from this processor's validated catalog. */
    /** @return contextual signal metadata, or empty for GPIO and independent signals */
    public Optional<SignalDescriptor> resolveSignal(String signalName) {
        return findPeripheralForSignal(signalName)
                .map(peripheral -> new SignalDescriptor(
                        peripheral.id(), peripheral.category(), signalName));
    }

    /** @return whether metadata marks the signal as exclusive */
    public boolean isSignalExclusive(String signalName) {
        return findPeripheralForSignal(signalName)
                .map(peripheral -> peripheral.isSignalExclusive(signalName))
                .orElse(false);
    }

    /** @return whether the signal intentionally bypasses peripheral ownership */
    public boolean isIndependentSignal(String signalName) {
        return independentSignals.contains(signalName);
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

    private static void validateSignalCoverage(
            List<PinDefinition> pins,
            List<PeripheralDefinition> peripherals,
            Set<String> independentSignals) {
        Set<String> availableSignals = new HashSet<>();
        pins.forEach(pin -> pin.availableFunctions().stream()
                .filter(function -> !"GPIO".equals(function))
                .forEach(availableSignals::add));
        Set<String> peripheralSignals = new HashSet<>();
        peripherals.forEach(peripheral -> peripheralSignals.addAll(peripheral.signals()));

        Set<String> unavailablePeripheralSignals = new HashSet<>(peripheralSignals);
        unavailablePeripheralSignals.removeAll(availableSignals);
        if (!unavailablePeripheralSignals.isEmpty()) {
            throw new IllegalArgumentException(
                    "Peripheral metadata references unavailable signals: " + unavailablePeripheralSignals);
        }

        Set<String> unavailableIndependentSignals = new HashSet<>(independentSignals);
        unavailableIndependentSignals.removeAll(availableSignals);
        if (!unavailableIndependentSignals.isEmpty()) {
            throw new IllegalArgumentException(
                    "Independent signals are not available on any pin: " + unavailableIndependentSignals);
        }

        Set<String> ambiguousSignals = new HashSet<>(peripheralSignals);
        ambiguousSignals.retainAll(independentSignals);
        if (!ambiguousSignals.isEmpty()) {
            throw new IllegalArgumentException(
                    "Signals cannot be both peripheral-owned and independent: " + ambiguousSignals);
        }

        Set<String> unmanagedSignals = new HashSet<>(availableSignals);
        unmanagedSignals.removeAll(peripheralSignals);
        unmanagedSignals.removeAll(independentSignals);
        if (!unmanagedSignals.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unmanaged pin functions require peripheral metadata or independentSignals: "
                            + unmanagedSignals);
        }
    }
}
