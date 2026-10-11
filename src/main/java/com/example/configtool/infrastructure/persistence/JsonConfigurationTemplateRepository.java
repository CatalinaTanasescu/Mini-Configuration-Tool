package com.example.configtool.infrastructure.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.example.configtool.application.port.ConfigurationTemplateRepository;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.infrastructure.export.JsonConfigurationExporter;
import com.example.configtool.infrastructure.importer.JsonConfigurationImporter;

/**
 * Filesystem-backed template repository that reuses the production JSON importer and exporter.
 * Template names are constrained before path resolution so a name cannot escape the configured
 * directory.
 */
public final class JsonConfigurationTemplateRepository implements ConfigurationTemplateRepository {

    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private final Path directory;
    private final JsonConfigurationExporter exporter = new JsonConfigurationExporter();
    private final JsonConfigurationImporter importer = new JsonConfigurationImporter();

    /** @param directory root directory dedicated to template JSON files */
    public JsonConfigurationTemplateRepository(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    @Override
    public List<String> findAll() throws IOException {
        if (!Files.exists(directory)) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase().endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .sorted()
                    .forEach(names::add);
        }
        return List.copyOf(names);
    }

    @Override
    public Optional<Configuration> findByName(String name) throws IOException {
        Path path = pathFor(name);
        return Files.isRegularFile(path)
                ? Optional.of(importer.importConfiguration(path))
                : Optional.empty();
    }

    @Override
    public void save(
            String name,
            ProcessorDefinition processor,
            Configuration configuration) throws IOException {
        Files.createDirectories(directory);
        exporter.export(processor, configuration, pathFor(name));
    }

    private Path pathFor(String name) {
        if (name == null || !VALID_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException(
                    "Template name must use 1-64 letters, digits, dots, underscores or hyphens");
        }
        return directory.resolve(name + ".json").normalize();
    }
}
