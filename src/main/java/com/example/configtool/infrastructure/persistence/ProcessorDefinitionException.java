package com.example.configtool.infrastructure.persistence;

public final class ProcessorDefinitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ProcessorDefinitionException(String message) {
        super(message);
    }

    public ProcessorDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
