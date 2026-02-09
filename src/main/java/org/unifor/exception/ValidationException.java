package org.unifor.exception;

import java.util.Map;

public class ValidationException extends UniforException {

    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Map<String, Object> details) {
        super("VALIDATION_ERROR", message, details);
    }
}
