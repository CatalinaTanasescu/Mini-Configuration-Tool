package com.example.configtool.presentation.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.bootstrap.ApplicationBootstrap;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.export.JsonConfigurationExporter;
import com.example.configtool.infrastructure.export.XmlConfigurationExporter;
import com.example.configtool.infrastructure.importer.JsonConfigurationImporter;
import com.example.configtool.infrastructure.persistence.JsonConfigurationTemplateRepository;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;

import picocli.CommandLine;

class CliApplicationTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void scriptedSessionConfiguresValidatesSummarizesAndExports() {
        ConfigurationService service = ApplicationBootstrap.createService(null);
        StringWriter output = new StringWriter();
        CliApplication cli = new CliApplication(
                service,
                new BufferedReader(new StringReader("")),
                new PrintWriter(output, true));
        Path export = temporaryDirectory.resolve("cli-configuration.json");

        int exitCode = new CommandLine(cli).execute(
                "--script=enable LPUART0;clock LPUART0 on;assign PTA2 LPUART0_TX;"
                        + "validate;summary;export \"" + export + "\"");

        assertEquals(0, exitCode);
        assertTrue(output.toString().contains("VALID: no errors or warnings."));
        assertTrue(output.toString().contains("Processor:          ProcessorA"));
        assertTrue(export.toFile().isFile());
    }

    @Test
    void cliReportsSameValidationErrorsAsService() {
        ConfigurationService service = ApplicationBootstrap.createService(null);
        StringWriter output = new StringWriter();
        CliApplication cli = new CliApplication(
                service,
                new BufferedReader(new StringReader("")),
                new PrintWriter(output, true));

        int exitCode = new CommandLine(cli).execute(
                "--script=enable CAN0;power LOW_POWER;validate");

        assertEquals(3, exitCode);
        assertTrue(output.toString().contains("PERIPHERAL_CLOCK"));
        assertTrue(output.toString().contains("POWER_MODE"));
        assertEquals(2, service.validate().errorCount());
    }

    @Test
    void advancedCommandsShareTheSamePersistentSession() {
        ConfigurationService service = new ConfigurationService(
                new JsonProcessorRepository(null),
                new ValidationEngine(DefaultValidationRules.create()),
                List.of(new JsonConfigurationExporter(), new XmlConfigurationExporter()),
                List.of(new JsonConfigurationImporter()),
                new JsonConfigurationTemplateRepository(temporaryDirectory.resolve("templates")));
        StringWriter output = new StringWriter();
        CliApplication cli = new CliApplication(
                service,
                new BufferedReader(new StringReader("")),
                new PrintWriter(output, true));
        Path json = temporaryDirectory.resolve("advanced.json");
        Path xml = temporaryDirectory.resolve("advanced.xml");

        int exitCode = new CommandLine(cli).execute(
                "--script=enable LPUART0;clock LPUART0 on;auto-assign LPUART0;"
                        + "export \"" + json + "\";export \"" + xml + "\" --format xml;"
                        + "clear PTA2;undo;diff \"" + json + "\";"
                        + "save-template uart;select ProcessorB --discard;"
                        + "apply-template uart --discard;validate");

        assertEquals(0, exitCode);
        assertTrue(json.toFile().isFile());
        assertTrue(xml.toFile().isFile());
        assertTrue(output.toString().contains("IDENTICAL: no configuration differences."));
        assertTrue(output.toString().contains("Applied template uart."));
        assertEquals("ProcessorA", service.currentProcessor().id());
        assertTrue(service.validate().isValid());
    }
}
