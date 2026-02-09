package org.unifor.service;

import org.unifor.entity.TimeSlot;

/**
 * Pure utility for schedule conflict detection (PRD 7.2).
 * Two time slots overlap if: same day AND [start1, end1) overlaps [start2, end2).
 * Overlap: start1 &lt; end2 AND start2 &lt; end1.
 */
public final class ScheduleConflictUtil {

    private ScheduleConflictUtil() {
    }

    public static boolean overlaps(TimeSlot a, TimeSlot b) {
        if (!a.dayOfWeek.equals(b.dayOfWeek)) return false;
        return a.startTime.isBefore(b.endTime) && b.startTime.isBefore(a.endTime);
    }
}
