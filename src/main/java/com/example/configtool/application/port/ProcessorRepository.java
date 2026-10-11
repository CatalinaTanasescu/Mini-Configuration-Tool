package com.example.configtool.application.port;

import java.util.List;
import java.util.Optional;

import com.example.configtool.domain.model.ProcessorDefinition;

/**
 * Read-only application boundary for discovering hardware definitions.
 * Processor definitions may come from bundled resources, external JSON, or another adapter.
 */
public interface ProcessorRepository {

    /** @return all available processor definitions in deterministic display order */
    List<ProcessorDefinition> findAll();

    /**
     * @param processorId stable processor identifier
     * @return matching definition, or an empty optional when it is unknown
     */
    Optional<ProcessorDefinition> findById(String processorId);
}
