package com.example.configtool.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable user-selected state, deliberately separate from hardware capabilities. */
/**
 * Immutable snapshot of all choices made for one processor.
 * Static hardware capabilities remain in {@link ProcessorDefinition}; keeping them separate from
 * selected state makes history, templates, comparison, and interface-independent testing simpler.
 */
public final class Configuration {

    private final String processorId;
    private final PowerMode powerMode;
    private final Map<String, PeripheralConfiguration> peripheralConfigurations;
    private final List<PinAssignment> pinAssignments;

    /**
     * @param processorId processor to which this snapshot belongs
     * @param powerMode selected operating mode
     * @param peripheralConfigurations state of every processor peripheral
     * @param pinAssignments selected pin functions
     */
    public Configuration(
            String processorId,
            PowerMode powerMode,
            Map<String, PeripheralConfiguration> peripheralConfigurations,
            List<PinAssignment> pinAssignments) {
        this.processorId = requireText(processorId, "Processor id");
        this.powerMode = Objects.requireNonNull(powerMode, "Power mode cannot be null");
        Objects.requireNonNull(peripheralConfigurations, "Peripheral configurations cannot be null");
        Objects.requireNonNull(pinAssignments, "Pin assignments cannot be null");
        this.peripheralConfigurations = Collections.unmodifiableMap(
                new LinkedHashMap<>(peripheralConfigurations));
        this.pinAssignments = List.copyOf(pinAssignments);
    }

    /**
     * @param processor hardware definition used to initialize peripheral states
     * @return an empty RUN-mode snapshot with every peripheral and clock disabled
     */
    public static Configuration emptyFor(ProcessorDefinition processor) {
        Map<String, PeripheralConfiguration> states = new LinkedHashMap<>();
        processor.peripherals().forEach(peripheral -> states.put(
                peripheral.id(), new PeripheralConfiguration(peripheral.id(), false, false)));
        return new Configuration(processor.id(), PowerMode.RUN, states, List.of());
    }

    public String processorId() {
        return processorId;
    }

    public PowerMode powerMode() {
        return powerMode;
    }

    public Map<String, PeripheralConfiguration> peripheralConfigurations() {
        return peripheralConfigurations;
    }

    public List<PinAssignment> pinAssignments() {
        return pinAssignments;
    }

    /**
     * @param pinName logical pin name
     * @return assignment for the pin, if present
     */
    public Optional<PinAssignment> assignmentFor(String pinName) {
        return pinAssignments.stream().filter(assignment -> assignment.pinName().equals(pinName)).findFirst();
    }

    /** @return a copy with the requested power mode */
    public Configuration withPowerMode(PowerMode mode) {
        return new Configuration(processorId, mode, peripheralConfigurations, pinAssignments);
    }

    /** @return a copy with the requested peripheral enable state */
    public Configuration withPeripheralEnabled(String peripheralId, boolean enabled) {
        PeripheralConfiguration current = requirePeripheral(peripheralId);
        Map<String, PeripheralConfiguration> next = new LinkedHashMap<>(peripheralConfigurations);
        next.put(peripheralId, current.withEnabled(enabled));
        return new Configuration(processorId, powerMode, next, pinAssignments);
    }

    /** @return a copy with the requested peripheral clock state */
    public Configuration withClockEnabled(String peripheralId, boolean enabled) {
        PeripheralConfiguration current = requirePeripheral(peripheralId);
        Map<String, PeripheralConfiguration> next = new LinkedHashMap<>(peripheralConfigurations);
        next.put(peripheralId, current.withClockEnabled(enabled));
        return new Configuration(processorId, powerMode, next, pinAssignments);
    }

    /**
     * Returns a copy containing the assignment. Any previous assignment for the same physical pin
     * is removed, which models the normal interactive multiplexing behavior.
     *
     * @param assignment pin/function pair to store
     * @return updated immutable snapshot
     */
    public Configuration withAssignment(PinAssignment assignment) {
        List<PinAssignment> next = new ArrayList<>(pinAssignments);
        next.removeIf(existing -> existing.pinName().equals(assignment.pinName()));
        next.add(assignment);
        return new Configuration(processorId, powerMode, peripheralConfigurations, next);
    }

    /** @return a copy without an assignment for {@code pinName} */
    public Configuration withoutAssignment(String pinName) {
        List<PinAssignment> next = pinAssignments.stream()
                .filter(existing -> !existing.pinName().equals(pinName))
                .toList();
        return new Configuration(processorId, powerMode, peripheralConfigurations, next);
    }

    private PeripheralConfiguration requirePeripheral(String peripheralId) {
        PeripheralConfiguration state = peripheralConfigurations.get(peripheralId);
        if (state == null) {
            throw new IllegalArgumentException("Unknown peripheral: " + peripheralId);
        }
        return state;
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Configuration that)) {
            return false;
        }
        return processorId.equals(that.processorId)
                && powerMode == that.powerMode
                && peripheralConfigurations.equals(that.peripheralConfigurations)
                && pinAssignments.equals(that.pinAssignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processorId, powerMode, peripheralConfigurations, pinAssignments);
    }
}
