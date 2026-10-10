package com.example.configtool.application.service;

import com.example.configtool.domain.validation.ValidationResult;

public final class ExportBlockedException extends ConfigurationException {

    private static final long serialVersionUID = 1L;
    private final ValidationResult validationResult;

    public ExportBlockedException(ValidationResult validationResult) {
        super("Export is blocked by " + validationResult.errorCount() + " validation error(s)");
        this.validationResult = validationResult;
    }

    public ValidationResult validationResult() {
        return validationResult;
    }
}
