package com.example.configtool.application.port;

import java.io.IOException;
import java.nio.file.Path;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

/**
 * Output boundary used by the application layer to serialize a validated configuration.
 * Implementations belong to the infrastructure layer, which keeps file-format details out of
 * {@code ConfigurationService}.
 */
public interface ConfigurationExporter {

    /** @return the stable, case-insensitive format identifier, such as {@code json} */
    String formatId();

    /** @return the file extension normally used by this exporter, without a leading dot */
    String fileExtension();

    /**
     * Writes the selected processor and its configuration to the target path.
     *
     * @param processor static hardware definition associated with the configuration
     * @param configuration user-selected configuration state
     * @param target destination file
     * @throws IOException if the serialized output cannot be written
     */
    void export(ProcessorDefinition processor, Configuration configuration, Path target) throws IOException;
}
