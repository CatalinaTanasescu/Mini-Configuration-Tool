package com.example.configtool.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.example.configtool.application.port.ProcessorRepository;
import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.bootstrap.ApplicationBootstrap;
import com.example.configtool.domain.model.PeripheralCategory;
import com.example.configtool.domain.model.PeripheralDefinition;
import com.example.configtool.domain.model.PinDefinition;
import com.example.configtool.domain.model.ProcessorDefinition;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.presentation.cli.CliApplication;
import com.example.configtool.presentation.gui.MainWindow;
import com.example.configtool.presentation.gui.table.PeripheralTableModel;
import com.example.configtool.presentation.gui.table.PinTableModel;

import picocli.CommandLine;

class ArchitecturalAcceptanceTest {

    @Test
    void guiAndCliDependOnTheSameApplicationFacade() throws Exception {
        Constructor<MainWindow> guiConstructor = MainWindow.class
                .getConstructor(ConfigurationService.class);
        Constructor<CliApplication> cliConstructor = CliApplication.class
                .getConstructor(ConfigurationService.class);

        assertEquals(ConfigurationService.class, guiConstructor.getParameterTypes()[0]);
        assertEquals(ConfigurationService.class, cliConstructor.getParameterTypes()[0]);
    }

    @Test
    void equivalentGuiAndCliFlowsProduceIdenticalStateAndValidation() {
        ConfigurationService guiService = ApplicationBootstrap.createService(null);
        ConfigurationService cliService = ApplicationBootstrap.createService(null);

        PeripheralTableModel peripheralModel = new PeripheralTableModel(
                guiService, () -> { }, exception -> { throw exception; });
        int uartRow = findPeripheralRow(peripheralModel, "LPUART0");
        peripheralModel.setValueAt(true, uartRow, 0);
        peripheralModel.setValueAt(true, uartRow, 1);

        PinTableModel pinModel = new PinTableModel(
                guiService, () -> { }, exception -> { throw exception; });
        int pinRow = findPinRow(pinModel, "PTA2");
        pinModel.setValueAt("LPUART0_TX", pinRow, 3);

        StringWriter cliOutput = new StringWriter();
        CliApplication cli = new CliApplication(
                cliService,
                new BufferedReader(new StringReader("")),
                new PrintWriter(cliOutput, true));
        int exitCode = new CommandLine(cli).execute(
                "--script=enable LPUART0;clock LPUART0 on;assign PTA2 LPUART0_TX;validate");

        assertEquals(0, exitCode);
        assertEquals(guiService.currentConfiguration(), cliService.currentConfiguration());
        assertEquals(guiService.validate(), cliService.validate());
        assertTrue(guiService.validate().isValid());
    }

    @Test
    void metadataDefinedSignalFlowsThroughServiceAndGuiWithoutNamingConvention() {
        ProcessorDefinition processor = new ProcessorDefinition(
                "ProcessorD",
                "Processor D",
                List.of(new PinDefinition(
                        "PTX0", 1, List.of("GPIO", "WAKE_REQUEST"))),
                List.of(new PeripheralDefinition(
                        "SAFETY0",
                        PeripheralCategory.OTHER,
                        List.of("WAKE_REQUEST"),
                        Set.of("WAKE_REQUEST"))));
        ProcessorRepository repository = new ProcessorRepository() {
            @Override
            public List<ProcessorDefinition> findAll() {
                return List.of(processor);
            }

            @Override
            public Optional<ProcessorDefinition> findById(String processorId) {
                return processor.id().equals(processorId)
                        ? Optional.of(processor)
                        : Optional.empty();
            }
        };
        ConfigurationService service = new ConfigurationService(
                repository,
                new ValidationEngine(DefaultValidationRules.create()),
                List.of());
        service.setPeripheralEnabled("SAFETY0", true);
        service.setClockEnabled("SAFETY0", true);
        service.assignPin("PTX0", "WAKE_REQUEST");
        PinTableModel model = new PinTableModel(
                service, () -> { }, exception -> { throw exception; });

        assertEquals("SAFETY0", model.getValueAt(0, 4));
        assertTrue(service.validate().isValid());
    }

    private static int findPeripheralRow(PeripheralTableModel model, String id) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (model.peripheralAt(row).id().equals(id)) {
                return row;
            }
        }
        throw new AssertionError("Peripheral not found: " + id);
    }

    private static int findPinRow(PinTableModel model, String name) {
        for (int row = 0; row < model.getRowCount(); row++) {
            if (model.pinAt(row).name().equals(name)) {
                return row;
            }
        }
        throw new AssertionError("Pin not found: " + name);
    }
}
