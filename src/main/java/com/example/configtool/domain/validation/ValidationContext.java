package com.example.configtool.domain.validation;

import java.util.Objects;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

public record ValidationContext(ProcessorDefinition processor, Configuration configuration) {

    public ValidationContext {
        processor = Objects.requireNonNull(processor, "Processor cannot be null");
        configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    }
}
