package com.example.configtool.infrastructure.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.example.configtool.application.port.ConfigurationExporter;
import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

public final class XmlConfigurationExporter implements ConfigurationExporter {

    public static final String FORMAT_ID = "xml";
    private static final String SCHEMA_VERSION = "1.0";

    @Override
    public String formatId() {
        return FORMAT_ID;
    }

    @Override
    public String fileExtension() {
        return "xml";
    }

    @Override
    public void export(ProcessorDefinition processor, Configuration configuration, Path target)
            throws IOException {
        Path absoluteTarget = target.toAbsolutePath().normalize();
        Path parent = absoluteTarget.getParent();
        if (parent == null) {
            throw new IOException("Export target must have a parent directory");
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absoluteTarget.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            writeDocument(processor, configuration, temporary);
            try {
                Files.move(temporary, absoluteTarget,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static void writeDocument(
            ProcessorDefinition processor,
            Configuration configuration,
            Path target) throws IOException {
        try (OutputStream output = Files.newOutputStream(target)) {
            XMLStreamWriter xml = XMLOutputFactory.newFactory()
                    .createXMLStreamWriter(output, "UTF-8");
            try {
                xml.writeStartDocument("UTF-8", "1.0");
                xml.writeStartElement("configuration");
                xml.writeAttribute("schemaVersion", SCHEMA_VERSION);
                xml.writeAttribute("processorId", configuration.processorId());
                xml.writeAttribute("powerMode", configuration.powerMode().name());

                xml.writeStartElement("peripherals");
                for (var peripheral : processor.peripherals()) {
                    var state = configuration.peripheralConfigurations().get(peripheral.id());
                    xml.writeEmptyElement("peripheral");
                    xml.writeAttribute("id", peripheral.id());
                    xml.writeAttribute("category", peripheral.category().name());
                    xml.writeAttribute("enabled", Boolean.toString(state.enabled()));
                    xml.writeAttribute("clockEnabled", Boolean.toString(state.clockEnabled()));
                }
                xml.writeEndElement();

                xml.writeStartElement("pinAssignments");
                for (var assignment : configuration.pinAssignments().stream()
                        .sorted((left, right) -> Integer.compare(
                                processor.findPin(left.pinName()).orElseThrow().physicalNumber(),
                                processor.findPin(right.pinName()).orElseThrow().physicalNumber()))
                        .toList()) {
                    xml.writeEmptyElement("assignment");
                    xml.writeAttribute("physicalPinNumber", Integer.toString(
                            processor.findPin(assignment.pinName()).orElseThrow().physicalNumber()));
                    xml.writeAttribute("pinName", assignment.pinName());
                    xml.writeAttribute("function", assignment.function());
                    processor.resolveSignal(assignment.function())
                            .ifPresent(signal -> writeAttribute(xml, "peripheralId", signal.peripheralId()));
                }
                xml.writeEndElement();
                xml.writeEndElement();
                xml.writeEndDocument();
                xml.flush();
            } finally {
                xml.close();
            }
        } catch (XMLStreamException exception) {
            throw new IOException("Cannot generate XML configuration", exception);
        }
    }

    private static void writeAttribute(XMLStreamWriter xml, String name, String value) {
        try {
            xml.writeAttribute(name, value);
        } catch (XMLStreamException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
