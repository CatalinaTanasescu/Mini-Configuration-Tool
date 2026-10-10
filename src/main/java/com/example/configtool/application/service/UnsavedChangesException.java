package com.example.configtool.application.service;

public final class UnsavedChangesException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    public UnsavedChangesException(String message) {
        super(message);
    }
}
