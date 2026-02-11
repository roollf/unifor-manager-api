# PRD Compliance Report

**Document:** PRD.md (Academic Course Registration System)  
**Date:** 2025-02-09  
**Scope:** Phases 1–6

---

## Phase 1: Core Entities and Repositories

**Status:** Fully compliant

**Issues:** None

**Risk:** None

**Recommendation:** None

---

## Phase 2: Coordinator Features (CRUD Matrix)

**Status:** Partially compliant

**Issues:**
- `timeRangeStart` and `timeRangeEnd` query params (PRD Section 4.2) are not implemented; only `periodOfDay`, `authorizedCourseId`, `maxStudentsMin`, `maxStudentsMax` exist.
- `includeDeleted` query param is accepted but has no effect; `MatrixClass` uses `@SQLRestriction("deleted_at IS NULL")` so soft-deleted classes are never returned.

**Risk:** Users cannot filter classes by a custom time range; coordinators cannot list or audit soft-deleted classes.

**Recommendation:**
- Add `timeRangeStart` and `timeRangeEnd` (ISO time) to `MatrixClassFilter` and `MatrixClassResource`; filter by TimeSlot `start_time` and `end_time` within the range.
- Either remove `@SQLRestriction` and filter by `deleted_at` in the repository when `includeDeleted=false`, or add a repository method that bypasses the restriction when `includeDeleted=true`; ensure `list()` applies the chosen behavior.

---

## Phase 3: Student Features (Enrollment)

**Status:** Fully compliant

**Issues:** None

**Risk:** None

**Recommendation:** None

**Note (EN-01 — student course assignment):** PRD §3.7a now explicitly defines: (1) single source of truth for the student's course (User.course_id, linked by email per AC-05); (2) how the student's course is set and updated (seed + optional admin/IdP/PATCH); (3) use of User.course_id for GET /api/student/classes/available (filter and authorizedForStudentCourse) and POST /api/student/enrollments (409 CONFLICT_UNAUTHORIZED_COURSE when not authorized); (4) "no course" behavior (empty available list, 409 on enroll with CONFLICT_UNAUTHORIZED_COURSE). Implementation already follows this (EnrollmentService uses User.course; seed sets course_id for students). If new students are created without course_id outside the seed, implement and document an explicit way to set/update the student's course (e.g. PATCH /api/student/profile or admin endpoint) per §3.7a.

---

## Phase 4: Security and Access Control

**Status:** Fully compliant

**Issues:** None

**Risk:** None

**Recommendation:** None

---

## Phase 5: Concurrency Handling

**Status:** Fully compliant

**Issues:** None

**Risk:** None

**Recommendation:** None

---

## Phase 6: Testing

**Status:** Fully compliant

**Issues:** None

**Risk:** None

**Recommendation:** None

---

## Overall Summary

| Phase | Status         | Issues |
|-------|----------------|--------|
| 1     | Compliant      | 0      |
| 2     | Partial        | 2      |
| 3     | Compliant      | 0      |
| 4     | Compliant      | 0      |
| 5     | Compliant      | 0      |
| 6     | Compliant      | 0      |

**Total gaps:** 2 (both in Phase 2)

---

## Time slot rules (Appendix C)

**Status:** Implemented

**Changes:** Time slots follow Unifor rules: turn (M/T/N), days (246/35), block (AB/CD/EF), code format e.g. M24AB. Migration V6 adds `time_slots.code`, replaces seed with 40 slots. API exposes `code` in TimeSlotDto. Period-of-day filter (MORNING/AFTERNOON/EVENING) unchanged; M/T/N align with those ranges.
