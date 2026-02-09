package org.unifor.dto.response;

import java.util.List;

/**
 * Generic wrapper for paginated list responses.
 */
public record PageResponse<T>(List<T> items, long total) {}
