package org.unifor.dto.response;

import java.time.LocalTime;

public record TimeSlotDto(Long id, String dayOfWeek, LocalTime startTime, LocalTime endTime) {}
