package com.example.configtool.bootstrap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.example.configtool.application.service.ConfigurationService;
import com.example.configtool.domain.validation.ValidationEngine;
import com.example.configtool.domain.validation.rules.DefaultValidationRules;
import com.example.configtool.infrastructure.export.JsonConfigurationExporter;
import com.example.configtool.infrastructure.export.XmlConfigurationExporter;
import com.example.configtool.infrastructure.importer.JsonConfigurationImporter;
import com.example.configtool.infrastructure.persistence.JsonConfigurationTemplateRepository;
import com.example.configtool.infrastructure.persistence.JsonProcessorRepository;
import com.example.configtool.infrastructure.persistence.JsonValidationRuleRepository;

public final class ApplicationBootstrap {

    public static final String PROCESSOR_DIRECTORY_PROPERTY = "configtool.processorDir";
    public static final String PROCESSOR_DIRECTORY_ENVIRONMENT = "CONFIGTOOL_PROCESSOR_DIR";
    public static final String VALIDATION_DIRECTORY_PROPERTY = "configtool.validationDir";
    public static final String VALIDATION_DIRECTORY_ENVIRONMENT = "CONFIGTOOL_VALIDATION_DIR";
    public static final String TEMPLATE_DIRECTORY_PROPERTY = "configtool.templateDir";
    public static final String TEMPLATE_DIRECTORY_ENVIRONMENT = "CONFIGTOOL_TEMPLATE_DIR";

    private ApplicationBootstrap() {
    }

    public static ConfigurationService createService() {
        return createService(resolveExternalProcessorDirectory());
    }

    public static ConfigurationService createService(Path externalProcessorDirectory) {
        Path validationDirectory = resolveDirectory(
                VALIDATION_DIRECTORY_PROPERTY, VALIDATION_DIRECTORY_ENVIRONMENT);
        Path templateDirectory = resolveDirectory(
                TEMPLATE_DIRECTORY_PROPERTY, TEMPLATE_DIRECTORY_ENVIRONMENT);
        if (templateDirectory == null) {
            templateDirectory = Path.of(
                    System.getProperty("user.home"),
                    ".nxp-mini-configuration-tool",
                    "templates");
        }
        var rules = new ArrayList<>(DefaultValidationRules.create());
        rules.addAll(new JsonValidationRuleRepository(validationDirectory).findAll());
        return new ConfigurationService(
                new JsonProcessorRepository(externalProcessorDirectory),
                new ValidationEngine(rules),
                List.of(new JsonConfigurationExporter(), new XmlConfigurationExporter()),
                List.of(new JsonConfigurationImporter()),
                new JsonConfigurationTemplateRepository(templateDirectory));
    }

    static Path resolveExternalProcessorDirectory() {
        return resolveDirectory(PROCESSOR_DIRECTORY_PROPERTY, PROCESSOR_DIRECTORY_ENVIRONMENT);
    }

    private static Path resolveDirectory(String property, String environment) {
        String configured = System.getProperty(property);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(environment);
        }
        return configured == null || configured.isBlank() ? null : Path.of(configured);
    }
}
