package com.example.configtool.infrastructure.persistence;

public final class ValidationRuleDefinitionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ValidationRuleDefinitionException(String message) {
        super(message);
    }

    public ValidationRuleDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
