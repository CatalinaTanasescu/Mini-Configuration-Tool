package com.example.configtool.domain.validation.rules;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;

/**
 * Declarative rule loaded from user-managed JSON.
 * All non-empty condition groups must match; within an {@code any} group, at least one value must
 * match. Reference validation is performed by the repository before this domain rule is created.
 */
public final class ConfiguredValidationRule implements ValidationRule {

    private final String code;
    private final ValidationSeverity severity;
    private final String message;
    private final String suggestion;
    private final Set<String> processorIds;
    private final Set<PowerMode> powerModes;
    private final Set<String> allEnabledPeripherals;
    private final Set<String> anyEnabledPeripherals;
    private final Set<String> allAssignedFunctions;
    private final Set<String> anyAssignedFunctions;

    /**
     * @param code stable diagnostic code
     * @param severity issue severity
     * @param message user-facing explanation
     * @param suggestion corrective guidance
     * @param processorIds optional processor filter
     * @param powerModes optional power-mode filter
     * @param allEnabledPeripherals peripherals that must all be enabled
     * @param anyEnabledPeripherals peripherals of which at least one must be enabled
     * @param allAssignedFunctions functions that must all be assigned
     * @param anyAssignedFunctions functions of which at least one must be assigned
     * @throws IllegalArgumentException if no condition is provided
     */
    public ConfiguredValidationRule(
            String code,
            ValidationSeverity severity,
            String message,
            String suggestion,
            Set<String> processorIds,
            Set<PowerMode> powerModes,
            Set<String> allEnabledPeripherals,
            Set<String> anyEnabledPeripherals,
            Set<String> allAssignedFunctions,
            Set<String> anyAssignedFunctions) {
        this.code = requireText(code, "Rule code");
        this.severity = Objects.requireNonNull(severity, "Rule severity cannot be null");
        this.message = requireText(message, "Rule message");
        this.suggestion = suggestion == null ? "" : suggestion;
        this.processorIds = copy(processorIds);
        this.powerModes = copy(powerModes);
        this.allEnabledPeripherals = copy(allEnabledPeripherals);
        this.anyEnabledPeripherals = copy(anyEnabledPeripherals);
        this.allAssignedFunctions = copy(allAssignedFunctions);
        this.anyAssignedFunctions = copy(anyAssignedFunctions);
        if (this.processorIds.isEmpty()
                && this.powerModes.isEmpty()
                && this.allEnabledPeripherals.isEmpty()
                && this.anyEnabledPeripherals.isEmpty()
                && this.allAssignedFunctions.isEmpty()
                && this.anyAssignedFunctions.isEmpty()) {
            throw new IllegalArgumentException("Configured rule must contain at least one condition");
        }
    }

    @Override
    public List<ValidationIssue> validate(ValidationContext context) {
        if (!processorIds.isEmpty() && !processorIds.contains(context.processor().id())) {
            return List.of();
        }
        if (!powerModes.isEmpty() && !powerModes.contains(context.configuration().powerMode())) {
            return List.of();
        }
        Set<String> enabled = context.configuration().peripheralConfigurations().values().stream()
                .filter(state -> state.enabled())
                .map(state -> state.peripheralId())
                .collect(Collectors.toSet());
        if (!enabled.containsAll(allEnabledPeripherals)
                || (!anyEnabledPeripherals.isEmpty()
                        && enabled.stream().noneMatch(anyEnabledPeripherals::contains))) {
            return List.of();
        }
        Set<String> assigned = context.configuration().pinAssignments().stream()
                .map(assignment -> assignment.function())
                .collect(Collectors.toSet());
        if (!assigned.containsAll(allAssignedFunctions)
                || (!anyAssignedFunctions.isEmpty()
                        && assigned.stream().noneMatch(anyAssignedFunctions::contains))) {
            return List.of();
        }
        String peripheralId = allEnabledPeripherals.size() == 1
                ? allEnabledPeripherals.iterator().next()
                : anyEnabledPeripherals.size() == 1
                        ? anyEnabledPeripherals.iterator().next()
                        : null;
        return List.of(new ValidationIssue(
                code, severity, message, suggestion, null, peripheralId));
    }

    /** @return stable diagnostic code, also used to detect duplicate configured rules */
    public String code() {
        return code;
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " cannot be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value;
    }

    private static <T> Set<T> copy(Set<T> values) {
        return values == null ? Set.of() : Set.copyOf(values);
    }
}
