package com.example.configtool.infrastructure.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.example.configtool.application.port.ValidationRuleRepository;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationRule;
import com.example.configtool.domain.validation.ValidationSeverity;
import com.example.configtool.domain.validation.rules.ConfiguredValidationRule;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonValidationRuleRepository implements ValidationRuleRepository {

    private static final int SUPPORTED_SCHEMA_VERSION = 1;
    private final ObjectMapper mapper;
    private final Path directory;

    public JsonValidationRuleRepository(Path directory) {
        this.mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.directory = directory;
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
