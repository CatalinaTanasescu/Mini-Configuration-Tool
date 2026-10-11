package com.example.configtool.application.service;

/** Signals that a destructive session replacement requires explicit user consent. */
public final class UnsavedChangesException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    /** @param message explanation of the guarded operation */
    public UnsavedChangesException(String message) {
        super(message);
    }
}
