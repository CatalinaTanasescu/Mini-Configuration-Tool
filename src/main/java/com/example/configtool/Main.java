package com.example.configtool;

import java.util.Arrays;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.example.configtool.bootstrap.ApplicationBootstrap;
import com.example.configtool.presentation.cli.CliApplication;
import com.example.configtool.presentation.gui.MainWindow;
import com.example.configtool.presentation.gui.theme.UiTheme;

import picocli.CommandLine;

/**
 * Process entry point that selects either the Swing or command-line presentation adapter.
 * Both modes receive a service from {@link ApplicationBootstrap}, so mode selection does not fork
 * business behavior.
 */
public final class Main {

    private Main() {
    }

    /**
     * @param args first argument selects {@code gui} or {@code cli}; remaining arguments are passed
     *        to the CLI adapter
     */
    public static void main(String[] args) {
        String mode = args.length == 0 ? "gui" : args[0].toLowerCase();
        String[] adapterArguments = args.length == 0 ? new String[0] : Arrays.copyOfRange(args, 1, args.length);
        try {
            var service = ApplicationBootstrap.createService();
            if ("cli".equals(mode)) {
                int exitCode = new CommandLine(new CliApplication(service)).execute(adapterArguments);
                System.exit(exitCode);
            }
            if (!"gui".equals(mode)) {
                System.err.println("Unknown mode: " + mode);
                System.err.println("Usage: java -jar mini-configuration-tool-0.0.1-SNAPSHOT-all.jar [gui|cli]");
                System.exit(2);
            }
            UiTheme.install();
            SwingUtilities.invokeLater(() -> new MainWindow(service).setVisible(true));
        } catch (RuntimeException exception) {
            if ("gui".equals(mode)) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null,
                        "Application startup failed:\n" + exception.getMessage(),
                        "NXP Mini Configuration Tool",
                        JOptionPane.ERROR_MESSAGE));
            } else {
                System.err.println("ERROR: " + exception.getMessage());
                System.exit(2);
            }
        }
    }
}
