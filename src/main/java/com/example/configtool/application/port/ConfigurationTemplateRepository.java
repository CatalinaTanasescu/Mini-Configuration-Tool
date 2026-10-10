package com.example.configtool.application.port;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

public interface ConfigurationTemplateRepository {

    List<String> findAll() throws IOException;

    Optional<Configuration> findByName(String name) throws IOException;

    void save(String name, ProcessorDefinition processor, Configuration configuration) throws IOException;
}
