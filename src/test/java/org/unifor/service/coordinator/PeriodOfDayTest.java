package org.unifor.service.coordinator;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for period-of-day filter (PRD Appendix A).
 */
class PeriodOfDayTest {

    @Test
    void morning_containsStartBoundary() {
        assertTrue(PeriodOfDay.MORNING.contains(LocalTime.of(6, 0)));
    }

    @Test
    void morning_containsBeforeEnd() {
        assertTrue(PeriodOfDay.MORNING.contains(LocalTime.of(11, 59)));
    }

    @Test
    void morning_excludesEndBoundary() {
        assertFalse(PeriodOfDay.MORNING.contains(LocalTime.of(12, 0)));
    }

    @Test
    void morning_excludesBeforeStart() {
        assertFalse(PeriodOfDay.MORNING.contains(LocalTime.of(5, 59)));
    }

    @Test
    void afternoon_containsStartBoundary() {
        assertTrue(PeriodOfDay.AFTERNOON.contains(LocalTime.of(12, 0)));
    }

    @Test
    void afternoon_containsMidRange() {
        assertTrue(PeriodOfDay.AFTERNOON.contains(LocalTime.of(15, 0)));
    }

    @Test
    void afternoon_excludesEndBoundary() {
        assertFalse(PeriodOfDay.AFTERNOON.contains(LocalTime.of(18, 0)));
    }

    @Test
    void evening_containsStartAndLate() {
        assertTrue(PeriodOfDay.EVENING.contains(LocalTime.of(18, 0)));
        assertTrue(PeriodOfDay.EVENING.contains(LocalTime.of(23, 59)));
    }
}
