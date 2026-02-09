package org.unifor.service.coordinator;

import java.time.LocalTime;

/**
 * Period of day for filtering matrix classes (PRD Appendix A).
 */
public enum PeriodOfDay {
    MORNING(LocalTime.of(6, 0), LocalTime.of(12, 0)),
    AFTERNOON(LocalTime.of(12, 0), LocalTime.of(18, 0)),
    EVENING(LocalTime.of(18, 0), LocalTime.MAX);

    private final LocalTime start;
    private final LocalTime end;

    PeriodOfDay(LocalTime start, LocalTime end) {
        this.start = start;
        this.end = end;
    }

    public boolean contains(LocalTime time) {
        return !time.isBefore(start) && (end == LocalTime.MAX || time.isBefore(end));
    }
}
