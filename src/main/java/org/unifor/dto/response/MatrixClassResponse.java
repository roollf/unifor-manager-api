package org.unifor.dto.response;

import java.time.Instant;
import java.util.List;

public record MatrixClassResponse(
        Long id,
        Long matrixId,
        SubjectDto subject,
        ProfessorDto professor,
        TimeSlotDto timeSlot,
        List<CourseDto> authorizedCourses,
        Integer maxStudents,
        long currentEnrollments,
        Instant deletedAt,
        Instant createdAt
) {}
