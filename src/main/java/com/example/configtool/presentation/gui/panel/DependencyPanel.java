package com.example.configtool.presentation.gui.panel;

import java.awt.BorderLayout;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.presentation.gui.theme.UiTheme;

/** Read-only Swing view of peripheral, clock, signal, and assigned-pin relationships. */
public final class DependencyPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private final ConfigurationService service;
    private final JTree tree = new JTree();

    /** @param service source of the current processor and configuration snapshot */
    public DependencyPanel(ConfigurationService service) {
        super(new BorderLayout(0, 14));
        this.service = service;
        setBackground(UiTheme.CANVAS);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 28, 28));
        add(PanelSupport.header(
                "Peripheral dependency visualization",
                "Expand the processor tree to inspect peripheral, clock, signal and pin relationships."),
                BorderLayout.NORTH);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setRowHeight(25);
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
        add(scrollPane, BorderLayout.CENTER);
    }

    /** Rebuilds the tree from the current service snapshot. */
    public void refresh() {
        var processor = service.currentProcessor();
        var configuration = service.currentConfiguration();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                processor.id() + "  |  " + configuration.powerMode());
        processor.peripherals().forEach(peripheral -> {
            var state = configuration.peripheralConfigurations().get(peripheral.id());
            DefaultMutableTreeNode peripheralNode = new DefaultMutableTreeNode(
                    peripheral.id() + "  [" + peripheral.category() + "]  "
                            + (state.enabled() ? "ENABLED" : "disabled"));
            peripheralNode.add(new DefaultMutableTreeNode(
                    "Clock -> " + (state.clockEnabled() ? "ON" : "OFF")));
            peripheral.signals().forEach(signal -> {
                String pins = configuration.pinAssignments().stream()
                        .filter(assignment -> assignment.function().equals(signal))
                        .map(assignment -> assignment.pinName())
                        .sorted()
                        .collect(Collectors.joining(", "));
                peripheralNode.add(new DefaultMutableTreeNode(
                        signal + " -> " + (pins.isBlank() ? "unassigned" : pins)
                                + (peripheral.isSignalExclusive(signal) ? "  [exclusive]" : "")));
            });
            root.add(peripheralNode);
        });
        processor.independentSignals().stream().sorted().forEach(signal ->
                root.add(new DefaultMutableTreeNode(signal + "  [independent]")));
        tree.setModel(new DefaultTreeModel(root));
        for (int row = 0; row < Math.min(tree.getRowCount(), 12); row++) {
            tree.expandRow(row);
        }
    }
}
