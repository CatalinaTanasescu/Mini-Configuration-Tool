package com.example.configtool.domain.validation;

import java.util.Objects;

import com.example.configtool.domain.model.Configuration;
import com.example.configtool.domain.model.ProcessorDefinition;

/**
 * Complete immutable input supplied to every validation rule.
 *
 * @param processor static capabilities used to interpret the selected state
 * @param configuration selected state being validated
 */
public record ValidationContext(ProcessorDefinition processor, Configuration configuration) {

    /** Validates that both halves of the validation input are present. */
    public ValidationContext {
        processor = Objects.requireNonNull(processor, "Processor cannot be null");
        configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
    }
}
