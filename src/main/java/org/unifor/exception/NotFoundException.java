package org.unifor.exception;

import java.util.Map;

public class NotFoundException extends UniforException {

    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    public NotFoundException(String message, Map<String, Object> details) {
        super("NOT_FOUND", message, details);
    }
}
