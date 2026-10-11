package com.example.configtool.infrastructure.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.configtool.application.port.ConfigurationImporter;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PeripheralConfiguration;
import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.domain.model.PowerMode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Strict Jackson-based reader for versioned configuration JSON.
 * This adapter validates document shape and primitive values; processor-specific references and
 * business rules remain responsibilities of the application service and validation engine.
 */
public final class JsonConfigurationImporter implements ConfigurationImporter {

    private static final String SUPPORTED_SCHEMA_VERSION = "1.0";
    private final ObjectMapper mapper;

    /** Creates an importer with an isolated Jackson mapper. */
    public JsonConfigurationImporter() {
        this(new ObjectMapper());
    }

    JsonConfigurationImporter(ObjectMapper mapper) {
        this.mapper = mapper.copy();
    }

    @Override
    public String formatId() {
        return "json";
    }

    @Override
    public Configuration importConfiguration(Path source) throws IOException {
        try {
            JsonNode root = mapper.readTree(source.toFile());
            if (root == null || !root.isObject()) {
                throw new IOException("Configuration document must contain a JSON object");
            }
            String schemaVersion = requireText(root, "schemaVersion");
            if (!SUPPORTED_SCHEMA_VERSION.equals(schemaVersion)) {
                throw new IOException("Unsupported configuration schema version: " + schemaVersion);
            }
            String processorId = requireText(root, "processorId");
            PowerMode powerMode;
            try {
                powerMode = PowerMode.valueOf(requireText(root, "powerMode"));
            } catch (IllegalArgumentException exception) {
                throw new IOException("Invalid powerMode in configuration", exception);
            }

            JsonNode peripheralNodes = requireArray(root, "peripherals");
            Map<String, PeripheralConfiguration> peripherals = new LinkedHashMap<>();
            for (JsonNode node : peripheralNodes) {
                String id = requireText(node, "id");
                PeripheralConfiguration state = new PeripheralConfiguration(
                        id,
                        requireBoolean(node, "enabled"),
                        requireBoolean(node, "clockEnabled"));
                if (peripherals.putIfAbsent(id, state) != null) {
                    throw new IOException("Duplicate peripheral state in configuration: " + id);
                }
            }

            JsonNode assignmentNodes = requireArray(root, "pinAssignments");
            List<PinAssignment> assignments = new ArrayList<>();
            for (JsonNode node : assignmentNodes) {
                assignments.add(new PinAssignment(
                        requireText(node, "pinName"),
                        requireText(node, "function")));
            }
            return new Configuration(processorId, powerMode, peripherals, assignments);
        } catch (IOException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new IOException("Invalid JSON configuration: " + exception.getMessage(), exception);
        }
    }

    private static JsonNode requireArray(JsonNode parent, String field) throws IOException {
        JsonNode value = parent.path(field);
        if (!value.isArray()) {
            throw new IOException("Missing or invalid array field: " + field);
        }
        return value;
    }

    private static String requireText(JsonNode parent, String field) throws IOException {
        JsonNode value = parent.path(field);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IOException("Missing or invalid text field: " + field);
        }
        return value.asText();
    }

    private static boolean requireBoolean(JsonNode parent, String field) throws IOException {
        JsonNode value = parent.path(field);
        if (!value.isBoolean()) {
            throw new IOException("Missing or invalid boolean field: " + field);
        }
        return value.asBoolean();
    }
}
