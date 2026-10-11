package com.example.configtool.application.service;

/** Base exception for user-correctable application workflow failures. */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message actionable explanation suitable for presentation to the user
     */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * @param message actionable explanation suitable for presentation to the user
     * @param cause lower-level failure that triggered the workflow error
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
