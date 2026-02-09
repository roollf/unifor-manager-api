package org.unifor.dto.response;

import java.time.Instant;

/**
 * Response for enrollment (list enrolled and create response).
 */
public record EnrollmentResponse(
        Long id,
        Long matrixClassId,
        SubjectDto subject,
        ProfessorDto professor,
        TimeSlotDto timeSlot,
        Instant enrolledAt
) {}
