package com.example.configtool.presentation.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationIssue;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Picocli presentation adapter for interactive and scripted configuration workflows.
 * Commands translate textual input into calls on {@link ConfigurationService}; they do not
 * reimplement validation or configuration rules, which keeps CLI results aligned with the GUI.
 */
@Command(
        name = "configtool",
        description = "Configure processor pins, peripherals, clocks and power mode.",
        mixinStandardHelpOptions = true,
        version = "NXP Mini Configuration Tool 0.0.1",
        subcommands = {
                CliApplication.ProcessorsCommand.class,
                CliApplication.SelectCommand.class,
                CliApplication.PinsCommand.class,
                CliApplication.PeripheralsCommand.class,
                CliApplication.EnableCommand.class,
                CliApplication.DisableCommand.class,
                CliApplication.ClockCommand.class,
                CliApplication.AssignCommand.class,
                CliApplication.ClearCommand.class,
                CliApplication.PowerCommand.class,
                CliApplication.ValidateCommand.class,
                CliApplication.SummaryCommand.class,
                CliApplication.ExportCommand.class,
                CliApplication.ImportCommand.class,
                CliApplication.UndoCommand.class,
                CliApplication.RedoCommand.class,
                CliApplication.AutoAssignCommand.class,
                CliApplication.DependenciesCommand.class,
                CliApplication.DiffCommand.class,
                CliApplication.TemplatesCommand.class,
                CliApplication.SaveTemplateCommand.class,
                CliApplication.ApplyTemplateCommand.class,
                CliApplication.ExitCommand.class,
                CommandLine.HelpCommand.class
        })
public final class CliApplication implements Callable<Integer> {

    private final ConfigurationService service;
    private final BufferedReader input;
    private final PrintWriter output;
    private boolean exitRequested;

    @Option(names = "--script",
            description = "Execute semicolon-separated commands in one persistent session.")
    private String script;

    @Option(names = "--no-banner", description = "Suppress the interactive banner.")
    private boolean noBanner;

    /**
     * Creates an interactive CLI using standard input and output.
     *
     * @param service shared application facade
     */
    public CliApplication(ConfigurationService service) {
        this(service,
                new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)),
                new PrintWriter(System.out, true));
    }

    /**
     * Creates a CLI with injected streams, primarily for deterministic adapter tests.
     *
     * @param service shared application facade
     * @param input command input
     * @param output command output and diagnostics
     */
    public CliApplication(ConfigurationService service, BufferedReader input, PrintWriter output) {
        this.service = service;
        this.input = input;
        this.output = output;
    }

    @Override
    /**
     * Runs a scripted session when {@code --script} is present, otherwise starts an interactive loop.
     *
     * @return process-style exit code
     * @throws IOException if interactive input cannot be read
     */
    public Integer call() throws IOException {
        CommandLine shell = configuredCommandLine();
        if (script != null && !script.isBlank()) {
            int result = 0;
            for (String command : splitScript(script)) {
                result = shell.execute(splitCommandLine(command));
                if (result != 0 || exitRequested) {
                    break;
                }
            }
            return result;
        }

        if (!noBanner) {
            output.println("NXP Mini Configuration Tool CLI");
            output.println("Type 'help' for commands and 'exit' to close the session.");
        }
        while (!exitRequested) {
            output.print("configtool> ");
            output.flush();
            String line = input.readLine();
            if (line == null) {
                break;
            }
            if (!line.isBlank()) {
                shell.execute(splitCommandLine(line));
            }
        }
        return 0;
    }

    /**
     * Builds the command tree and centralizes user-facing exception formatting.
     *
     * @return configured Picocli command dispatcher
     */
    public CommandLine configuredCommandLine() {
        CommandLine commandLine = new CommandLine(this);
        commandLine.setOut(output);
        commandLine.setErr(output);
        commandLine.setExecutionExceptionHandler((exception, ignored, parseResult) -> {
            output.println("ERROR: " + exception.getMessage());
            return 2;
        });
        commandLine.setParameterExceptionHandler((exception, arguments) -> {
            output.println("ERROR: " + exception.getMessage());
            output.println("Use 'help' or '<command> --help' for usage.");
            return 2;
        });
        return commandLine;
    }

    @Command(name = "processors", description = "List discovered processors.")
    static final class ProcessorsCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            String current = parent.service.currentProcessor().id();
            parent.service.availableProcessors().forEach(processor -> parent.output.printf(
                    "%s %-14s %3d pins%n",
                    processor.id().equals(current) ? "*" : " ",
                    processor.id(),
                    processor.pinCount()));
            return 0;
        }
    }

    @Command(name = "select", description = "Select a processor and reset its configuration.")
    static final class SelectCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PROCESSOR") String processorId;
        @Option(names = "--discard", description = "Discard unsaved changes.") boolean discard;

        @Override
        public Integer call() {
            parent.service.selectProcessor(processorId, discard);
            parent.output.println("Selected " + parent.service.currentProcessor().id() + ".");
            return 0;
        }
    }

    @Command(name = "pins", description = "List pins, functions and current assignments.")
    static final class PinsCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Option(names = {"-f", "--filter"}, description = "Filter pin names or functions.") String filter;

        @Override
        public Integer call() {
            String needle = filter == null ? "" : filter.toUpperCase();
            parent.service.currentProcessor().pins().stream()
                    .filter(pin -> needle.isEmpty()
                            || pin.name().contains(needle)
                            || pin.availableFunctions().stream().anyMatch(value -> value.contains(needle)))
                    .forEach(pin -> parent.output.printf(
                            "%3d %-7s %-16s %s%n",
                            pin.physicalNumber(),
                            pin.name(),
                            parent.service.currentConfiguration().assignmentFor(pin.name())
                                    .map(assignment -> assignment.function()).orElse("-"),
                            String.join(", ", pin.availableFunctions())));
            return 0;
        }
    }

    @Command(name = "peripherals", description = "List peripheral and clock state.")
    static final class PeripheralsCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            parent.service.currentProcessor().peripherals().forEach(peripheral -> {
                var state = parent.service.currentConfiguration().peripheralConfigurations().get(peripheral.id());
                parent.output.printf("%-12s %-13s enabled=%-5s clock=%-5s%n",
                        peripheral.id(), peripheral.category(), state.enabled(), state.clockEnabled());
            });
            return 0;
        }
    }

    @Command(name = "enable", description = "Enable a peripheral.")
    static final class EnableCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PERIPHERAL") String peripheralId;

        @Override
        public Integer call() {
            parent.service.setPeripheralEnabled(peripheralId, true);
            parent.output.println("Enabled " + peripheralId + ".");
            return 0;
        }
    }

    @Command(name = "disable", description = "Disable a peripheral without silently clearing assignments.")
    static final class DisableCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PERIPHERAL") String peripheralId;

        @Override
        public Integer call() {
            parent.service.setPeripheralEnabled(peripheralId, false);
            parent.output.println("Disabled " + peripheralId + ".");
            return 0;
        }
    }

    @Command(name = "clock", description = "Enable or disable a peripheral clock.")
    static final class ClockCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PERIPHERAL") String peripheralId;
        @Parameters(index = "1", paramLabel = "on|off") SwitchValue value;

        @Override
        public Integer call() {
            parent.service.setClockEnabled(peripheralId, value == SwitchValue.on);
            parent.output.println("Clock for " + peripheralId + " is " + value + ".");
            return 0;
        }
    }

    enum SwitchValue { on, off }

    @Command(name = "assign", description = "Assign a supported function to a pin.")
    static final class AssignCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PIN") String pinName;
        @Parameters(index = "1", paramLabel = "FUNCTION") String function;

        @Override
        public Integer call() {
            parent.service.assignPin(pinName, function);
            parent.output.println("Assigned " + function + " to " + pinName + ".");
            return 0;
        }
    }

    @Command(name = "clear", description = "Clear a pin assignment.")
    static final class ClearCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PIN") String pinName;

        @Override
        public Integer call() {
            parent.service.clearPin(pinName);
            parent.output.println("Cleared " + pinName + ".");
            return 0;
        }
    }

    @Command(name = "power", description = "Set RUN or LOW_POWER mode.")
    static final class PowerCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "MODE") PowerMode mode;

        @Override
        public Integer call() {
            parent.service.setPowerMode(mode);
            parent.output.println("Power mode is " + mode + ".");
            return 0;
        }
    }

    @Command(name = "validate", description = "Validate the current configuration.")
    static final class ValidateCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            var result = parent.service.validate();
            if (result.issues().isEmpty()) {
                parent.output.println("VALID: no errors or warnings.");
            } else {
                result.issues().forEach(issue -> printIssue(parent.output, issue));
                parent.output.printf("Result: %d error(s), %d warning(s).%n",
                        result.errorCount(), result.warningCount());
            }
            return result.isValid() ? 0 : 3;
        }
    }

    @Command(name = "summary", description = "Show the current configuration summary.")
    static final class SummaryCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            var summary = parent.service.summary();
            parent.output.println("Processor:          " + summary.processorId());
            parent.output.println("Physical pins:      " + summary.pinCount());
            parent.output.println("Assigned pins:      " + summary.assignedPins());
            parent.output.println("Enabled peripherals:" + " " + summary.enabledPeripherals());
            parent.output.println("Enabled clocks:     " + summary.enabledClocks());
            parent.output.println("Power mode:         " + summary.powerMode());
            parent.output.printf("Validation:         %d error(s), %d warning(s)%n",
                    summary.validation().errorCount(), summary.validation().warningCount());
            parent.output.println("Unsaved changes:    " + summary.dirty());
            return 0;
        }
    }

    @Command(name = "export", description = "Validate and export the current configuration.")
    static final class ExportCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PATH") Path target;
        @Option(names = "--format", defaultValue = "json", description = "Export format (default: ${DEFAULT-VALUE}).")
        String format;

        @Override
        public Integer call() {
            parent.service.export(format, target);
            parent.output.println("Exported " + format + " configuration to " + target.toAbsolutePath() + ".");
            return 0;
        }
    }

    @Command(name = "import", description = "Import a saved configuration into the current session.")
    static final class ImportCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PATH") Path source;
        @Option(names = "--format", defaultValue = "json",
                description = "Import format (default: ${DEFAULT-VALUE}).")
        String format;
        @Option(names = "--discard", description = "Discard unsaved changes.") boolean discard;

        @Override
        public Integer call() {
            parent.service.importConfiguration(format, source, discard);
            var validation = parent.service.validate();
            parent.output.printf("Imported %s with %d error(s) and %d warning(s).%n",
                    source.toAbsolutePath(), validation.errorCount(), validation.warningCount());
            if (!validation.issues().isEmpty()) {
                parent.output.println("Run 'validate' to review corrective actions.");
            }
            return 0;
        }
    }

    @Command(name = "undo", description = "Restore the previous configuration snapshot.")
    static final class UndoCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            parent.service.undo();
            parent.output.println("Undo complete.");
            return 0;
        }
    }

    @Command(name = "redo", description = "Reapply the most recently undone snapshot.")
    static final class RedoCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            parent.service.redo();
            parent.output.println("Redo complete.");
            return 0;
        }
    }

    @Command(name = "auto-assign", description = "Assign all unassigned signals of one peripheral.")
    static final class AutoAssignCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PERIPHERAL") String peripheralId;
        @Option(names = "--enable",
                description = "Enable the peripheral and its clock in the same undoable operation.")
        boolean enable;

        @Override
        public Integer call() {
            var assignments = parent.service.autoAssignPeripheral(peripheralId, enable);
            if (assignments.isEmpty()) {
                parent.output.println("No assignment changes were required for " + peripheralId + ".");
            } else {
                assignments.forEach(assignment -> parent.output.printf(
                        "Assigned %-18s to %s.%n", assignment.function(), assignment.pinName()));
            }
            if (enable) {
                parent.output.println("Enabled " + peripheralId + " and its clock.");
            }
            return 0;
        }
    }

    @Command(name = "dependencies", description = "Visualize peripheral, clock and pin dependencies.")
    static final class DependenciesCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            var processor = parent.service.currentProcessor();
            var configuration = parent.service.currentConfiguration();
            processor.peripherals().forEach(peripheral -> {
                var state = configuration.peripheralConfigurations().get(peripheral.id());
                String pins = configuration.pinAssignments().stream()
                        .filter(assignment -> processor.resolveSignal(assignment.function())
                                .map(signal -> signal.peripheralId().equals(peripheral.id()))
                                .orElse(false))
                        .map(assignment -> assignment.function() + "@" + assignment.pinName())
                        .sorted()
                        .collect(java.util.stream.Collectors.joining(", "));
                parent.output.printf("%s -> clock=%s -> %s%n",
                        peripheral.id(),
                        state.clockEnabled() ? "ON" : "OFF",
                        pins.isBlank() ? "no assigned pins" : pins);
            });
            return 0;
        }
    }

    @Command(name = "diff", description = "Compare the current state with a saved configuration.")
    static final class DiffCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "PATH") Path source;
        @Option(names = "--format", defaultValue = "json",
                description = "Compared format (default: ${DEFAULT-VALUE}).")
        String format;

        @Override
        public Integer call() {
            var differences = parent.service.diff(format, source);
            if (differences.isEmpty()) {
                parent.output.println("IDENTICAL: no configuration differences.");
            } else {
                differences.forEach(difference -> parent.output.printf(
                        "%-28s current=%s compared=%s%n",
                        difference.field(), difference.currentValue(), difference.comparedValue()));
            }
            return 0;
        }
    }

    @Command(name = "templates", description = "List saved configuration templates.")
    static final class TemplatesCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            var names = parent.service.templateNames();
            if (names.isEmpty()) {
                parent.output.println("No configuration templates are saved.");
            } else {
                names.forEach(parent.output::println);
            }
            return 0;
        }
    }

    @Command(name = "save-template", description = "Save the current state as a reusable template.")
    static final class SaveTemplateCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "NAME") String name;

        @Override
        public Integer call() {
            parent.service.saveTemplate(name);
            parent.output.println("Saved template " + name + ".");
            return 0;
        }
    }

    @Command(name = "apply-template", description = "Apply a saved configuration template.")
    static final class ApplyTemplateCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;
        @Parameters(index = "0", paramLabel = "NAME") String name;
        @Option(names = "--discard", description = "Discard unsaved changes.") boolean discard;

        @Override
        public Integer call() {
            parent.service.applyTemplate(name, discard);
            parent.output.println("Applied template " + name + ".");
            return 0;
        }
    }

    @Command(name = "exit", description = "Close the interactive CLI session.")
    static final class ExitCommand implements Callable<Integer> {
        @ParentCommand CliApplication parent;

        @Override
        public Integer call() {
            parent.exitRequested = true;
            return 0;
        }
    }

    private static void printIssue(PrintWriter output, ValidationIssue issue) {
        output.printf("[%s] %s at %s: %s%n  Fix: %s%n",
                issue.severity(), issue.code(), issue.location(), issue.message(), issue.suggestion());
    }

    static String[] splitCommandLine(String command) {
        List<String> arguments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quoteCharacter = 0;
        for (int index = 0; index < command.length(); index++) {
            char character = command.charAt(index);
            if ((character == '\"' || character == '\'') && (!quoted || quoteCharacter == character)) {
                if (quoted) {
                    quoted = false;
                } else {
                    quoted = true;
                    quoteCharacter = character;
                }
            } else if (Character.isWhitespace(character) && !quoted) {
                if (!current.isEmpty()) {
                    arguments.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quote in command: " + command);
        }
        if (!current.isEmpty()) {
            arguments.add(current.toString());
        }
        return arguments.toArray(String[]::new);
    }

    static List<String> splitScript(String script) {
        List<String> commands = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quoteCharacter = 0;
        for (int index = 0; index < script.length(); index++) {
            char character = script.charAt(index);
            if ((character == '\"' || character == '\'') && (!quoted || quoteCharacter == character)) {
                quoted = !quoted;
                quoteCharacter = quoted ? character : 0;
                current.append(character);
            } else if (character == ';' && !quoted) {
                if (!current.toString().isBlank()) {
                    commands.add(current.toString().trim());
                }
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Unclosed quote in script");
        }
        if (!current.toString().isBlank()) {
            commands.add(current.toString().trim());
        }
        return List.copyOf(commands);
    }
}
