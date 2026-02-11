package org.unifor.dto.response;

/**
 * Current student profile for GET /api/student/me.
 * course is null when the student has no course assigned.
 */
public record StudentMeResponse(CourseDto course) {}
