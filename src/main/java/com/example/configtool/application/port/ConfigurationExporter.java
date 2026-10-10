package com.example.configtool.application.port;

import java.io.IOException;
import java.nio.file.Path;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

public interface ConfigurationExporter {

    String formatId();

    String fileExtension();

    void export(ProcessorDefinition processor, Configuration configuration, Path target) throws IOException;
}
