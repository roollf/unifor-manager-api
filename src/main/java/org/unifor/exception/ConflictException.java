package org.unifor.exception;

import java.util.Map;

public class ConflictException extends UniforException {

    public ConflictException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(String errorCode, String message, Map<String, Object> details) {
        super(errorCode, message, details);
    }
}
