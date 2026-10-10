package com.example.configtool.infrastructure.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class JsonConfigurationExporterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsStableParseableConfigurationSchema() throws Exception {
        ConfigurationService service = new ConfigurationService(
                new JsonProcessorRepository(null),
                new ValidationEngine(DefaultValidationRules.create()),
                List.of(new JsonConfigurationExporter()));
        service.setPeripheralEnabled("LPUART0", true);
        service.setClockEnabled("LPUART0", true);
        service.assignPin("PTA2", "LPUART0_TX");
        Path target = temporaryDirectory.resolve("configuration.json");

        service.export("json", target);

        JsonNode root = new ObjectMapper().readTree(target.toFile());
        assertEquals("1.0", root.path("schemaVersion").asText());
        assertEquals("ProcessorA", root.path("processorId").asText());
        assertEquals("RUN", root.path("powerMode").asText());
        assertEquals("PTA2", root.path("pinAssignments").get(0).path("pinName").asText());
        assertEquals("LPUART0", root.path("pinAssignments").get(0).path("peripheralId").asText());
        assertTrue(root.path("peripherals").isArray());
    }
}
