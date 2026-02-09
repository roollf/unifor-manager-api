package org.unifor.exception;

import java.util.Map;

/**
 * Base exception for application errors. All custom exceptions extend this.
 */
public abstract class UniforException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    protected UniforException(String errorCode, String message) {
        this(errorCode, message, null);
    }

    protected UniforException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
