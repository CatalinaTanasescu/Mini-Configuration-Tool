package com.example.configtool.infrastructure.persistence;

/** Reports malformed, duplicate, or unreadable processor definition data. */
public final class ProcessorDefinitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message source-specific definition diagnostic */
    public ProcessorDefinitionException(String message) {
        super(message);
    }

    /**
     * @param message source-specific definition diagnostic
     * @param cause parsing or I/O failure
     */
    public ProcessorDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
