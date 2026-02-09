package org.unifor.dto.response;

/**
 * Response for available classes (classes student can enroll in).
 */
public record AvailableClassResponse(
        Long id,
        SubjectDto subject,
        ProfessorDto professor,
        TimeSlotDto timeSlot,
        Integer maxStudents,
        int availableSeats,
        boolean authorizedForStudentCourse
) {}
