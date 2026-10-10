package com.example.configtool.application.port;

import java.io.IOException;
import java.nio.file.Path;

import com.example.configtool.domain.model.Configuration;

public interface ConfigurationImporter {

    String formatId();

    Configuration importConfiguration(Path source) throws IOException;
}
