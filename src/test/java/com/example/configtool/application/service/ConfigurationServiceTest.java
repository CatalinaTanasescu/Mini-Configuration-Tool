package com.example.configtool.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.application.port.ConfigurationExporter;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationSeverity;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;

class ConfigurationServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void processorChangeRequiresExplicitDiscardWhenStateIsDirty() {
        ConfigurationService service = serviceWith(new RecordingExporter());
        service.setPeripheralEnabled("CAN0", true);

        assertThrows(UnsavedChangesException.class,
                () -> service.selectProcessor("ProcessorB", false));
        assertEquals("ProcessorA", service.currentProcessor().id());

        service.selectProcessor("ProcessorB", true);
        assertEquals("ProcessorB", service.currentProcessor().id());
        assertFalse(service.hasUnsavedChanges());
        assertTrue(service.currentConfiguration().pinAssignments().isEmpty());
    }

    @Test
    void serviceRejectsUnsupportedAssignmentBeforeMutatingState() {
        ConfigurationService service = serviceWith(new RecordingExporter());

        assertThrows(ConfigurationException.class,
                () -> service.assignPin("PTA0", "SPI0_SCK"));
        assertTrue(service.currentConfiguration().pinAssignments().isEmpty());
    }

    @Test
    void newValidationRuleCanBeAddedWithoutChangingEngine() {
        ValidationEngine engine = new ValidationEngine(List.of(context -> List.of(
                new ValidationIssue("CUSTOM", ValidationSeverity.WARNING,
                        "Custom rule executed.", "No action required.", null, null))));
        ConfigurationService service = new ConfigurationService(
                new JsonProcessorRepository(null), engine, List.of(new RecordingExporter()));

        assertEquals("CUSTOM", service.validate().issues().getFirst().code());
    }

    @Test
    void alternateExporterUsesSameConfigurationLogic() {
        RecordingExporter exporter = new RecordingExporter();
        ConfigurationService service = serviceWith(exporter);

        service.export("test", temporaryDirectory.resolve("configuration.test"));

        assertTrue(exporter.called);
        assertEquals("ProcessorA", exporter.processor.id());
        assertEquals("ProcessorA", exporter.configuration.processorId());
        assertFalse(service.hasUnsavedChanges());
    }

    private static ConfigurationService serviceWith(ConfigurationExporter exporter) {
        return new ConfigurationService(
                new JsonProcessorRepository(null),
                new ValidationEngine(DefaultValidationRules.create()),
                List.of(exporter));
    }

    private static final class RecordingExporter implements ConfigurationExporter {
        private boolean called;
        private ProcessorDefinition processor;
        private Configuration configuration;

        @Override
        public String formatId() {
            return "test";
        }

        @Override
        public String fileExtension() {
            return "test";
        }

        @Override
        public void export(ProcessorDefinition processor, Configuration configuration, Path target)
                throws IOException {
            this.called = true;
            this.processor = processor;
            this.configuration = configuration;
        }
    }
}
