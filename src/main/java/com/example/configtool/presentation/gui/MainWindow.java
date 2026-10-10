package com.example.configtool.presentation.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.application.service.ExportBlockedException;
import com.example.configtool.domain.model.PowerMode;
import com.example.configtool.domain.validation.ValidationIssue;
import com.example.configtool.presentation.gui.panel.OverviewPanel;
import com.example.configtool.presentation.gui.panel.DependencyPanel;
import com.example.configtool.presentation.gui.panel.PeripheralConfigurationPanel;
import com.example.configtool.presentation.gui.panel.PinConfigurationPanel;
import com.example.configtool.presentation.gui.panel.SummaryPanel;
import com.example.configtool.presentation.gui.panel.ValidationPanel;
import com.example.configtool.presentation.gui.theme.UiTheme;

public final class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final String[] NAVIGATION = {
            "Overview", "Pin Configuration", "Peripherals & Clocks", "Validation",
            "Dependencies", "Configuration Summary"
    };

    private final ConfigurationService service;
    private final JComboBox<String> processorSelector = new JComboBox<>();
    private final JComboBox<PowerMode> powerModeSelector = new JComboBox<>(PowerMode.values());
    private final JLabel statusLabel = new JLabel("Ready", SwingConstants.CENTER);
    private final CardLayout cards = new CardLayout();
    private final JPanel workspace = new JPanel(cards);
    private final JList<String> navigation = new JList<>(NAVIGATION);
    private final OverviewPanel overviewPanel;
    private final PinConfigurationPanel pinPanel;
    private final PeripheralConfigurationPanel peripheralPanel;
    private final ValidationPanel validationPanel;
    private final DependencyPanel dependencyPanel;
    private final SummaryPanel summaryPanel;
    private boolean updating;

    public MainWindow(ConfigurationService service) {
        super("NXP Mini Configuration Tool");
        this.service = service;
        this.overviewPanel = new OverviewPanel(service);
        this.pinPanel = new PinConfigurationPanel(service, this::refreshAll, this::showError);
        this.peripheralPanel = new PeripheralConfigurationPanel(service, this::refreshAll, this::showError);
        this.validationPanel = new ValidationPanel(service, this::navigateToIssue);
        this.dependencyPanel = new DependencyPanel(service);
        this.summaryPanel = new SummaryPanel(service);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 700));
        setSize(1280, 820);
        setLocationByPlatform(true);
        buildInterface();
        setJMenuBar(createMenuBar());
        installShortcuts();
        installCloseGuard();
        refreshAll();
    }

    private void buildInterface() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.CANVAS);
        root.add(createToolbar(), BorderLayout.NORTH);
        root.add(createNavigation(), BorderLayout.WEST);

        workspace.setBackground(UiTheme.CANVAS);
        workspace.add(overviewPanel, NAVIGATION[0]);
        workspace.add(pinPanel, NAVIGATION[1]);
        workspace.add(peripheralPanel, NAVIGATION[2]);
        workspace.add(validationPanel, NAVIGATION[3]);
        workspace.add(dependencyPanel, NAVIGATION[4]);
        workspace.add(summaryPanel, NAVIGATION[5]);
        root.add(workspace, BorderLayout.CENTER);
        root.add(createStatusBar(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem importItem = new JMenuItem("Import JSON...");
        importItem.addActionListener(event -> importConfiguration());
        JMenuItem exportJson = new JMenuItem("Export JSON...");
        exportJson.addActionListener(event -> exportConfiguration("json"));
        JMenuItem exportXml = new JMenuItem("Export XML...");
        exportXml.addActionListener(event -> exportConfiguration("xml"));
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(event -> dispatchEvent(
                new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        file.add(importItem);
        file.addSeparator();
        file.add(exportJson);
        file.add(exportXml);
        file.addSeparator();
        file.add(exit);

        JMenu edit = new JMenu("Edit");
        JMenuItem undo = new JMenuItem("Undo");
        undo.addActionListener(event -> undo());
        JMenuItem redo = new JMenuItem("Redo");
        redo.addActionListener(event -> redo());
        edit.add(undo);
        edit.add(redo);

        JMenu tools = new JMenu("Tools");
        JMenuItem autoAssign = new JMenuItem("Auto-assign selected peripheral");
        autoAssign.addActionListener(event -> autoAssignSelectedPeripheral());
        JMenuItem saveTemplate = new JMenuItem("Save configuration template...");
        saveTemplate.addActionListener(event -> saveTemplate());
        JMenuItem applyTemplate = new JMenuItem("Apply configuration template...");
        applyTemplate.addActionListener(event -> applyTemplate());
        JMenuItem diff = new JMenuItem("Compare with JSON...");
        diff.addActionListener(event -> compareConfiguration());
        tools.add(autoAssign);
        tools.addSeparator();
        tools.add(saveTemplate);
        tools.add(applyTemplate);
        tools.addSeparator();
        tools.add(diff);

        JMenu view = new JMenu("View");
        JMenuItem dependencies = new JMenuItem("Peripheral dependencies");
        dependencies.addActionListener(event -> navigation.setSelectedIndex(4));
        JMenuItem theme = new JMenuItem(UiTheme.isDark() ? "Use light theme" : "Use dark theme");
        theme.addActionListener(event -> toggleTheme());
        view.add(dependencies);
        view.add(theme);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(tools);
        menuBar.add(view);
        return menuBar;
    }

    private JComponent createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(24, 0));
        toolbar.setBackground(UiTheme.SURFACE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(13, 18, 13, 18)));

        JPanel identity = new JPanel();
        identity.setOpaque(false);
        identity.setLayout(new javax.swing.BoxLayout(identity, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("NXP Mini Configuration Tool");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(UiTheme.TEXT);
        JLabel subtitle = new JLabel("Embedded configuration workspace");
        subtitle.setFont(subtitle.getFont().deriveFont(11f));
        subtitle.setForeground(UiTheme.MUTED_TEXT);
        identity.add(title);
        identity.add(subtitle);
        toolbar.add(identity, BorderLayout.WEST);

        JPanel selectors = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        selectors.setOpaque(false);
        selectors.add(new JLabel("Processor"));
        processorSelector.setPreferredSize(new Dimension(155, 34));
        service.availableProcessors().forEach(processor -> processorSelector.addItem(processor.id()));
        processorSelector.addActionListener(event -> changeProcessor());
        selectors.add(processorSelector);
        selectors.add(javax.swing.Box.createHorizontalStrut(8));
        selectors.add(new JLabel("Power mode"));
        powerModeSelector.setPreferredSize(new Dimension(130, 34));
        powerModeSelector.addActionListener(event -> changePowerMode());
        selectors.add(powerModeSelector);
        toolbar.add(selectors, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton validate = new JButton("Validate");
        validate.addActionListener(event -> showValidation());
        JButton export = new JButton("Export");
        export.setBackground(UiTheme.ACCENT);
        export.setForeground(Color.WHITE);
        export.putClientProperty("JButton.buttonType", "roundRect");
        export.addActionListener(event -> exportConfiguration());
        actions.add(validate);
        actions.add(export);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    private JComponent createNavigation() {
        navigation.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navigation.setSelectedIndex(0);
        navigation.setFixedCellHeight(48);
        navigation.setBackground(UiTheme.NAVIGATION);
        navigation.setForeground(UiTheme.NAVIGATION_TEXT);
        navigation.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
        navigation.setCellRenderer(new NavigationRenderer());
        navigation.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && navigation.getSelectedValue() != null) {
                cards.show(workspace, navigation.getSelectedValue());
                refreshVisiblePanel();
            }
        });
        JPanel container = new JPanel(new BorderLayout());
        container.setPreferredSize(new Dimension(215, 0));
        container.setBackground(UiTheme.NAVIGATION);
        container.add(navigation, BorderLayout.CENTER);
        return container;
    }

    private JComponent createStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(UiTheme.SURFACE);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        JLabel hint = new JLabel(
                "F5 Validate   •   Ctrl+F Filter   •   Ctrl+O Import   •   Ctrl+S Export   •   Ctrl+Z/Y Undo/Redo");
        hint.setForeground(UiTheme.MUTED_TEXT);
        status.add(hint, BorderLayout.WEST);
        statusLabel.setOpaque(true);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        status.add(statusLabel, BorderLayout.EAST);
        return status;
    }

    private void changeProcessor() {
        if (updating || processorSelector.getSelectedItem() == null) {
            return;
        }
        String selected = processorSelector.getSelectedItem().toString();
        if (selected.equals(service.currentProcessor().id())) {
            return;
        }
        boolean discard = true;
        if (service.hasUnsavedChanges()) {
            discard = JOptionPane.showConfirmDialog(
                    this,
                    "Changing the processor resets the current configuration. Discard unsaved changes?",
                    "Confirm processor change",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
        if (discard) {
            service.selectProcessor(selected, true);
        }
        refreshAll();
    }

    private void changePowerMode() {
        if (updating || powerModeSelector.getSelectedItem() == null) {
            return;
        }
        try {
            service.setPowerMode((PowerMode) powerModeSelector.getSelectedItem());
            refreshAll();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void showValidation() {
        refreshAll();
        navigation.setSelectedIndex(3);
    }

    private void exportConfiguration() {
        String[] formats = service.exportFormats().stream().sorted().toArray(String[]::new);
        String format = (String) JOptionPane.showInputDialog(
                this,
                "Choose the export format:",
                "Export configuration",
                JOptionPane.QUESTION_MESSAGE,
                null,
                formats,
                formats.length == 0 ? null : formats[0]);
        if (format != null) {
            exportConfiguration(format);
        }
    }

    private void exportConfiguration(String format) {
        var validation = service.validate();
        if (!validation.isValid()) {
            navigation.setSelectedIndex(3);
            JOptionPane.showMessageDialog(
                    this,
                    "Export is blocked by " + validation.errorCount()
                            + " validation error(s). Open Validation for corrective actions.",
                    "Configuration is not exportable",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        String upperFormat = format.toUpperCase();
        chooser.setDialogTitle("Export configuration as " + upperFormat);
        chooser.setFileFilter(new FileNameExtensionFilter(
                upperFormat + " configuration (*." + format + ")", format));
        chooser.setSelectedFile(new java.io.File(
                service.currentProcessor().id().toLowerCase() + "-configuration." + format));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase().endsWith("." + format)) {
            target = target.resolveSibling(target.getFileName() + "." + format);
        }
        if (Files.exists(target) && JOptionPane.showConfirmDialog(
                this,
                "Replace existing file?\n" + target,
                "Confirm overwrite",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            service.export(format, target);
            refreshAll();
            JOptionPane.showMessageDialog(
                    this,
                    "Configuration exported successfully.\n" + target.toAbsolutePath(),
                    "Export complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (ExportBlockedException exception) {
            navigation.setSelectedIndex(3);
            showError(exception);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void importConfiguration() {
        boolean discard = true;
        if (service.hasUnsavedChanges()) {
            discard = JOptionPane.showConfirmDialog(
                    this,
                    "Importing replaces the current configuration. Discard unsaved changes?",
                    "Confirm import",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
        if (!discard) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import JSON configuration");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON configuration (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            service.importConfiguration("json", chooser.getSelectedFile().toPath(), true);
            refreshAll();
            JOptionPane.showMessageDialog(
                    this,
                    "Configuration imported successfully.",
                    "Import complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void undo() {
        try {
            service.undo();
            refreshAll();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void redo() {
        try {
            service.redo();
            refreshAll();
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void autoAssignSelectedPeripheral() {
        navigation.setSelectedIndex(2);
        String peripheralId = peripheralPanel.selectedPeripheralId();
        if (peripheralId == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Select a peripheral in Peripherals & Clocks first.",
                    "Auto-assignment",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            var assignments = service.autoAssignPeripheral(peripheralId);
            refreshAll();
            JOptionPane.showMessageDialog(
                    this,
                    assignments.isEmpty()
                            ? "All signals of " + peripheralId + " already have assignments."
                            : "Assigned " + assignments.size() + " signal(s) for " + peripheralId + ".",
                    "Auto-assignment complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void saveTemplate() {
        String name = JOptionPane.showInputDialog(
                this,
                "Template name (letters, digits, dots, underscores or hyphens):",
                "Save configuration template",
                JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        try {
            service.saveTemplate(name.trim());
            JOptionPane.showMessageDialog(
                    this,
                    "Template '" + name.trim() + "' was saved.",
                    "Template saved",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void applyTemplate() {
        try {
            List<String> templates = service.templateNames();
            if (templates.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "No configuration templates are saved.",
                        "Apply template",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String selected = (String) JOptionPane.showInputDialog(
                    this,
                    "Choose a template:",
                    "Apply configuration template",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    templates.toArray(String[]::new),
                    templates.getFirst());
            if (selected == null) {
                return;
            }
            boolean discard = !service.hasUnsavedChanges() || JOptionPane.showConfirmDialog(
                    this,
                    "Applying a template replaces the current state. Discard unsaved changes?",
                    "Confirm template",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
            if (discard) {
                service.applyTemplate(selected, true);
                refreshAll();
            }
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void compareConfiguration() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Compare with JSON configuration");
        chooser.setFileFilter(new FileNameExtensionFilter("JSON configuration (*.json)", "json"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            var differences = service.diff("json", chooser.getSelectedFile().toPath());
            JTextArea content = new JTextArea(18, 80);
            content.setEditable(false);
            content.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            if (differences.isEmpty()) {
                content.setText("IDENTICAL: no configuration differences.");
            } else {
                differences.forEach(difference -> content.append(String.format(
                        "%-30s current=%-24s compared=%s%n",
                        difference.field(), difference.currentValue(), difference.comparedValue())));
            }
            content.setCaretPosition(0);
            JOptionPane.showMessageDialog(
                    this,
                    new JScrollPane(content),
                    "Configuration diff",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException exception) {
            showError(exception);
        }
    }

    private void toggleTheme() {
        var bounds = getBounds();
        int selectedNavigation = navigation.getSelectedIndex();
        UiTheme.install(!UiTheme.isDark());
        MainWindow replacement = new MainWindow(service);
        replacement.setBounds(bounds);
        replacement.navigation.setSelectedIndex(selectedNavigation);
        replacement.setVisible(true);
        dispose();
    }

    private void navigateToIssue(ValidationIssue issue) {
        if (issue.pinName() != null) {
            navigation.setSelectedIndex(1);
            pinPanel.focusPin(issue.pinName());
        } else if (issue.peripheralId() != null) {
            navigation.setSelectedIndex(2);
            peripheralPanel.focusPeripheral(issue.peripheralId());
        }
    }

    private void refreshAll() {
        updating = true;
        try {
            processorSelector.setSelectedItem(service.currentProcessor().id());
            powerModeSelector.setSelectedItem(service.currentConfiguration().powerMode());
            overviewPanel.refresh();
            pinPanel.refresh();
            peripheralPanel.refresh();
            validationPanel.refresh();
            dependencyPanel.refresh();
            summaryPanel.refresh();
            updateStatus();
        } finally {
            updating = false;
        }
    }

    private void refreshVisiblePanel() {
        String selected = navigation.getSelectedValue();
        if (NAVIGATION[0].equals(selected)) overviewPanel.refresh();
        if (NAVIGATION[1].equals(selected)) pinPanel.refresh();
        if (NAVIGATION[2].equals(selected)) peripheralPanel.refresh();
        if (NAVIGATION[3].equals(selected)) validationPanel.refresh();
        if (NAVIGATION[4].equals(selected)) dependencyPanel.refresh();
        if (NAVIGATION[5].equals(selected)) summaryPanel.refresh();
    }

    private void updateStatus() {
        var result = service.validate();
        if (result.errorCount() > 0) {
            statusLabel.setText(result.errorCount() + " error(s) • Export blocked");
            statusLabel.setForeground(UiTheme.ERROR);
            statusLabel.setBackground(UiTheme.ERROR_BACKGROUND);
        } else if (result.warningCount() > 0) {
            statusLabel.setText(result.warningCount() + " warning(s) • Export ready");
            statusLabel.setForeground(UiTheme.WARNING);
            statusLabel.setBackground(UiTheme.WARNING_BACKGROUND);
        } else {
            statusLabel.setText("Valid • Export ready");
            statusLabel.setForeground(UiTheme.SUCCESS);
            statusLabel.setBackground(UiTheme.SUCCESS_BACKGROUND);
        }
    }

    private void installShortcuts() {
        var inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var actionMap = getRootPane().getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("F5"), "validate");
        inputMap.put(KeyStroke.getKeyStroke("control S"), "export");
        inputMap.put(KeyStroke.getKeyStroke("control O"), "import");
        inputMap.put(KeyStroke.getKeyStroke("control F"), "filterPins");
        inputMap.put(KeyStroke.getKeyStroke("control Z"), "undo");
        inputMap.put(KeyStroke.getKeyStroke("control Y"), "redo");
        actionMap.put("validate", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { showValidation(); }
        });
        actionMap.put("export", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { exportConfiguration(); }
        });
        actionMap.put("import", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { importConfiguration(); }
        });
        actionMap.put("filterPins", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) {
                navigation.setSelectedIndex(1);
                pinPanel.focusSearch();
            }
        });
        actionMap.put("undo", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { undo(); }
        });
        actionMap.put("redo", new AbstractAction() {
            private static final long serialVersionUID = 1L;
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { redo(); }
        });
    }

    private void installCloseGuard() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (!service.hasUnsavedChanges() || JOptionPane.showConfirmDialog(
                        MainWindow.this,
                        "Close without exporting the current changes?",
                        "Unsaved configuration",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    dispose();
                }
            }
        });
    }

    private void showError(RuntimeException exception) {
        JOptionPane.showMessageDialog(
                this,
                exception.getMessage(),
                "Configuration error",
                JOptionPane.ERROR_MESSAGE);
        refreshAll();
    }

    private static final class NavigationRenderer extends DefaultListCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
                JList<?> list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
            label.setFont(label.getFont().deriveFont(isSelected ? Font.BOLD : Font.PLAIN));
            label.setBackground(isSelected ? UiTheme.NAVIGATION_SELECTED : UiTheme.NAVIGATION);
            label.setForeground(isSelected ? Color.WHITE : UiTheme.NAVIGATION_TEXT);
            return label;
        }
    }
}
