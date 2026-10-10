package com.example.configtool.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class DomainModelTest {

    @Test
    void pinDefinitionDefensivelyCopiesFunctions() {
        List<String> functions = new ArrayList<>(List.of("GPIO", "CAN0_TX"));
        PinDefinition pin = new PinDefinition("PTA0", 1, functions);

        functions.add("SPI0_SCK");

        assertEquals(List.of("GPIO", "CAN0_TX"), pin.availableFunctions());
        assertThrows(UnsupportedOperationException.class,
                () -> pin.availableFunctions().add("SPI0_SCK"));
    }

    @Test
    void processorRejectsDuplicatePhysicalNumbers() {
        List<PinDefinition> pins = List.of(
                new PinDefinition("PTA0", 1, List.of("GPIO")),
                new PinDefinition("PTA1", 1, List.of("GPIO")));

        assertThrows(IllegalArgumentException.class,
                () -> new ProcessorDefinition("Test", "Test", pins, List.of()));
    }

    @Test
    void sourceSpecificSignalMappingIsExplicit() {
        assertEquals("I2C0", SignalResolver.resolve("I2C_SCL").orElseThrow().peripheralId());
        assertEquals(PeripheralCategory.UART,
                SignalResolver.resolve("LPUART3_TX").orElseThrow().category());
        assertEquals(PeripheralCategory.ETHERNET,
                SignalResolver.resolve("ETH_RX").orElseThrow().category());
    }

    @Test
    void interactiveAssignmentReplacesPreviousFunctionOnSamePin() {
        Configuration configuration = new Configuration(
                "Test", PowerMode.RUN, Map.of(), List.of())
                .withAssignment(new PinAssignment("PTA0", "GPIO"))
                .withAssignment(new PinAssignment("PTA0", "ADC0_CH0"));

        assertEquals(List.of(new PinAssignment("PTA0", "ADC0_CH0")),
                configuration.pinAssignments());
    }
}
