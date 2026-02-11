package org.unifor.dto.response;

import java.time.LocalTime;

/** Time slot for API responses; code is display code per PRD Appendix C (e.g. M24AB). */
public record TimeSlotDto(Long id, String dayOfWeek, LocalTime startTime, LocalTime endTime, String code) {}
