package org.unifor.service.coordinator;

import java.util.Optional;

/**
 * Filter criteria for listing matrix classes (PRD VM-02).
 */
public record MatrixClassFilter(
        Optional<PeriodOfDay> periodOfDay,
        Optional<Long> authorizedCourseId,
        Optional<Integer> maxStudentsMin,
        Optional<Integer> maxStudentsMax,
        boolean includeDeleted
) {
    public static MatrixClassFilter empty() {
        return new MatrixClassFilter(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false
        );
    }
}
