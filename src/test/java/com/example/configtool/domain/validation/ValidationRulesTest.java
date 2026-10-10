package com.example.configtool.domain.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;

class ValidationRulesTest {

    private ProcessorDefinition processor;
    private Configuration configuration;
    private ValidationEngine engine;

    @BeforeEach
    void setUp() {
        processor = new JsonProcessorRepository(null).findById("ProcessorA").orElseThrow();
        configuration = Configuration.emptyFor(processor);
        engine = new ValidationEngine(DefaultValidationRules.create());
    }

    @Test
    void detectsPinMultiplexingConflict() {
        configuration = new Configuration(
                processor.id(), PowerMode.RUN, configuration.peripheralConfigurations(),
                List.of(
                        new PinAssignment("PTA2", "LPUART0_TX"),
                        new PinAssignment("PTA2", "SPI0_MOSI")));

        assertHasError("PIN_MULTIPLEXING");
    }

    @Test
    void detectsEnabledPeripheralWithoutClock() {
        configuration = configuration.withPeripheralEnabled("CAN0", true);

        assertHasError("PERIPHERAL_CLOCK");
    }

    @Test
    void enforcesOnlyDocumentedLowPowerRestrictions() {
        configuration = configuration
                .withPeripheralEnabled("CAN0", true)
                .withClockEnabled("CAN0", true)
                .withPowerMode(PowerMode.LOW_POWER);
        assertHasError("POWER_MODE");

        configuration = Configuration.emptyFor(processor)
                .withPeripheralEnabled("LPUART0", true)
                .withClockEnabled("LPUART0", true)
                .withPowerMode(PowerMode.LOW_POWER);
        assertFalse(hasIssue("POWER_MODE"));

        configuration = Configuration.emptyFor(processor)
                .withPeripheralEnabled("ADC0", true)
                .withClockEnabled("ADC0", true)
                .withPowerMode(PowerMode.LOW_POWER);
        assertFalse(hasIssue("POWER_MODE"));
    }

    @Test
    void detectsUnsupportedFunction() {
        configuration = new Configuration(
                processor.id(), PowerMode.RUN, configuration.peripheralConfigurations(),
                List.of(new PinAssignment("PTA0", "SPI0_SCK")));

        assertHasError("SUPPORTED_FUNCTION");
    }

    @Test
    void detectsDuplicateExclusiveSignalAcrossAlternativePins() {
        configuration = new Configuration(
                processor.id(), PowerMode.RUN, configuration.peripheralConfigurations(),
                List.of(
                        new PinAssignment("PTA12", "CAN0_TX"),
                        new PinAssignment("PTB5", "CAN0_TX")));

        assertHasError("DUPLICATE_EXCLUSIVE_SIGNAL");
    }

    @Test
    void detectsAssignmentToDisabledPeripheral() {
        configuration = configuration.withAssignment(new PinAssignment("PTA12", "CAN0_TX"));

        assertHasError("DISABLED_PERIPHERAL_ASSIGNMENT");
    }

    @Test
    void reportsWarningsSeparatelyFromErrors() {
        configuration = configuration
                .withPeripheralEnabled("LPUART0", true)
                .withClockEnabled("LPUART0", true);

        ValidationResult result = validate();

        assertTrue(result.isValid());
        assertEquals(0, result.errorCount());
        assertEquals(1, result.warningCount());
        assertTrue(hasIssue("PERIPHERAL_WITHOUT_RESOURCES"));
    }

    private ValidationResult validate() {
        return engine.validate(new ValidationContext(processor, configuration));
    }

    private boolean hasIssue(String code) {
        return validate().issues().stream().anyMatch(issue -> issue.code().equals(code));
    }

    private void assertHasError(String code) {
        assertTrue(validate().issues().stream()
                .anyMatch(issue -> issue.code().equals(code)
                        && issue.severity() == ValidationSeverity.ERROR));
    }
}
