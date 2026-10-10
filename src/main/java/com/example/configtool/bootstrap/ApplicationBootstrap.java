package com.example.configtool.bootstrap;

import java.nio.file.Path;
import java.util.List;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.export.JsonConfigurationExporter;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;

public final class ApplicationBootstrap {

    public static final String PROCESSOR_DIRECTORY_PROPERTY = "configtool.processorDir";
    public static final String PROCESSOR_DIRECTORY_ENVIRONMENT = "CONFIGTOOL_PROCESSOR_DIR";

    private ApplicationBootstrap() {
    }

    public static ConfigurationService createService() {
        return createService(resolveExternalProcessorDirectory());
    }

    public static ConfigurationService createService(Path externalProcessorDirectory) {
        return new ConfigurationService(
                new JsonProcessorRepository(externalProcessorDirectory),
                new ValidationEngine(DefaultValidationRules.create()),
                List.of(new JsonConfigurationExporter()));
    }

    static Path resolveExternalProcessorDirectory() {
        String configured = System.getProperty(PROCESSOR_DIRECTORY_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(PROCESSOR_DIRECTORY_ENVIRONMENT);
        }
        return configured == null || configured.isBlank() ? null : Path.of(configured);
    }
}
