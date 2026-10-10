package com.example.configtool.application.port;

import java.util.List;
import java.util.Optional;

import com.example.configtool.domain.model.ProcessorDefinition;

public interface ProcessorRepository {

    List<ProcessorDefinition> findAll();

    Optional<ProcessorDefinition> findById(String processorId);
}
