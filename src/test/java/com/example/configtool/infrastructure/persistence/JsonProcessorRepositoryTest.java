package com.example.configtool.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.domain.model.ProcessorDefinition;

class JsonProcessorRepositoryTest {

    private static final Map<String, String> REVIEWED_PIN_MAP_DIGESTS = Map.of(
            "ProcessorA", "9c9827f9290a59f08edf44d2257670a48bcf70da466c4a90e778b8d90c519c83",
            "ProcessorB", "b9bc848623f3be003fd298a90bfd4e01c2c30ae5da5869d2a8df5db4a6338052",
            "ProcessorC", "51ed4adc8bd8b457796592aeb362aad623148193a9600abdcf999a3b330066d4");

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
    void bundledPinMapsMatchExhaustivelyReviewedSourceBaseline() {
        JsonProcessorRepository repository = new JsonProcessorRepository(null);

        REVIEWED_PIN_MAP_DIGESTS.forEach((processorId, expectedDigest) -> {
            ProcessorDefinition processor = repository.findById(processorId).orElseThrow();
            String canonicalPinMap = processor.pins().stream()
                    .map(pin -> pin.name() + "|" + pin.physicalNumber() + "|"
                            + String.join(",", pin.availableFunctions()))
                    .collect(Collectors.joining("\n"));

            assertEquals(expectedDigest, sha256(canonicalPinMap), processorId);
        });
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
    void loadsExplicitMetadataForSignalsOutsideLegacyNamingConventions() throws IOException {
        Files.writeString(temporaryDirectory.resolve("processor-d.json"), """
                {
                  "schemaVersion": 1,
                  "id": "ProcessorD",
                  "displayName": "Processor D",
                  "pins": [
                    {"name":"PTX0","physicalNumber":1,
                     "availableFunctions":["GPIO","WAKE_REQUEST","STATUS_BUS"]},
                    {"name":"PTX1","physicalNumber":2,
                     "availableFunctions":["GPIO","WAKE_REQUEST","STATUS_BUS"]}
                  ],
                  "peripherals": [
                    {
                      "id": "SAFETY0",
                      "category": "OTHER",
                      "signals": ["WAKE_REQUEST", "STATUS_BUS"],
                      "exclusiveSignals": ["WAKE_REQUEST"]
                    }
                  ]
                }
                """);

        ProcessorDefinition processor = new JsonProcessorRepository(temporaryDirectory)
                .findById("ProcessorD").orElseThrow();

        assertEquals("SAFETY0",
                processor.resolveSignal("WAKE_REQUEST").orElseThrow().peripheralId());
        assertTrue(processor.isSignalExclusive("WAKE_REQUEST"));
        assertFalse(processor.isSignalExclusive("STATUS_BUS"));
        assertEquals(2, processor.findPeripheral("SAFETY0").orElseThrow().signals().size());
    }

    @Test
    void rejectsExplicitMetadataThatReferencesUnavailableSignal() throws IOException {
        Files.writeString(temporaryDirectory.resolve("broken-metadata.json"), """
                {
                  "schemaVersion": 1,
                  "id": "BrokenMetadata",
                  "displayName": "Broken metadata",
                  "pins": [
                    {"name":"PTX0","physicalNumber":1,"availableFunctions":["GPIO"]}
                  ],
                  "peripherals": [
                    {
                      "id": "SAFETY0",
                      "category": "OTHER",
                      "signals": ["MISSING_SIGNAL"],
                      "exclusiveSignals": []
                    }
                  ]
                }
                """);

        ProcessorDefinitionException exception = assertThrows(
                ProcessorDefinitionException.class,
                () -> new JsonProcessorRepository(temporaryDirectory));

        assertTrue(exception.getMessage().contains("references unavailable signal MISSING_SIGNAL"));
    }

    @Test
    void rejectsUnmanagedFunctionWithActionableDiagnostic() throws IOException {
        Files.writeString(temporaryDirectory.resolve("unmanaged.json"), """
                {
                  "schemaVersion": 1,
                  "id": "Unmanaged",
                  "pins": [
                    {"name":"PTX0","physicalNumber":1,
                     "availableFunctions":["GPIO","BOARD_WAKE"]}
                  ]
                }
                """);

        ProcessorDefinitionException exception = assertThrows(
                ProcessorDefinitionException.class,
                () -> new JsonProcessorRepository(temporaryDirectory));

        assertTrue(exception.getMessage().contains(
                "Unmanaged pin functions require peripheral metadata or independentSignals"));
    }

    @Test
    void loadsExplicitIndependentSignalWithoutInventingPeripheralOrClock() throws IOException {
        Files.writeString(temporaryDirectory.resolve("independent.json"), """
                {
                  "schemaVersion": 1,
                  "id": "Independent",
                  "pins": [
                    {"name":"PTX0","physicalNumber":1,
                     "availableFunctions":["GPIO","BOARD_WAKE"]}
                  ],
                  "independentSignals": ["BOARD_WAKE"]
                }
                """);

        ProcessorDefinition processor = new JsonProcessorRepository(temporaryDirectory)
                .findById("Independent").orElseThrow();

        assertTrue(processor.isIndependentSignal("BOARD_WAKE"));
        assertTrue(processor.peripherals().isEmpty());
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

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 must be available in every Java runtime", exception);
        }
    }
}
