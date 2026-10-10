package com.example.configtool.infrastructure.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.infrastructure.export.JsonConfigurationExporter;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;

class JsonConfigurationImporterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsExportedConfiguration() throws Exception {
        var processor = new JsonProcessorRepository(null).findById("ProcessorA").orElseThrow();
        Configuration configuration = Configuration.emptyFor(processor)
                .withPeripheralEnabled("LPUART0", true)
                .withClockEnabled("LPUART0", true)
                .withAssignment(new PinAssignment("PTA2", "LPUART0_TX"));
        Path file = temporaryDirectory.resolve("configuration.json");
        new JsonConfigurationExporter().export(processor, configuration, file);

        Configuration imported = new JsonConfigurationImporter().importConfiguration(file);

        assertEquals(configuration, imported);
    }

    @Test
    void rejectsUnsupportedSchemaVersion() throws IOException {
        Path file = temporaryDirectory.resolve("future.json");
        Files.writeString(file, """
                {
                  "schemaVersion":"99.0",
                  "processorId":"ProcessorA",
                  "powerMode":"RUN",
                  "peripherals":[],
                  "pinAssignments":[]
                }
                """);

        IOException exception = assertThrows(IOException.class,
                () -> new JsonConfigurationImporter().importConfiguration(file));

        assertEquals("Unsupported configuration schema version: 99.0", exception.getMessage());
    }
}
