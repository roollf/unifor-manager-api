package org.unifor.service;

import org.junit.jupiter.api.Test;
import org.unifor.entity.TimeSlot;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for schedule conflict algorithm (PRD 7.2).
 */
class ScheduleConflictUtilTest {

    private static TimeSlot slot(String day, String start, String end) {
        return new TimeSlot(day, LocalTime.parse(start), LocalTime.parse(end));
    }

    @Test
    void overlaps_sameDayOverlapping_returnsTrue() {
        var a = slot("MON", "08:00", "10:00");
        var b = slot("MON", "09:00", "11:00");
        assertTrue(ScheduleConflictUtil.overlaps(a, b));
        assertTrue(ScheduleConflictUtil.overlaps(b, a));
    }

    @Test
    void overlaps_sameDayAdjacent_returnsFalse() {
        var a = slot("MON", "08:00", "10:00");
        var b = slot("MON", "10:00", "12:00");
        assertFalse(ScheduleConflictUtil.overlaps(a, b));
        assertFalse(ScheduleConflictUtil.overlaps(b, a));
    }

    @Test
    void overlaps_differentDays_returnsFalse() {
        var a = slot("MON", "08:00", "10:00");
        var b = slot("TUE", "08:00", "10:00");
        assertFalse(ScheduleConflictUtil.overlaps(a, b));
    }

    @Test
    void overlaps_sameDayNonOverlapping_returnsFalse() {
        var a = slot("MON", "08:00", "10:00");
        var b = slot("MON", "14:00", "16:00");
        assertFalse(ScheduleConflictUtil.overlaps(a, b));
    }

    @Test
    void overlaps_contained_returnsTrue() {
        var outer = slot("MON", "07:00", "12:00");
        var inner = slot("MON", "08:00", "10:00");
        assertTrue(ScheduleConflictUtil.overlaps(outer, inner));
        assertTrue(ScheduleConflictUtil.overlaps(inner, outer));
    }
}
