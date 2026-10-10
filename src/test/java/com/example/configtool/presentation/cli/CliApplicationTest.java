package com.example.configtool.presentation.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.bootstrap.ApplicationBootstrap;

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
}
