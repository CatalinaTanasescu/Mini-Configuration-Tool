package com.example.configtool.infrastructure.persistence;

/** Reports an unreadable or semantically invalid declarative validation rule. */
public final class ValidationRuleDefinitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message file-specific rule diagnostic */
    public ValidationRuleDefinitionException(String message) {
        super(message);
    }

    /**
     * @param message file-specific rule diagnostic
     * @param cause parsing, reference-validation, or I/O failure
     */
    public ValidationRuleDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
