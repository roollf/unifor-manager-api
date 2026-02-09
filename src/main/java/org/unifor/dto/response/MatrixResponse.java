package org.unifor.dto.response;

import java.time.Instant;

public record MatrixResponse(
        Long id,
        String name,
        Long coordinatorId,
        boolean active,
        Instant createdAt
) {}
