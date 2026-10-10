package com.example.configtool.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.application.port.ConfigurationExporter;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.domain.validation.ValidationSeverity;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;
import com.example.configtool.infrastructure.export.JsonConfigurationExporter;
import com.example.configtool.infrastructure.export.XmlConfigurationExporter;
import com.example.configtool.infrastructure.importer.JsonConfigurationImporter;
import com.example.configtool.infrastructure.persistence.JsonConfigurationTemplateRepository;

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

    @Test
    void undoAndRedoRestoreImmutableConfigurationSnapshots() {
        ConfigurationService service = serviceWith(new RecordingExporter());

        service.setPowerMode(PowerMode.LOW_POWER);
        assertTrue(service.canUndo());

        service.undo();
        assertEquals(PowerMode.RUN, service.currentConfiguration().powerMode());
        assertTrue(service.canRedo());

        service.redo();
        assertEquals(PowerMode.LOW_POWER, service.currentConfiguration().powerMode());
    }

    @Test
    void autoAssignmentFindsDeterministicNonConflictingPins() {
        ConfigurationService service = serviceWith(new RecordingExporter());
        service.setPeripheralEnabled("LPUART0", true);
        service.setClockEnabled("LPUART0", true);

        var assignments = service.autoAssignPeripheral("LPUART0");

        assertEquals(2, assignments.size());
        assertEquals(Set.of("LPUART0_RX", "LPUART0_TX"), assignments.stream()
                .map(assignment -> assignment.function()).collect(Collectors.toSet()));
        assertEquals(2, assignments.stream().map(assignment -> assignment.pinName()).distinct().count());
        assertTrue(service.validate().isValid());
    }

    @Test
    void importsDiffsAndRestoresExportedJson() {
        ConfigurationService service = featureService();
        service.setPeripheralEnabled("LPUART0", true);
        service.setClockEnabled("LPUART0", true);
        service.assignPin("PTA2", "LPUART0_TX");
        Path exported = temporaryDirectory.resolve("baseline.json");
        service.export("json", exported);
        service.clearPin("PTA2");

        assertTrue(service.diff("json", exported).stream()
                .anyMatch(difference -> difference.field().equals("pin.PTA2")));
        assertThrows(UnsavedChangesException.class,
                () -> service.importConfiguration("json", exported, false));

        service.importConfiguration("json", exported, true);
        assertEquals("LPUART0_TX", service.currentConfiguration()
                .assignmentFor("PTA2").orElseThrow().function());
        assertFalse(service.hasUnsavedChanges());
    }

    @Test
    void savesAndAppliesReusableConfigurationTemplate() {
        ConfigurationService service = featureService();
        service.setPeripheralEnabled("LPUART0", true);
        service.setClockEnabled("LPUART0", true);
        service.assignPin("PTA2", "LPUART0_TX");
        service.saveTemplate("uart-console");
        service.selectProcessor("ProcessorB", true);

        service.applyTemplate("uart-console", true);

        assertEquals(List.of("uart-console"), service.templateNames());
        assertEquals("ProcessorA", service.currentProcessor().id());
        assertEquals("LPUART0_TX", service.currentConfiguration()
                .assignmentFor("PTA2").orElseThrow().function());
        assertTrue(service.hasUnsavedChanges());
    }

    private static ConfigurationService serviceWith(ConfigurationExporter exporter) {
        return new ConfigurationService(
                new JsonProcessorRepository(null),
                new ValidationEngine(DefaultValidationRules.create()),
                List.of(exporter));
    }

    private ConfigurationService featureService() {
        return new ConfigurationService(
                new JsonProcessorRepository(null),
                new ValidationEngine(DefaultValidationRules.create()),
                List.of(new JsonConfigurationExporter(), new XmlConfigurationExporter()),
                List.of(new JsonConfigurationImporter()),
                new JsonConfigurationTemplateRepository(temporaryDirectory.resolve("templates")));
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
