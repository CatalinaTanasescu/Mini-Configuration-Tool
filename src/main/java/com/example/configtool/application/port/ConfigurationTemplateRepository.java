package com.example.configtool.application.port;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

/**
 * Persistence boundary for named, reusable configuration snapshots.
 * The application service depends on this abstraction so storage can change independently of the
 * GUI, CLI, and domain model.
 */
public interface ConfigurationTemplateRepository {

    /** @return available template names in repository-defined order
     * @throws IOException if the repository cannot be read
     */
    List<String> findAll() throws IOException;

    /**
     * @param name template name
     * @return the stored configuration, or an empty optional when the name is unknown
     * @throws IOException if the repository cannot be read
     */
    Optional<Configuration> findByName(String name) throws IOException;

    /**
     * @param name stable name used to retrieve the template
     * @param processor processor definition associated with the snapshot
     * @param configuration snapshot to store
     * @throws IOException if the template cannot be persisted
     */
    void save(String name, ProcessorDefinition processor, Configuration configuration) throws IOException;
}
