package com.example.configtool.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.example.configtool.application.port.ConfigurationExporter;
import com.example.configtool.application.port.ProcessorRepository;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.ValidationResult;

/** Stateful application facade shared by the GUI and CLI adapters. */
public final class ConfigurationService {

    private final ProcessorRepository processorRepository;
    private final ValidationEngine validationEngine;
    private final Map<String, ConfigurationExporter> exporters;

    private ProcessorDefinition currentProcessor;
    private Configuration currentConfiguration;
    private boolean dirty;

    public ConfigurationService(
            ProcessorRepository processorRepository,
            ValidationEngine validationEngine,
            List<ConfigurationExporter> exporters) {
        this.processorRepository = Objects.requireNonNull(processorRepository);
        this.validationEngine = Objects.requireNonNull(validationEngine);
        Objects.requireNonNull(exporters, "Exporters cannot be null");
        this.exporters = new LinkedHashMap<>();
        exporters.forEach(exporter -> {
            String id = exporter.formatId().toLowerCase(Locale.ROOT);
            if (this.exporters.putIfAbsent(id, exporter) != null) {
                throw new IllegalArgumentException("Duplicate exporter format: " + id);
            }
        });
        List<ProcessorDefinition> processors = availableProcessors();
        if (processors.isEmpty()) {
            throw new ConfigurationException("No processors are available");
        }
        selectProcessor(processors.getFirst().id(), true);
    }

    public synchronized List<ProcessorDefinition> availableProcessors() {
        return processorRepository.findAll();
    }

    public synchronized void selectProcessor(String processorId, boolean discardUnsavedChanges) {
        ProcessorDefinition selected = processorRepository.findById(processorId)
                .orElseThrow(() -> new ConfigurationException("Unknown processor: " + processorId));
        if (currentProcessor != null && currentProcessor.id().equals(selected.id())) {
            return;
        }
        if (dirty && !discardUnsavedChanges) {
            throw new UnsavedChangesException(
                    "The current configuration contains unsaved changes. Confirm reset before selecting "
                            + processorId + ".");
        }
        currentProcessor = selected;
        currentConfiguration = Configuration.emptyFor(selected);
        dirty = false;
    }

    public synchronized void setPowerMode(PowerMode mode) {
        update(currentConfiguration.withPowerMode(mode));
    }

    public synchronized void setPeripheralEnabled(String peripheralId, boolean enabled) {
        requirePeripheral(peripheralId);
        update(currentConfiguration.withPeripheralEnabled(peripheralId, enabled));
    }

    public synchronized void setClockEnabled(String peripheralId, boolean enabled) {
        requirePeripheral(peripheralId);
        update(currentConfiguration.withClockEnabled(peripheralId, enabled));
    }

    public synchronized void assignPin(String pinName, String function) {
        var pin = currentProcessor.findPin(pinName)
                .orElseThrow(() -> new ConfigurationException("Unknown pin: " + pinName));
        if (!pin.supportsFunction(function)) {
            throw new ConfigurationException(
                    function + " is not supported by " + pinName + ". Available functions: "
                            + String.join(", ", pin.availableFunctions()));
        }
        update(currentConfiguration.withAssignment(new PinAssignment(pinName, function)));
    }

    public synchronized void clearPin(String pinName) {
        if (currentProcessor.findPin(pinName).isEmpty()) {
            throw new ConfigurationException("Unknown pin: " + pinName);
        }
        update(currentConfiguration.withoutAssignment(pinName));
    }

    public synchronized ValidationResult validate() {
        return validationEngine.validate(new ValidationContext(currentProcessor, currentConfiguration));
    }

    public synchronized void export(String formatId, Path target) {
        ConfigurationExporter exporter = exporters.get(formatId.toLowerCase(Locale.ROOT));
        if (exporter == null) {
            throw new ConfigurationException("Unknown export format: " + formatId);
        }
        ValidationResult result = validate();
        if (!result.isValid()) {
            throw new ExportBlockedException(result);
        }
        try {
            exporter.export(currentProcessor, currentConfiguration, target);
            dirty = false;
        } catch (IOException exception) {
            throw new ConfigurationException("Cannot export configuration to " + target, exception);
        }
    }

    public synchronized ConfigurationSummary summary() {
        ValidationResult result = validate();
        return new ConfigurationSummary(
                currentProcessor.id(),
                currentProcessor.pinCount(),
                currentConfiguration.pinAssignments().size(),
                currentConfiguration.peripheralConfigurations().values().stream()
                        .filter(state -> state.enabled()).count(),
                currentConfiguration.peripheralConfigurations().values().stream()
                        .filter(state -> state.clockEnabled()).count(),
                currentConfiguration.powerMode(),
                result,
                dirty);
    }

    public synchronized ProcessorDefinition currentProcessor() {
        return currentProcessor;
    }

    public synchronized Configuration currentConfiguration() {
        return currentConfiguration;
    }

    public synchronized boolean hasUnsavedChanges() {
        return dirty;
    }

    public Set<String> exportFormats() {
        return Set.copyOf(exporters.keySet());
    }

    private void requirePeripheral(String peripheralId) {
        if (currentProcessor.findPeripheral(peripheralId).isEmpty()) {
            throw new ConfigurationException("Unknown peripheral: " + peripheralId);
        }
    }

    private void update(Configuration next) {
        if (!next.equals(currentConfiguration)) {
            currentConfiguration = next;
            dirty = true;
        }
    }
}
