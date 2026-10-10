package com.example.configtool.application.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.example.configtool.application.port.ConfigurationExporter;
import com.example.configtool.application.port.ConfigurationImporter;
import com.example.configtool.application.port.ConfigurationTemplateRepository;
import com.example.configtool.application.port.ProcessorRepository;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PeripheralConfiguration;
import com.example.configtool.domain.model.PeripheralDefinition;
import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.validation.ValidationContext;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.ValidationResult;

/** Stateful application facade shared by the GUI and CLI adapters. */
public final class ConfigurationService {

    private static final int HISTORY_LIMIT = 100;

    private final ProcessorRepository processorRepository;
    private final ValidationEngine validationEngine;
    private final Map<String, ConfigurationExporter> exporters;
    private final Map<String, ConfigurationImporter> importers;
    private final ConfigurationTemplateRepository templateRepository;
    private final Deque<SessionSnapshot> undoHistory = new ArrayDeque<>();
    private final Deque<SessionSnapshot> redoHistory = new ArrayDeque<>();

    private ProcessorDefinition currentProcessor;
    private Configuration currentConfiguration;
    private boolean dirty;

    public ConfigurationService(
            ProcessorRepository processorRepository,
            ValidationEngine validationEngine,
            List<ConfigurationExporter> exporters) {
        this(processorRepository, validationEngine, exporters, List.of(), new EmptyTemplateRepository());
    }

    public ConfigurationService(
            ProcessorRepository processorRepository,
            ValidationEngine validationEngine,
            List<ConfigurationExporter> exporters,
            List<ConfigurationImporter> importers,
            ConfigurationTemplateRepository templateRepository) {
        this.processorRepository = Objects.requireNonNull(processorRepository);
        this.validationEngine = Objects.requireNonNull(validationEngine);
        Objects.requireNonNull(exporters, "Exporters cannot be null");
        Objects.requireNonNull(importers, "Importers cannot be null");
        this.templateRepository = Objects.requireNonNull(templateRepository);
        this.exporters = new LinkedHashMap<>();
        exporters.forEach(exporter -> {
            String id = exporter.formatId().toLowerCase(Locale.ROOT);
            if (this.exporters.putIfAbsent(id, exporter) != null) {
                throw new IllegalArgumentException("Duplicate exporter format: " + id);
            }
        });
        this.importers = new LinkedHashMap<>();
        importers.forEach(importer -> {
            String id = importer.formatId().toLowerCase(Locale.ROOT);
            if (this.importers.putIfAbsent(id, importer) != null) {
                throw new IllegalArgumentException("Duplicate importer format: " + id);
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
        if (currentProcessor != null) {
            checkpoint();
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

    public synchronized List<PinAssignment> autoAssignPeripheral(String peripheralId) {
        var peripheral = requirePeripheral(peripheralId);
        Set<String> assignedSignals = currentConfiguration.pinAssignments().stream()
                .map(PinAssignment::function)
                .collect(Collectors.toSet());
        Set<String> usedPins = currentConfiguration.pinAssignments().stream()
                .map(PinAssignment::pinName)
                .collect(Collectors.toSet());
        List<String> targets = peripheral.signals().stream()
                .filter(signal -> !assignedSignals.contains(signal))
                .toList();
        if (targets.isEmpty()) {
            return List.of();
        }

        Map<String, List<String>> candidates = new LinkedHashMap<>();
        targets.forEach(signal -> candidates.put(signal, currentProcessor.pins().stream()
                .filter(pin -> !usedPins.contains(pin.name()) && pin.supportsFunction(signal))
                .sorted(Comparator.comparingInt(pin -> pin.physicalNumber()))
                .map(pin -> pin.name())
                .toList()));
        List<String> orderedTargets = targets.stream()
                .sorted(Comparator
                        .comparingInt((String signal) -> candidates.get(signal).size())
                        .thenComparing(signal -> signal))
                .toList();
        Map<String, String> plan = new LinkedHashMap<>();
        if (!findAssignmentPlan(orderedTargets, candidates, 0, new HashSet<>(usedPins), plan)) {
            List<String> unavailable = orderedTargets.stream()
                    .filter(signal -> candidates.get(signal).isEmpty())
                    .toList();
            throw new ConfigurationException(
                    "Cannot auto-assign all signals for " + peripheralId
                            + (unavailable.isEmpty()
                                    ? "; available pins conflict with existing assignments"
                                    : "; no free candidate pins for " + unavailable));
        }

        List<PinAssignment> added = orderedTargets.stream()
                .map(signal -> new PinAssignment(plan.get(signal), signal))
                .toList();
        Configuration next = currentConfiguration;
        for (PinAssignment assignment : added) {
            next = next.withAssignment(assignment);
        }
        update(next);
        return added;
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

    public synchronized void importConfiguration(
            String formatId,
            Path source,
            boolean discardUnsavedChanges) {
        Configuration imported = readConfiguration(formatId, source);
        ProcessorDefinition processor = processorRepository.findById(imported.processorId())
                .orElseThrow(() -> new ConfigurationException(
                        "Imported configuration references unknown processor: " + imported.processorId()));
        Configuration normalized = normalizeImportedConfiguration(processor, imported);
        if (dirty && !discardUnsavedChanges) {
            throw new UnsavedChangesException(
                    "The current configuration contains unsaved changes. Confirm replacement before import.");
        }
        checkpoint();
        currentProcessor = processor;
        currentConfiguration = normalized;
        dirty = false;
    }

    public synchronized List<ConfigurationDifference> diff(String formatId, Path source) {
        Configuration compared = readConfiguration(formatId, source);
        ProcessorDefinition comparedProcessor = processorRepository.findById(compared.processorId())
                .orElseThrow(() -> new ConfigurationException(
                        "Compared configuration references unknown processor: " + compared.processorId()));
        return compare(currentConfiguration,
                normalizeImportedConfiguration(comparedProcessor, compared));
    }

    public synchronized void saveTemplate(String name) {
        try {
            templateRepository.save(name, currentProcessor, currentConfiguration);
        } catch (IOException | IllegalArgumentException exception) {
            throw new ConfigurationException("Cannot save template '" + name + "'", exception);
        }
    }

    public synchronized List<String> templateNames() {
        try {
            return templateRepository.findAll();
        } catch (IOException exception) {
            throw new ConfigurationException("Cannot list configuration templates", exception);
        }
    }

    public synchronized void applyTemplate(String name, boolean discardUnsavedChanges) {
        Configuration template;
        try {
            template = templateRepository.findByName(name)
                    .orElseThrow(() -> new ConfigurationException("Unknown template: " + name));
        } catch (IOException | IllegalArgumentException exception) {
            throw new ConfigurationException("Cannot load template '" + name + "'", exception);
        }
        ProcessorDefinition processor = processorRepository.findById(template.processorId())
                .orElseThrow(() -> new ConfigurationException(
                        "Template references unknown processor: " + template.processorId()));
        Configuration normalized = normalizeImportedConfiguration(processor, template);
        if (dirty && !discardUnsavedChanges) {
            throw new UnsavedChangesException(
                    "The current configuration contains unsaved changes. Confirm replacement before applying template.");
        }
        checkpoint();
        currentProcessor = processor;
        currentConfiguration = normalized;
        dirty = true;
    }

    public synchronized boolean canUndo() {
        return !undoHistory.isEmpty();
    }

    public synchronized boolean canRedo() {
        return !redoHistory.isEmpty();
    }

    public synchronized void undo() {
        if (undoHistory.isEmpty()) {
            throw new ConfigurationException("Nothing to undo");
        }
        redoHistory.push(snapshot());
        restore(undoHistory.pop());
    }

    public synchronized void redo() {
        if (redoHistory.isEmpty()) {
            throw new ConfigurationException("Nothing to redo");
        }
        undoHistory.push(snapshot());
        restore(redoHistory.pop());
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

    public Set<String> importFormats() {
        return Set.copyOf(importers.keySet());
    }

    private PeripheralDefinition requirePeripheral(String peripheralId) {
        return currentProcessor.findPeripheral(peripheralId)
                .orElseThrow(() -> new ConfigurationException("Unknown peripheral: " + peripheralId));
    }

    private void update(Configuration next) {
        if (!next.equals(currentConfiguration)) {
            checkpoint();
            currentConfiguration = next;
            dirty = true;
        }
    }

    private Configuration readConfiguration(String formatId, Path source) {
        ConfigurationImporter importer = importers.get(formatId.toLowerCase(Locale.ROOT));
        if (importer == null) {
            throw new ConfigurationException("Unknown import format: " + formatId);
        }
        try {
            return importer.importConfiguration(source);
        } catch (IOException exception) {
            throw new ConfigurationException("Cannot import configuration from " + source, exception);
        }
    }

    private static Configuration normalizeImportedConfiguration(
            ProcessorDefinition processor,
            Configuration imported) {
        Set<String> expectedPeripheralIds = processor.peripherals().stream()
                .map(peripheral -> peripheral.id())
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> actualPeripheralIds = new TreeSet<>(imported.peripheralConfigurations().keySet());
        if (!expectedPeripheralIds.equals(actualPeripheralIds)) {
            Set<String> missing = new TreeSet<>(expectedPeripheralIds);
            missing.removeAll(actualPeripheralIds);
            Set<String> unknown = new TreeSet<>(actualPeripheralIds);
            unknown.removeAll(expectedPeripheralIds);
            throw new ConfigurationException(
                    "Peripheral states do not match " + processor.id()
                            + "; missing=" + missing + ", unknown=" + unknown);
        }

        Map<String, PeripheralConfiguration> orderedStates = new LinkedHashMap<>();
        processor.peripherals().forEach(peripheral -> {
            PeripheralConfiguration state = imported.peripheralConfigurations().get(peripheral.id());
            if (!peripheral.id().equals(state.peripheralId())) {
                throw new ConfigurationException(
                        "Peripheral state key does not match its id: " + peripheral.id());
            }
            orderedStates.put(peripheral.id(), state);
        });
        for (PinAssignment assignment : imported.pinAssignments()) {
            var pin = processor.findPin(assignment.pinName())
                    .orElseThrow(() -> new ConfigurationException(
                            "Imported assignment references unknown pin: " + assignment.pinName()));
            if (!pin.supportsFunction(assignment.function())) {
                throw new ConfigurationException(
                        assignment.function() + " is not supported by imported pin " + assignment.pinName());
            }
        }
        return new Configuration(
                processor.id(), imported.powerMode(), orderedStates, imported.pinAssignments());
    }

    private static List<ConfigurationDifference> compare(
            Configuration current,
            Configuration compared) {
        List<ConfigurationDifference> differences = new ArrayList<>();
        addDifference(differences, "processorId", current.processorId(), compared.processorId());
        addDifference(differences, "powerMode", current.powerMode().name(), compared.powerMode().name());

        Set<String> peripheralIds = new TreeSet<>(current.peripheralConfigurations().keySet());
        peripheralIds.addAll(compared.peripheralConfigurations().keySet());
        peripheralIds.forEach(id -> addDifference(
                differences,
                "peripheral." + id,
                formatPeripheral(current.peripheralConfigurations().get(id)),
                formatPeripheral(compared.peripheralConfigurations().get(id))));

        Map<String, String> currentPins = assignmentsByPin(current);
        Map<String, String> comparedPins = assignmentsByPin(compared);
        Set<String> pinNames = new TreeSet<>(currentPins.keySet());
        pinNames.addAll(comparedPins.keySet());
        pinNames.forEach(pin -> addDifference(
                differences, "pin." + pin, currentPins.get(pin), comparedPins.get(pin)));
        return List.copyOf(differences);
    }

    private static Map<String, String> assignmentsByPin(Configuration configuration) {
        return configuration.pinAssignments().stream().collect(Collectors.groupingBy(
                PinAssignment::pinName,
                LinkedHashMap::new,
                Collectors.mapping(PinAssignment::function,
                        Collectors.collectingAndThen(Collectors.toList(), values -> values.stream()
                                .sorted().collect(Collectors.joining(","))))));
    }

    private static String formatPeripheral(PeripheralConfiguration state) {
        return state == null ? null : "enabled=" + state.enabled() + ",clock=" + state.clockEnabled();
    }

    private static void addDifference(
            List<ConfigurationDifference> differences,
            String field,
            String current,
            String compared) {
        if (!Objects.equals(current, compared)) {
            differences.add(new ConfigurationDifference(field, current, compared));
        }
    }

    private static boolean findAssignmentPlan(
            List<String> signals,
            Map<String, List<String>> candidates,
            int index,
            Set<String> usedPins,
            Map<String, String> plan) {
        if (index == signals.size()) {
            return true;
        }
        String signal = signals.get(index);
        for (String pin : candidates.get(signal)) {
            if (usedPins.add(pin)) {
                plan.put(signal, pin);
                if (findAssignmentPlan(signals, candidates, index + 1, usedPins, plan)) {
                    return true;
                }
                plan.remove(signal);
                usedPins.remove(pin);
            }
        }
        return false;
    }

    private void checkpoint() {
        if (currentProcessor == null) {
            return;
        }
        undoHistory.push(snapshot());
        while (undoHistory.size() > HISTORY_LIMIT) {
            undoHistory.removeLast();
        }
        redoHistory.clear();
    }

    private SessionSnapshot snapshot() {
        return new SessionSnapshot(currentProcessor.id(), currentConfiguration, dirty);
    }

    private void restore(SessionSnapshot snapshot) {
        currentProcessor = processorRepository.findById(snapshot.processorId())
                .orElseThrow(() -> new ConfigurationException(
                        "History references unavailable processor: " + snapshot.processorId()));
        currentConfiguration = snapshot.configuration();
        dirty = snapshot.dirty();
    }

    private record SessionSnapshot(String processorId, Configuration configuration, boolean dirty) {
    }

    private static final class EmptyTemplateRepository implements ConfigurationTemplateRepository {
        @Override
        public List<String> findAll() {
            return List.of();
        }

        @Override
        public java.util.Optional<Configuration> findByName(String name) {
            return java.util.Optional.empty();
        }

        @Override
        public void save(String name, ProcessorDefinition processor, Configuration configuration)
                throws IOException {
            throw new IOException("Configuration template storage is not configured");
        }
    }
}
