package com.example.configtool.infrastructure.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.application.port.ValidationRuleRepository;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;
import com.example.configtool.domain.validation.rules.ConfiguredValidationRule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Loads declarative validation rules from a user-managed JSON directory.
 * References are checked against discovered processor definitions before rules reach the engine,
 * preventing misspelled conditions from becoming silently inactive policies.
 */
public final class JsonValidationRuleRepository implements ValidationRuleRepository {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private final ObjectMapper mapper;
    private final Path directory;
    private final List<ProcessorDefinition> processors;

    /**
     * Creates a repository that validates references against bundled processors.
     *
     * @param directory rule directory, or {@code null} to disable custom rules
     */
    public JsonValidationRuleRepository(Path directory) {
        this(directory, new JsonProcessorRepository(null).findAll());
    }

    /**
     * Creates a repository using the same processor catalog as the application session.
     *
     * @param directory rule directory, or {@code null} to disable custom rules
     * @param processors processor definitions used to validate rule references
     */
    public JsonValidationRuleRepository(Path directory, List<ProcessorDefinition> processors) {
        this.mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.directory = directory;
        this.processors = List.copyOf(Objects.requireNonNull(processors, "Processors cannot be null"));
    }

    @Override
    public List<ValidationRule> findAll() {
        if (directory == null) {
            return List.of();
        }
        if (!Files.isDirectory(directory)) {
            throw new ValidationRuleDefinitionException(
                    "Custom validation rule directory does not exist: " + directory.toAbsolutePath());
        }
        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException exception) {
            throw new ValidationRuleDefinitionException(
                    "Cannot list custom validation rules: " + directory.toAbsolutePath(), exception);
        }

        List<ValidationRule> rules = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        for (Path file : files) {
            try (InputStream input = Files.newInputStream(file)) {
                RuleFile definition = mapper.readValue(input, RuleFile.class);
                if (definition.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
                    throw new IllegalArgumentException(
                            "Unsupported rule schema version " + definition.schemaVersion());
                }
                ConditionsFile conditions = definition.when();
                if (conditions == null) {
                    throw new IllegalArgumentException("Missing when object");
                }
                validateReferences(conditions);
                ConfiguredValidationRule rule = new ConfiguredValidationRule(
                        definition.code(),
                        definition.severity(),
                        definition.message(),
                        definition.suggestion(),
                        conditions.processorIds(),
                        conditions.powerModes(),
                        conditions.allEnabledPeripherals(),
                        conditions.anyEnabledPeripherals(),
                        conditions.allAssignedFunctions(),
                        conditions.anyAssignedFunctions());
                if (!codes.add(rule.code())) {
                    throw new IllegalArgumentException("Duplicate custom rule code " + rule.code());
                }
                rules.add(rule);
            } catch (Exception exception) {
                throw new ValidationRuleDefinitionException(
                        "Invalid custom validation rule in " + file.toAbsolutePath()
                                + ": " + exception.getMessage(), exception);
            }
        }
        return List.copyOf(rules);
    }

    private void validateReferences(ConditionsFile conditions) {
        Set<String> processorIds = values(conditions.processorIds());
        Set<String> availableProcessorIds = processors.stream()
                .map(ProcessorDefinition::id)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        Set<String> unknownProcessors = new TreeSet<>(processorIds);
        unknownProcessors.removeAll(availableProcessorIds);
        if (!unknownProcessors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown processor reference(s): " + unknownProcessors);
        }

        List<ProcessorDefinition> relevantProcessors = processorIds.isEmpty()
                ? processors
                : processors.stream().filter(processor -> processorIds.contains(processor.id())).toList();
        Set<String> peripheralReferences = union(
                conditions.allEnabledPeripherals(), conditions.anyEnabledPeripherals());
        Set<String> functionReferences = union(
                conditions.allAssignedFunctions(), conditions.anyAssignedFunctions());

        for (ProcessorDefinition processor : relevantProcessors) {
            Set<String> availablePeripherals = processor.peripherals().stream()
                    .map(peripheral -> peripheral.id())
                    .collect(java.util.stream.Collectors.toSet());
            Set<String> unknownPeripherals = new TreeSet<>(peripheralReferences);
            unknownPeripherals.removeAll(availablePeripherals);
            if (!unknownPeripherals.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown peripheral reference(s) for " + processor.id() + ": "
                                + unknownPeripherals);
            }

            Set<String> availableFunctions = processor.pins().stream()
                    .flatMap(pin -> pin.availableFunctions().stream())
                    .collect(java.util.stream.Collectors.toSet());
            Set<String> unknownFunctions = new TreeSet<>(functionReferences);
            unknownFunctions.removeAll(availableFunctions);
            if (!unknownFunctions.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unknown assigned-function reference(s) for " + processor.id() + ": "
                                + unknownFunctions);
            }
        }
    }

    @SafeVarargs
    private static <T> Set<T> union(Set<T>... sets) {
        Set<T> result = new LinkedHashSet<>();
        for (Set<T> set : sets) {
            result.addAll(values(set));
        }
        return Set.copyOf(result);
    }

    private static <T> Set<T> values(Set<T> values) {
        return values == null ? Set.of() : values;
    }

    private record RuleFile(
            int schemaVersion,
            String code,
            ValidationSeverity severity,
            String message,
            String suggestion,
            ConditionsFile when) {
    }

    private record ConditionsFile(
            Set<String> processorIds,
            Set<PowerMode> powerModes,
            Set<String> allEnabledPeripherals,
            Set<String> anyEnabledPeripherals,
            Set<String> allAssignedFunctions,
            Set<String> anyAssignedFunctions) {
    }
}
