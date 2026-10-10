package com.example.configtool.infrastructure.export;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.PinAssignment;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;

class XmlConfigurationExporterTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void exportsParseableConfigurationWithSameDomainState() throws Exception {
        var processor = new JsonProcessorRepository(null).findById("ProcessorA").orElseThrow();
        Configuration configuration = Configuration.emptyFor(processor)
                .withPeripheralEnabled("LPUART0", true)
                .withClockEnabled("LPUART0", true)
                .withAssignment(new PinAssignment("PTA2", "LPUART0_TX"));
        Path target = temporaryDirectory.resolve("configuration.xml");

        new XmlConfigurationExporter().export(processor, configuration, target);

        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(target.toFile());
        var root = document.getDocumentElement();
        assertEquals("configuration", root.getTagName());
        assertEquals("1.0", root.getAttribute("schemaVersion"));
        assertEquals("ProcessorA", root.getAttribute("processorId"));
        assertEquals("RUN", root.getAttribute("powerMode"));
        var assignment = document.getElementsByTagName("assignment").item(0).getAttributes();
        assertEquals("PTA2", assignment.getNamedItem("pinName").getNodeValue());
        assertEquals("LPUART0", assignment.getNamedItem("peripheralId").getNodeValue());
    }
}
