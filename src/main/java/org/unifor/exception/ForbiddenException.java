package org.unifor.exception;

import java.util.Map;

public class ForbiddenException extends UniforException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }

    public ForbiddenException(String message, Map<String, Object> details) {
        super("FORBIDDEN", message, details);
    }
}
