package com.example.configtool.application.port;

import java.io.IOException;
import java.nio.file.Path;

import com.example.configtool.domain.model.Configuration;

/**
 * Input boundary for loading configuration snapshots without coupling the application layer to a
 * particular serialization library or file format.
 */
public interface ConfigurationImporter {

    /** @return the stable, case-insensitive format identifier handled by this importer */
    String formatId();

    /**
     * Parses a configuration snapshot. Processor-specific consistency is checked later by the
     * application service because an importer does not own the hardware catalog.
     *
     * @param source file to read
     * @return parsed immutable configuration
     * @throws IOException if the file cannot be read or does not match the format
     */
    Configuration importConfiguration(Path source) throws IOException;
}
