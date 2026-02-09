package org.unifor.dto.response;

import java.time.Instant;

public record MatrixSummaryResponse(
        Long id,
        String name,
        boolean active,
        long classCount,
        Instant createdAt
) {}
