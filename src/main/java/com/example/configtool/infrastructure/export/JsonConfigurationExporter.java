package com.example.configtool.infrastructure.export;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.configtool.application.port.ConfigurationExporter;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.model.SignalResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class JsonConfigurationExporter implements ConfigurationExporter {

    public static final String FORMAT_ID = "json";
    private static final String SCHEMA_VERSION = "1.0";
    private final ObjectMapper mapper;

    public JsonConfigurationExporter() {
        this(new ObjectMapper());
    }

    JsonConfigurationExporter(ObjectMapper mapper) {
        this.mapper = mapper.copy().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public String formatId() {
        return FORMAT_ID;
    }

    @Override
    public String fileExtension() {
        return "json";
    }

    @Override
    public void export(ProcessorDefinition processor, Configuration configuration, Path target)
            throws IOException {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent == null) {
            throw new IOException("Export target must have a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absoluteTarget.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            mapper.writeValue(temporary.toFile(), toDocument(processor, configuration));
            try {
                Files.move(temporary, absoluteTarget,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static Map<String, Object> toDocument(
            ProcessorDefinition processor,
            Configuration configuration) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("schemaVersion", SCHEMA_VERSION);
        document.put("processorId", configuration.processorId());
        document.put("powerMode", configuration.powerMode().name());

        List<Map<String, Object>> peripherals = processor.peripherals().stream().map(peripheral -> {
            var state = configuration.peripheralConfigurations().get(peripheral.id());
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", peripheral.id());
            value.put("category", peripheral.category().name());
            value.put("enabled", state.enabled());
            value.put("clockEnabled", state.clockEnabled());
            return value;
        }).toList();
        document.put("peripherals", peripherals);

        List<Map<String, Object>> assignments = configuration.pinAssignments().stream()
                .sorted((left, right) -> {
                    int leftNumber = processor.findPin(left.pinName()).orElseThrow().physicalNumber();
                    int rightNumber = processor.findPin(right.pinName()).orElseThrow().physicalNumber();
                    return Integer.compare(leftNumber, rightNumber);
                })
                .map(assignment -> {
                    Map<String, Object> value = new LinkedHashMap<>();
                    value.put("physicalPinNumber",
                            processor.findPin(assignment.pinName()).orElseThrow().physicalNumber());
                    value.put("pinName", assignment.pinName());
                    value.put("function", assignment.function());
                    SignalResolver.resolve(assignment.function())
                            .ifPresent(signal -> value.put("peripheralId", signal.peripheralId()));
                    return value;
                })
                .toList();
        document.put("pinAssignments", assignments);
        return document;
    }
}
