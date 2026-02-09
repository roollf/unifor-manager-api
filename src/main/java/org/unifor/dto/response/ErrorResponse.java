package org.unifor.dto.response;

import java.util.Map;

/**
 * Standard error response format per PRD Section 4.4.
 */
public record ErrorResponse(String code, String message, Map<String, Object> details) {

    public ErrorResponse(String code, String message) {
        this(code, message, null);
    }
}
