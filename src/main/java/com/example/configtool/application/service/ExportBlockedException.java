package com.example.configtool.application.service;

import com.example.configtool.domain.validation.ValidationResult;

/** Indicates that export was refused because the configuration has validation errors. */
public final class ExportBlockedException extends ConfigurationException {

    private static final long serialVersionUID = 1L;
    private final ValidationResult validationResult;

    /** @param validationResult result containing the errors that blocked export */
    public ExportBlockedException(ValidationResult validationResult) {
        super("Export is blocked by " + validationResult.errorCount() + " validation error(s)");
        this.validationResult = validationResult;
    }

    /** @return validation result that explains why export was blocked */
    public ValidationResult validationResult() {
        return validationResult;
    }
}
