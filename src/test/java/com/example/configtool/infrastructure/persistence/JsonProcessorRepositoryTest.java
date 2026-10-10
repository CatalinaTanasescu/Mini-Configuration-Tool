package com.example.configtool.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.domain.model.ProcessorDefinition;

class JsonProcessorRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsAllBundledProcessorsWithRequiredPinCounts() {
        JsonProcessorRepository repository = new JsonProcessorRepository(null);

        assertEquals(3, repository.findAll().size());
        assertEquals(32, repository.findById("ProcessorA").orElseThrow().pinCount());
        assertEquals(48, repository.findById("ProcessorB").orElseThrow().pinCount());
        assertEquals(64, repository.findById("ProcessorC").orElseThrow().pinCount());
    }

    @Test
    void preservesRepresentativeSourceFunctionsExactly() {
        ProcessorDefinition processor = new JsonProcessorRepository(null)
                .findById("ProcessorA").orElseThrow();

        assertTrue(processor.findPin("PTA2").orElseThrow().supportsFunction("LPUART0_TX"));
        assertTrue(processor.findPin("PTA7").orElseThrow().supportsFunction("I2C_SCL"));
        assertTrue(!processor.findPin("PTA0").orElseThrow().supportsFunction("SPI0_SCK"));
    }

    @Test
    void pinIdentifiersAndPhysicalNumbersAreUnique() {
        new JsonProcessorRepository(null).findAll().forEach(processor -> {
            assertEquals(processor.pinCount(),
                    new HashSet<>(processor.pins().stream().map(pin -> pin.name()).toList()).size());
            assertEquals(processor.pinCount(),
                    new HashSet<>(processor.pins().stream().map(pin -> pin.physicalNumber()).toList()).size());
        });
    }

    @Test
    void discoversAdditionalProcessorFromExternalDirectory() throws IOException {
        Files.writeString(temporaryDirectory.resolve("processor-d.json"), """
                {
                  "schemaVersion": 1,
                  "id": "ProcessorD",
                  "displayName": "Processor D",
                  "pins": [
                    {"name":"PTX0","physicalNumber":1,"availableFunctions":["GPIO","LPUART9_TX"]},
                    {"name":"PTX1","physicalNumber":2,"availableFunctions":["GPIO","LPUART9_RX"]}
                  ]
                }
                """);

        JsonProcessorRepository repository = new JsonProcessorRepository(temporaryDirectory);

        assertEquals(4, repository.findAll().size());
        assertEquals(2, repository.findById("ProcessorD").orElseThrow().pinCount());
        assertTrue(repository.findById("ProcessorD").orElseThrow()
                .findPeripheral("LPUART9").isPresent());
    }

    @Test
    void rejectsMalformedExternalDefinitionWithActionableMessage() throws IOException {
        Files.writeString(temporaryDirectory.resolve("broken.json"), """
                {
                  "schemaVersion": 1,
                  "id": "Broken",
                  "displayName": "Broken",
                  "pins": [
                    {"name":"PTX0","physicalNumber":1,"availableFunctions":["GPIO"]},
                    {"name":"PTX0","physicalNumber":2,"availableFunctions":["GPIO"]}
                  ]
                }
                """);

        ProcessorDefinitionException exception = assertThrows(
                ProcessorDefinitionException.class,
                () -> new JsonProcessorRepository(temporaryDirectory));
        assertTrue(exception.getMessage().contains("Invalid processor definition"));
        assertTrue(exception.getMessage().contains("Duplicate pin names"));
    }
}
