package com.example.configtool.infrastructure.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.example.configtool.application.port.ProcessorRepository;
import com.example.configtool.domain.model.PeripheralCatalog;
import com.example.configtool.domain.model.PeripheralCategory;
import com.example.configtool.domain.model.PeripheralDefinition;
import com.example.configtool.domain.model.PinDefinition;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Loads bundled processors from an explicit manifest and optional external JSON files. */
public final class JsonProcessorRepository implements ProcessorRepository {

    private static final String INDEX_RESOURCE = "processors/index.json";
    private static final int SUPPORTED_SCHEMA_VERSION = 1;

    private final ObjectMapper mapper;
    private final ClassLoader classLoader;
    private final Path externalDirectory;
    private final Map<String, ProcessorDefinition> processors;

    public JsonProcessorRepository(Path externalDirectory) {
        this(new ObjectMapper(), Thread.currentThread().getContextClassLoader(), externalDirectory);
    }

    JsonProcessorRepository(ObjectMapper mapper, ClassLoader classLoader, Path externalDirectory) {
        this.mapper = mapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        this.classLoader = classLoader;
        this.externalDirectory = externalDirectory;
        this.processors = loadAll();
    }

    @Override
    public List<ProcessorDefinition> findAll() {
        return processors.values().stream()
                .sorted(Comparator.comparing(ProcessorDefinition::displayName))
                .toList();
    }

    @Override
    public Optional<ProcessorDefinition> findById(String processorId) {
        return Optional.ofNullable(processors.get(processorId));
    }

    private Map<String, ProcessorDefinition> loadAll() {
        Map<String, ProcessorDefinition> loaded = new LinkedHashMap<>();
        loadBundled(loaded);
        loadExternal(loaded);
        if (loaded.isEmpty()) {
            throw new ProcessorDefinitionException("No valid processor definitions were discovered");
        }
        return Map.copyOf(loaded);
    }

    private void loadBundled(Map<String, ProcessorDefinition> loaded) {
        try (InputStream indexStream = classLoader.getResourceAsStream(INDEX_RESOURCE)) {
            if (indexStream == null) {
                throw new ProcessorDefinitionException("Missing classpath processor index: " + INDEX_RESOURCE);
            }
            List<String> resources = mapper.readValue(indexStream, new TypeReference<List<String>>() { });
            if (resources.isEmpty()) {
                throw new ProcessorDefinitionException("Processor index is empty");
            }
            for (String resource : resources) {
                try (InputStream definitionStream = classLoader.getResourceAsStream(resource)) {
                    if (definitionStream == null) {
                        throw new ProcessorDefinitionException("Indexed processor resource not found: " + resource);
                    }
                    addUnique(loaded, readDefinition(definitionStream, "classpath:" + resource), resource);
                }
            }
        } catch (IOException exception) {
            throw new ProcessorDefinitionException("Cannot read bundled processor definitions", exception);
        }
    }

    private void loadExternal(Map<String, ProcessorDefinition> loaded) {
        if (externalDirectory == null) {
            return;
        }
        if (!Files.exists(externalDirectory)) {
            throw new ProcessorDefinitionException(
                    "External processor directory does not exist: " + externalDirectory.toAbsolutePath());
        }
        if (!Files.isDirectory(externalDirectory)) {
            throw new ProcessorDefinitionException(
                    "External processor path is not a directory: " + externalDirectory.toAbsolutePath());
        }

        List<Path> files = new ArrayList<>();
        try (Stream<Path> paths = Files.list(externalDirectory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted()
                    .forEach(files::add);
        } catch (IOException exception) {
            throw new ProcessorDefinitionException(
                    "Cannot list external processor directory: " + externalDirectory.toAbsolutePath(), exception);
        }

        for (Path file : files) {
            try (InputStream input = Files.newInputStream(file)) {
                addUnique(loaded, readDefinition(input, file.toAbsolutePath().toString()), file.toString());
            } catch (IOException exception) {
                throw new ProcessorDefinitionException("Cannot read processor definition: " + file, exception);
            }
        }
    }

    private ProcessorDefinition readDefinition(InputStream input, String source) {
        try {
            ProcessorFile file = mapper.readValue(input, ProcessorFile.class);
            if (file.schemaVersion() != SUPPORTED_SCHEMA_VERSION) {
                throw new ProcessorDefinitionException(
                        "Unsupported processor schema version " + file.schemaVersion() + " in " + source);
            }
            if (file.pins() == null) {
                throw new ProcessorDefinitionException("Missing pins array in " + source);
            }
            List<PinDefinition> pins = file.pins().stream()
                    .map(pin -> new PinDefinition(pin.name(), pin.physicalNumber(), pin.availableFunctions()))
                    .toList();
            List<PeripheralDefinition> explicitPeripherals = file.peripherals() == null
                    ? List.of()
                    : file.peripherals().stream()
                            .map(peripheral -> new PeripheralDefinition(
                                    peripheral.id(),
                                    peripheral.category(),
                                    peripheral.signals(),
                                    peripheral.exclusiveSignals() == null
                                            ? Set.of()
                                            : peripheral.exclusiveSignals()))
                            .toList();
            return new ProcessorDefinition(
                    file.id(),
                    file.displayName() == null ? file.id() : file.displayName(),
                    pins,
                    PeripheralCatalog.fromPins(pins, explicitPeripherals),
                    file.independentSignals() == null ? Set.of() : file.independentSignals());
        } catch (ProcessorDefinitionException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ProcessorDefinitionException(
                    "Invalid processor definition in " + source + ": " + exception.getMessage(), exception);
        }
    }

    private static void addUnique(
            Map<String, ProcessorDefinition> loaded,
            ProcessorDefinition definition,
            String source) {
        ProcessorDefinition existing = loaded.putIfAbsent(definition.id(), definition);
        if (existing != null) {
            throw new ProcessorDefinitionException(
                    "Duplicate processor id '" + definition.id() + "' discovered in " + source);
        }
    }

    private record ProcessorFile(
            int schemaVersion,
            String id,
            String displayName,
            List<PinFile> pins,
            List<PeripheralFile> peripherals,
            Set<String> independentSignals) {
    }

    private record PinFile(String name, int physicalNumber, List<String> availableFunctions) {
    }

    private record PeripheralFile(
            String id,
            PeripheralCategory category,
            List<String> signals,
            Set<String> exclusiveSignals) {
    }
}
