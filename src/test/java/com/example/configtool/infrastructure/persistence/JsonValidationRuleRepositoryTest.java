package com.example.configtool.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationContext;

class JsonValidationRuleRepositoryTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void loadsAndExecutesUserDefinedRule() throws IOException {
        Files.writeString(temporaryDirectory.resolve("low-power-uart.json"), """
                {
                  "schemaVersion": 1,
                  "code": "CUSTOM_UART_LOW_POWER",
                  "severity": "WARNING",
                  "message": "UART review is required in low power.",
                  "suggestion": "Confirm the board power budget.",
                  "when": {
                    "processorIds": ["ProcessorA"],
                    "powerModes": ["LOW_POWER"],
                    "allEnabledPeripherals": ["LPUART0"]
                  }
                }
                """);
        var processor = new JsonProcessorRepository(null).findById("ProcessorA").orElseThrow();
        Configuration configuration = Configuration.emptyFor(processor)
                .withPeripheralEnabled("LPUART0", true)
                .withPowerMode(PowerMode.LOW_POWER);

        var rules = new JsonValidationRuleRepository(temporaryDirectory).findAll();
        var issues = rules.getFirst().validate(new ValidationContext(processor, configuration));

        assertEquals(1, rules.size());
        assertEquals("CUSTOM_UART_LOW_POWER", issues.getFirst().code());
    }

    @Test
    void rejectsRuleWithoutConditions() throws IOException {
        Files.writeString(temporaryDirectory.resolve("invalid.json"), """
                {
                  "schemaVersion": 1,
                  "code": "INVALID",
                  "severity": "ERROR",
                  "message": "Always active is not allowed.",
                  "suggestion": "Add a condition.",
                  "when": {}
                }
                """);

        assertThrows(ValidationRuleDefinitionException.class,
                () -> new JsonValidationRuleRepository(temporaryDirectory).findAll());
    }
}
