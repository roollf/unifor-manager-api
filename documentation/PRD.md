# Product Requirements Document (PRD)
## Academic Course Registration System

**Version:** 1.0  
**Date:** February 8, 2025  
**Stack:** Java 21 + Quarkus 3.20 + PostgreSQL + Keycloak

---

## 1. System Overview

### 1.1 Brief Description

The Academic Course Registration System is a web application that enables **coordinators** to create and manage curriculum matrices (sets of classes offered in a semester) and **students** to view and enroll in classes. The system enforces academic rules (seat limits, course restrictions, schedule conflicts) and role-based access control via Keycloak.

### 1.2 Key Actors

| Actor | Description |
|-------|-------------|
| **Coordinator** | Creates and manages curriculum matrices. Can only access matrices they created and associated enrollments. |
| **Student** | Views enrolled classes and enrolls in available classes. Can only access their own enrollment data. |

### 1.3 Main Flows

**Coordinator Flow:**
1. Authenticate via Keycloak
2. Create a curriculum matrix (if not exists) and add classes
3. For each class: select subject, professor, time slot, authorized courses, max students
4. List/search classes with filters
5. Edit or soft-delete classes (when no enrollments exist)

**Student Flow:**
1. Authenticate via Keycloak
2. View list of enrolled classes
3. Browse available classes (filtered by course, availability)
4. Enroll in a class (validates course authorization, seat availability, schedule conflict)
5. System guarantees transactional seat allocation and conflict prevention

---

## 2. Domain Model

### 2.1 Reference Data Entities (Seeded, Read-Only for Application Logic)

| Entity | Description | Min. Records |
|--------|-------------|--------------|
| Subject | Academic subject/discipline | 15 |
| Professor | Instructor | 5 |
| TimeSlot | Day of week + start/end time (+ optional code per Appendix C) | 40 |
| Course | Academic course/program | 9 |
| User | System user with role (Coordinator or Student) | 5 students + 3 coordinators |

### 2.2 User Entity (Single Table with Role)

Users are stored in a single `users` table with a `role` enum (COORDINATOR, STUDENT). This approach:
- Simplifies schema: one place for authentication mapping (email)
- Both roles share common attributes: email, name
- Students require `course_id` (FK to courses) for enrollment authorization checks; coordinators have it null
- Application logic ensures `course_id` is set when `role = STUDENT`

### 2.3 New Entities (To Implement)

#### CurriculumMatrix
Represents the set of classes offered. Multiple matrices can exist; only one is active at a time.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | UUID/Long | Primary key |
| name | String | Display name for the matrix |
| coordinatorId | FK → User | Creator/owner (user with role=COORDINATOR) |
| active | Boolean | Whether this matrix is open for enrollment |
| deletedAt | Timestamp | Soft delete (nullable) |
| createdAt | Timestamp | Audit |
| updatedAt | Timestamp | Audit |

**Constraints:**
- One coordinator per matrix (ownership)
- At most one matrix can have `active = true` at a time

#### MatrixClass
A class within the curriculum matrix.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | UUID/Long | Primary key |
| matrixId | FK → CurriculumMatrix | Parent matrix |
| subjectId | FK → Subject | Pre-registered subject |
| professorId | FK → Professor | Pre-registered professor |
| timeSlotId | FK → TimeSlot | Pre-registered time slot |
| maxStudents | Integer | Maximum number of enrollments (fixed after creation) |
| deletedAt | Timestamp | Soft delete (nullable) |
| createdAt | Timestamp | Audit |
| updatedAt | Timestamp | Audit |

**Relationships:**
- Many-to-Many: MatrixClass ↔ Course (authorized courses)
- Subject is fixed after creation; maxStudents is not editable

#### Enrollment
Student enrollment in a class.

| Attribute | Type | Description |
|-----------|------|-------------|
| id | UUID/Long | Primary key |
| matrixClassId | FK → MatrixClass | The class |
| studentId | FK → User | The student (user with role=STUDENT) |
| enrolledAt | Timestamp | When enrollment occurred |
| createdAt | Timestamp | Audit |

**Unique constraint:** (matrixClassId, studentId) — student cannot enroll twice in same class.

### 2.4 Entity Relationships Diagram (Conceptual)

```
User (role=COORDINATOR) (1) ----< CurriculumMatrix (N)
                                        |
                                        +----< MatrixClass (N)
                                        |           |
                                        |           +---- Subject (N:1)
                                        |           +---- Professor (N:1)
                                        |           +---- TimeSlot (N:1)
                                        |           +----> Course (N:M, authorized_courses)
                                        |
                                        +----< Enrollment (N)
                                                    |
                                                    +---- MatrixClass (N:1)
                                                    +---- User (role=STUDENT) (N:1)
```

### 2.5 Important Constraints

- **Subject repetition:** Same subject can appear in multiple MatrixClasses if time slots differ.
- **Time slot validity:** Must reference existing TimeSlot.
- **Authorized courses:** Each MatrixClass has a list of courses allowed to enroll.
- **Seat limit:** `COUNT(Enrollment WHERE matrixClassId = X) <= MatrixClass.maxStudents`.
- **Schedule conflict:** Two enrollments for the same student cannot overlap in time (same day + overlapping start/end).
- **No duplicate subject enrollment:** A student cannot enroll in the same subject more than once, even in different time slots.

---

## 3. Business Rules (Grouped by Feature)

### 3.1 Authentication

| Rule ID | Description |
|---------|-------------|
| AUTH-01 | Login and logout must use Keycloak |
| AUTH-02 | Authorization must be handled via Keycloak (roles, claims) |

### 3.2 Access Control

| Rule ID | Description |
|---------|-------------|
| AC-01 | Coordinator can access only curriculum matrices they created |
| AC-02 | Coordinator cannot access student data unrelated to their matrices |
| AC-03 | Student can access only their own enrollment data |
| AC-04 | Student cannot view curriculum matrices or enrollments of other students |
| AC-05 | User ↔ Keycloak mapping via email (users table has email column) |
| AC-06 | Coordinator and Student are the same User entity with different roles |

### 3.3 Create Curriculum Matrix Class

| Rule ID | Description |
|---------|-------------|
| CR-01 | Subject, Professor, TimeSlot must reference existing entities |
| CR-02 | Time slot must be valid (exist in TimeSlot table) |
| CR-03 | Same subject may appear multiple times only if time slots differ |
| CR-04 | Authorized courses list must reference existing Course entities |
| CR-05 | maxStudents must be a positive integer |
| CR-06 | Matrix must exist and coordinator must own it |

### 3.4 Edit Curriculum Matrix Class

| Rule ID | Description |
|---------|-------------|
| ED-01 | Editable fields: time slot, professor, authorized courses |
| ED-02 | Subject and maxStudents are NOT editable |
| ED-03 | Edits must not cause inconsistencies (e.g., new time slot must be valid) |
| ED-04 | If removing a course from authorized list would invalidate existing enrollments, block the entire edit (cannot make any changes to authorized courses in that case) |
| ED-05 | If changing time slot would cause schedule conflict for any enrolled student, block the edit |
| ED-06 | Coordinator must own the matrix |

### 3.5 Delete Curriculum Matrix Class

| Rule ID | Description |
|---------|-------------|
| DL-01 | Deletion is not allowed if there are enrolled students |
| DL-02 | Deletion must be logical (soft delete: set deletedAt) |
| DL-03 | Coordinator must own the matrix |
| DL-04 | Soft-deleted classes must be excluded from listings and enrollment options |

### 3.6 View Enrolled Classes (Student)

| Rule ID | Description |
|---------|-------------|
| VE-01 | Student sees only their own enrollments |
| VE-02 | Display: Subject, Professor, Time slot for each enrolled class |
| VE-03 | Only active (non–soft-deleted) classes are shown in "View Enrolled Classes" |

### 3.7 Enroll in a Class (Student)

| Rule ID | Description |
|---------|-------------|
| EN-01 | Class must be authorized for the student's course |
| EN-02 | There must be available seats (current enrollments < maxStudents) |
| EN-03 | No schedule conflict with already enrolled classes (same day, overlapping start/end time) |
| EN-04 | Transactional: two students cannot occupy the same seat simultaneously (concurrency) |
| EN-05 | Enrollment must fail atomically if any rule is violated |
| EN-06 | Class must not be soft-deleted |
| EN-07 | Student cannot enroll twice in the same class |
| EN-08 | Student cannot enroll in the same subject more than once (even in different time slots) |

### 3.7a Student course assignment and course authorization (EN-01)

This section defines how the student's course is determined, how it is set and updated, and how it is used for course authorization. It also defines behavior when the student has no course assigned.

**Single source of truth for the student's course**

- The student's course is determined by the **User** entity (single table with role; see §2.2, §5.2). There is no separate Student entity.
- The user row linked to the authenticated identity **via email (AC-05)** has a **course_id** (FK to **courses**). For students, this is the student's course; for coordinators it is null.
- The API resolves the current user by email from Keycloak, then reads **User.course_id** (or the associated Course entity) for all course-authorization logic. This is the single source of truth.

**How the student's course is set and updated**

- **At creation / seed:** The seed script (e.g. Flyway V2) creates users with role STUDENT and sets **course_id** to a valid course. Keycloak must be configured with matching emails so that authenticated students have a corresponding User row with course_id.
- **When adding new students (outside seed):** The PRD does not mandate a single flow. Acceptable options include: (1) Admin sets **course_id** when creating or editing the user (admin-only endpoint or back-office); (2) Sync from IdP (e.g. Keycloak custom attribute) into **users.course_id**; (3) A dedicated endpoint to set/update the student's course (e.g. **PATCH /api/student/profile** with **courseId**, or admin **PATCH /api/admin/users/{id}**). If the system creates student users without **course_id**, an explicit way to set or update the student's course (one of the above) must be implemented and documented so that EN-01 can be satisfied in production.

**Use for GET /api/student/classes/available and POST /api/student/enrollments**

- **GET /api/student/classes/available:** The API uses the current student's **User.course_id** to filter and mark classes. Only classes in the active matrix that are authorized for that course (via matrix_class_authorized_courses), have available seats, and satisfy other rules (no schedule conflict, no duplicate subject, etc.) are returned. Each item includes **authorizedForStudentCourse** (true when the class is authorized for the student's course).
- **POST /api/student/enrollments:** EN-01 is enforced using **User.course_id**: if the class is not authorized for the student's course, the server returns **409 Conflict** with code **CONFLICT_UNAUTHORIZED_COURSE** (see §4.4).

**Handling "no course" (student has no course assigned)**

- If the authenticated student has **course_id** null:
  - **GET /api/student/classes/available:** Returns an empty list. No class is authorized for the student's course, so no items are returned (or all would have authorizedForStudentCourse: false if the API returned unfiltered list; current behavior is to return only authorized classes, so the list is empty).
  - **POST /api/student/enrollments:** Returns **409 Conflict** with code **CONFLICT_UNAUTHORIZED_COURSE**. The same code is used whether the student has no course or the class is not in the authorized list for the student's course. The message (e.g. "Turma não autorizada para o curso do estudante") should allow the frontend to show an appropriate message; if desired, the backend may use a distinct subcode or message for "student has no course" (e.g. **CONFLICT_STUDENT_NO_COURSE**) so the frontend can prompt the user to set their course.
- The backend does not block access to student endpoints when the student has no course; it returns the above responses so the frontend can show a suitable message (e.g. "Configure seu curso" or "Contacte o administrador para definir seu curso").

### 3.8 View Curriculum Matrix (Coordinator)

| Rule ID | Description |
|---------|-------------|
| VM-01 | Coordinator sees only classes in matrices they own |
| VM-02 | Filters: time range, period of day (Morning/Afternoon/Evening), authorized courses, max students |
| VM-03 | Soft-deleted classes excluded from default listing (or filterable) |

### 3.9 Concurrency and Data Consistency

| Rule ID | Description |
|---------|-------------|
| CC-01 | Seat allocation must be transactional (use DB locking or optimistic locking) |
| CC-02 | Enrollment transaction must be serializable or use SELECT FOR UPDATE to prevent overbooking |
| CC-03 | Schedule conflict check must run within the same transaction as enrollment |

---

## 4. API Endpoints Design

### 4.1 Authentication

| Method | Path | Role | Description |
|--------|------|------|-------------|
| N/A | Handled by Keycloak | All | Login/logout via Keycloak; tokens used for API calls |

### 4.2 Coordinator Endpoints

#### Create Curriculum Matrix (if matrix creation is explicit)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/coordinator/matrices | coordinator | Create a new curriculum matrix |

**Request Body:**
```json
{
  "name": "string"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "name": "string",
  "coordinatorId": "uuid",
  "active": true,
  "createdAt": "iso8601"
}
```

#### Create Matrix Class

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/coordinator/matrices/{matrixId}/classes | coordinator | Create a class in a matrix |

**Request Body:**
```json
{
  "subjectId": "uuid",
  "professorId": "uuid",
  "timeSlotId": "uuid",
  "authorizedCourseIds": ["uuid1", "uuid2"],
  "maxStudents": 30
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "matrixId": "uuid",
  "subjectId": "uuid",
  "professorId": "uuid",
  "timeSlotId": "uuid",
  "authorizedCourseIds": ["uuid1", "uuid2"],
  "maxStudents": 30,
  "currentEnrollments": 0,
  "createdAt": "iso8601"
}
```

**Errors:** 400 (validation), 403 (not owner), 404 (entity not found), 409 (subject+timeSlot duplicate)

#### List/Search Matrix Classes

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/coordinator/matrices/{matrixId}/classes | coordinator | List classes with filters |

**Query Params:**
- `timeRangeStart` (optional): ISO time
- `timeRangeEnd` (optional): ISO time
- `periodOfDay` (optional): MORNING | AFTERNOON | EVENING
- `authorizedCourseId` (optional): filter by authorized course
- `maxStudentsMin` (optional): minimum max students
- `maxStudentsMax` (optional): maximum max students
- `includeDeleted` (optional): boolean, default false

**Response 200:**
```json
{
  "items": [
    {
      "id": "uuid",
      "subject": { "id": "uuid", "name": "string" },
      "professor": { "id": "uuid", "name": "string" },
      "timeSlot": { "id": "uuid", "dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "10:00" },
      "authorizedCourses": [{ "id": "uuid", "name": "string" }],
      "maxStudents": 30,
      "currentEnrollments": 5,
      "deletedAt": null
    }
  ],
  "total": 10
}
```

**Errors:** 403 (not owner), 404 (matrix not found)

#### Update Matrix Class

| Method | Path | Role | Description |
|--------|------|------|-------------|
| PUT | /api/coordinator/matrices/{matrixId}/classes/{classId} | coordinator | Update class |

**Request Body:**
```json
{
  "timeSlotId": "uuid",
  "professorId": "uuid",
  "authorizedCourseIds": ["uuid1", "uuid2"]
}
```

**Response 200:** Same shape as create response

**Errors:** 400 (validation, invalidates enrollments), 403 (not owner), 404 (not found), 409 (inconsistency)

#### Delete Matrix Class (Soft Delete)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| DELETE | /api/coordinator/matrices/{matrixId}/classes/{classId} | coordinator | Soft delete class |

**Response 204:** No content

**Errors:** 403 (not owner), 404 (not found), 409 (has enrolled students)

#### List Coordinator's Matrices

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/coordinator/matrices | coordinator | List matrices owned by current coordinator |

**Response 200:**
```json
{
  "items": [
    {
      "id": "uuid",
      "name": "string",
      "active": true,
      "classCount": 15,
      "createdAt": "iso8601"
    }
  ]
}
```

#### Reference Data (Add Class Form Dropdowns)

Used by the frontend to populate the "Add class" form dropdowns and filter options, even when the matrix has no classes yet. IDs and shapes match create-class and list-classes.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/coordinator/reference/subjects | coordinator | List all subjects |
| GET | /api/coordinator/reference/professors | coordinator | List all professors |
| GET | /api/coordinator/reference/time-slots | coordinator | List all time slots |
| GET | /api/coordinator/reference/courses | coordinator | List all courses |

**Request:** None (GET, no body)

**Response 200:** Array of objects:

- **Subjects:** `[{ "id": number, "name": "string" }]`
- **Professors:** `[{ "id": number, "name": "string" }]`
- **Time slots:** `[{ "id": number, "dayOfWeek": "string", "startTime": "HH:mm", "endTime": "HH:mm", "code": "string" | null }]` (same shape as in list-classes; code e.g. M24AB per Appendix C)
- **Courses:** `[{ "id": number, "name": "string" }]`

**Errors:** 401 (unauthorized), 403 (not coordinator)

### 4.3 Student Endpoints

#### List Enrolled Classes

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/student/enrollments | student | List current student's enrollments (only active, non–soft-deleted classes) |

**Response 200:**
```json
{
  "items": [
    {
      "id": "uuid",
      "subject": { "id": "uuid", "name": "string" },
      "professor": { "id": "uuid", "name": "string" },
      "timeSlot": { "id": "uuid", "dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "10:00" },
      "enrolledAt": "iso8601"
    }
  ]
}
```

**Errors:** 403 (not student)

#### List Available Classes (for enrollment)

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | /api/student/classes/available | student | List classes student can enroll in (filtered by student's course, availability, etc.; see §3.7a) |

**Query Params:**
- `matrixId` (optional): filter by matrix
- `subjectId` (optional): filter by subject

**Response 200:**
```json
{
  "items": [
    {
      "id": "uuid",
      "subject": { "id": "uuid", "name": "string" },
      "professor": { "id": "uuid", "name": "string" },
      "timeSlot": { "id": "uuid", "dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "10:00" },
      "maxStudents": 30,
      "availableSeats": 5,
      "authorizedForStudentCourse": true
    }
  ]
}
```

**Note:** When the student has no course assigned (course_id null), the list is empty. See §3.7a.

**Errors:** 403 (not student)

#### Enroll in Class

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | /api/student/enrollments | student | Enroll in a class |

**Request Body:**
```json
{
  "matrixClassId": "uuid"
}
```

**Response 201:**
```json
{
  "id": "uuid",
  "matrixClassId": "uuid",
  "subject": { "id": "uuid", "name": "string" },
  "professor": { "id": "uuid", "name": "string" },
  "timeSlot": { "id": "uuid", "dayOfWeek": "MONDAY", "startTime": "08:00", "endTime": "10:00" },
  "enrolledAt": "iso8601"
}
```

**Errors:** 400 (validation), 403 (not student), 404 (class not found), 409 (no seats, schedule conflict, not authorized for course — code CONFLICT_UNAUTHORIZED_COURSE, including when student has no course; already enrolled; duplicate subject). See §3.7a for "no course" behavior.

### 4.4 Common Error Response Structure

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "details": {}
}
```

**Standard HTTP codes:** 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 409 Conflict, 500 Internal Server Error

---

## 5. Database Schema

All tables below must be implemented; none are pre-existing. Seed data is required for reference tables and users.

### 5.1 Single Users Table Design (Rationale)

Using one `users` table with a `role` enum (COORDINATOR, STUDENT) instead of separate `students` and `coordinators` tables:

- **Shared attributes:** Both roles need email (for Keycloak mapping) and name.
- **Role-specific attributes:** Only students need `course_id`; it is nullable (null for coordinators). Application logic enforces that `course_id` is set when `role = STUDENT`.
- **Simpler auth mapping:** Single lookup by email to resolve the user and their role.
- **Extensibility:** New roles can be added to the enum without new tables.

### 5.2 Reference Data Tables (To Implement + Seed)

**Migration order:** Create subjects, professors, time_slots, courses first (courses is referenced by users.course_id), then users.

#### users
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| email | VARCHAR(255) | NOT NULL, UNIQUE |
| name | VARCHAR(255) | NOT NULL |
| role | VARCHAR(20) | NOT NULL, CHECK (role IN ('COORDINATOR', 'STUDENT')) |
| course_id | BIGINT | FK → courses, NULL (required when role = STUDENT, enforced in app) |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Indexes:** email (unique), role, course_id

**Seed:** Minimum 5 users with role=STUDENT, 3 with role=COORDINATOR. Keycloak must be configured with matching emails.

#### subjects
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| name | VARCHAR(255) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Seed:** Minimum 15 subjects

#### professors
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| name | VARCHAR(255) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Seed:** Minimum 5 professors

#### time_slots
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| day_of_week | VARCHAR(15) | NOT NULL (e.g., SEG, TER, QUA, QUI, SEX) |
| start_time | TIME | NOT NULL |
| end_time | TIME | NOT NULL |
| code | VARCHAR(20) | NULL — display code (e.g. M24AB), see Appendix C |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Seed:** 40 time slots following the Unifor turn/days/block rules (Appendix C).

**Note:** Ensure `start_time` and `end_time` are comparable for schedule conflict detection. Each row is one (day, start, end); the optional `code` identifies the logical slot (e.g. M24AB = Manhã, seg/qua/sex, block A/B) for display.

#### courses
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| name | VARCHAR(255) | NOT NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Seed:** Minimum 9 courses (referenced by users.course_id for students)

### 5.3 Core Application Tables

#### curriculum_matrices
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| name | VARCHAR(255) | NOT NULL |
| coordinator_id | BIGINT | FK → users, NOT NULL (must be user with role=COORDINATOR) |
| active | BOOLEAN | DEFAULT false |
| deleted_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Indexes:** coordinator_id, active, deleted_at

**Constraint:** At most one row with `active = true` (enforced in application or via partial unique index)

#### matrix_classes
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| matrix_id | BIGINT | FK → curriculum_matrices, NOT NULL |
| subject_id | BIGINT | FK → subjects, NOT NULL |
| professor_id | BIGINT | FK → professors, NOT NULL |
| time_slot_id | BIGINT | FK → time_slots, NOT NULL |
| max_students | INTEGER | NOT NULL, CHECK (max_students > 0) |
| deleted_at | TIMESTAMPTZ | NULL |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| updated_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Unique:** (matrix_id, subject_id, time_slot_id) — same subject in same slot in same matrix not allowed  
**Indexes:** matrix_id, time_slot_id, deleted_at

#### matrix_class_authorized_courses (junction)
| Column | Type | Constraints |
|--------|------|-------------|
| matrix_class_id | BIGINT | FK → matrix_classes, PK |
| course_id | BIGINT | FK → courses, PK |

**Composite PK:** (matrix_class_id, course_id)

#### enrollments
| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGSERIAL | PK |
| matrix_class_id | BIGINT | FK → matrix_classes, NOT NULL |
| student_id | BIGINT | FK → users, NOT NULL (must be user with role=STUDENT) |
| enrolled_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |
| created_at | TIMESTAMPTZ | NOT NULL, DEFAULT now() |

**Unique:** (matrix_class_id, student_id)  
**Indexes:** student_id, matrix_class_id

### 5.4 Key Relationships

- User (role=COORDINATOR) → CurriculumMatrix (N:1 via coordinator_id)
- User (role=STUDENT) → Course (N:1 via course_id)
- CurriculumMatrix → MatrixClass (1:N)
- MatrixClass → Subject, Professor, TimeSlot (N:1 each)
- MatrixClass ↔ Course (N:M via matrix_class_authorized_courses)
- Enrollment → MatrixClass, User (N:1 each)

### 5.5 Indexes for Performance

- `users(email)` — unique, for Keycloak mapping
- `curriculum_matrices(coordinator_id)` — coordinator's matrices
- `curriculum_matrices(active)` WHERE active = true — find active matrix
- `matrix_classes(matrix_id, deleted_at)` — list classes in matrix
- `matrix_classes(time_slot_id)` — filter by time
- `enrollments(student_id)` — student's enrollments
- `enrollments(matrix_class_id)` — count enrollments per class (seat check)
- `time_slots(day_of_week, start_time, end_time)` — schedule conflict query

### 5.6 Soft Delete and Audit

- `deleted_at`: nullable; non-null = soft deleted
- `created_at`, `updated_at`: audit timestamps
- Queries must filter `WHERE deleted_at IS NULL` unless explicitly including deleted

---

## 6. Implementation Checkpoints

### Phase 1: Core Entities and Repositories

**Goal:** Set up persistence layer for all entities.

**Tasks:**
- Add Quarkus Hibernate ORM + PostgreSQL dependencies
- Create JPA entities: User, Subject, Professor, TimeSlot, Course, CurriculumMatrix, MatrixClass, Enrollment
- Create junction entity/table for MatrixClass ↔ Course (matrix_class_authorized_courses)
- Implement repositories (Panache or JpaRepository)
- Add Flyway/Liquibase migrations for all tables
- Seed script: users (5 students, 3 coordinators), subjects (15), professors (5), time_slots (40, see Appendix C), courses (9)
- Ensure Keycloak user emails match users.email for seeded users

**Dependencies:** None (foundation)

**Validation:**
- Application starts with DB connection
- Can insert/query all entities via repository tests or Dev UI

---

### Phase 2: Coordinator Features (CRUD Matrix)

**Goal:** Coordinator can create and manage curriculum matrices and classes.

**Tasks:**
- Create CurriculumMatrix (POST); ensure at most one active at a time when activating
- Create MatrixClass (POST) with validation (CR-01 to CR-06)
- List MatrixClasses with filters (VM-01, VM-02)
- Update MatrixClass (PUT) with validation (ED-01 to ED-05)
- Soft delete MatrixClass (DELETE) with validation (DL-01 to DL-04)
- Implement period-of-day filter logic (derive from TimeSlot start_time)
- Service layer for business rules

**Dependencies:** Phase 1

**Validation:**
- Integration tests: create class, list with filters, edit, soft delete
- Reject invalid inputs and edits that violate rules

---

### Phase 3: Student Features (Enrollment)

**Goal:** Student can view enrollments and enroll in classes.

**Tasks:**
- List enrolled classes (GET /api/student/enrollments)
- List available classes (GET /api/student/classes/available) — filter by student's course, availability
- Enroll in class (POST) with full validation:
  - EN-01: course authorization
  - EN-02: seat availability
  - EN-03: schedule conflict detection
  - EN-06, EN-07, EN-08: not deleted, not already enrolled, not same subject already enrolled
- Schedule conflict algorithm: compare day_of_week + overlapping [start, end) intervals

**Dependencies:** Phase 1, Phase 2

**Validation:**
- Unit tests for schedule conflict logic and duplicate-subject check
- Integration tests: enroll success, reject on conflict, reject on no seats, reject on unauthorized course, reject on duplicate subject
- Concurrent enrollment test: two students, one seat — only one succeeds

---

### Phase 4: Security and Access Control

**Goal:** Keycloak integration and role-based access.

**Tasks:**
- Add Quarkus OIDC (Keycloak) dependency
- Configure Keycloak realm, client, roles (coordinator, student)
- Map Keycloak user to User record via email (users.email)
- Secure endpoints: @RolesAllowed, resource-level checks
- Coordinator: verify matrix ownership (AC-01)
- Student: verify current user = student (AC-03)
- 401/403 error handling

**Dependencies:** Phase 2, Phase 3 (or parallelize with stub auth)

**Validation:**
- Coordinator cannot access another coordinator's matrix
- Student cannot see other students' enrollments
- Unauthenticated requests return 401

---

### Phase 5: Concurrency Handling

**Goal:** Prevent overbooking and ensure transactional integrity.

**Tasks:**
- Use `SELECT FOR UPDATE` or pessimistic lock on MatrixClass during enrollment
- Or: optimistic locking with version column on matrix_classes
- Ensure enrollment + seat check + conflict check in single transaction
- Retry logic for optimistic lock failures (optional)
- Document isolation level (e.g., READ COMMITTED with FOR UPDATE)

**Dependencies:** Phase 3

**Validation:**
- Load test: N students enroll in class with N-1 seats; exactly N-1 succeed
- No duplicate enrollments under concurrency

---

### Phase 6: Testing

**Goal:** Comprehensive test coverage.

**Tasks:**
- Unit tests: validation logic, schedule conflict algorithm, duplicate-subject check, filters
- Integration tests: all endpoints with Testcontainers PostgreSQL
- Concurrent enrollment tests
- Security tests: 403 for cross-actor access
- End-to-end tests (optional): REST Assured with Keycloak test container

**Dependencies:** Phases 1–5

**Validation:**
- All tests pass
- Coverage report acceptable

---

## 7. Critical Technical Challenges

### 7.1 Concurrency Control for Seat Availability

**Challenge:** Two students enroll in the last seat simultaneously; both may pass the availability check before either commits.

**Approaches:**
1. **Pessimistic locking:** `SELECT matrix_class FOR UPDATE` before checking seats and inserting enrollment.
2. **Optimistic locking:** Version column on matrix_classes; retry on conflict.
3. **Database constraint:** CHECK constraint or trigger that enforces `(SELECT COUNT(*) FROM enrollments WHERE matrix_class_id = X) <= (SELECT max_students FROM matrix_classes WHERE id = X)` — difficult to express; better to use locking.
4. **Serializable isolation:** Use `SET TRANSACTION ISOLATION LEVEL SERIALIZABLE` for enrollment transaction — may increase contention.

**Recommendation:** Pessimistic lock (`FOR UPDATE`) on the MatrixClass row during enrollment.

---

### 7.2 Schedule Conflict Detection Algorithm

**Challenge:** Efficiently determine if a new enrollment overlaps with existing ones for the same student.

**Logic:**
- Two time slots overlap if: same `day_of_week` AND `[start1, end1)` overlaps `[start2, end2)`.
- Overlap: `start1 < end2 AND start2 < end1`.

**Implementation:**
- Load student's enrollments with their TimeSlots.
- For each, compare day and time range with the candidate class's TimeSlot.
- Perform check inside enrollment transaction to avoid TOCTOU.

**Consideration:** TimeSlot format — ensure `start_time` and `end_time` are comparable (e.g., TIME or minutes-since-midnight).

### 7.2b Duplicate Subject Enrollment Check

**Requirement:** Student cannot enroll in the same subject more than once, even in different time slots.

**Implementation:** Before inserting enrollment, query existing enrollments for the student, join to matrix_classes and subjects. Reject if the candidate class's subject_id appears in any existing enrollment. Run within the same transaction as the enrollment.

---

### 7.3 Soft Delete Implications

**Challenges:**
- All list endpoints must filter `WHERE deleted_at IS NULL` unless explicitly including deleted.
- Edit/Delete: must not allow editing a soft-deleted class; delete must be idempotent or return 404.
- "View Enrolled Classes" shows only active (non–soft-deleted) classes.
- Cascade: soft-deleting a matrix — what happens to its classes? (See Section 8.4.)

---

### 7.4 Access Control Complexity

**Challenges:**
- Mapping Keycloak user to User: lookup by email (users.email). Keycloak user's email must match.
- If mapping fails (user has role but no DB record): return 403.
- Resource-level checks: every coordinator endpoint must verify `matrix.coordinatorId == currentUser.id`.
- Consider a custom annotation or interceptor for "matrix ownership" checks.

---

### 7.5 Period of Day Filter

**Challenge:** TimeSlots have day + start/end time; filter uses Morning/Afternoon/Evening.

**Approach:** Define ranges (e.g., Morning: 06:00–12:00, Afternoon: 12:00–18:00, Evening: 18:00–24:00). Filter TimeSlots where `start_time` (or `end_time`) falls within the range. Document ranges as configuration or constants.

---

## 8. Resolved Questions and Remaining Open Items

### 8.1 Resolved Requirements

| Topic | Decision |
|-------|----------|
| **Single matrix vs. multiple** | Multiple matrices can exist; only one active at a time. Students enroll in classes from the active matrix. |
| **maxStudents editability** | Not editable after creation. |
| **Subject editability** | Fixed after creation. |
| **Soft-deleted class in "View Enrolled Classes"** | Only active (non–soft-deleted) classes are shown. |
| **Unenroll/Cancel enrollment** | Not in scope; no drop/unenroll feature. |

### 8.2 Confirmed Assumptions

| Assumption | Value |
|------------|-------|
| **User ↔ Keycloak mapping** | Via email (users.email) |
| **TimeSlot structure** | `day_of_week`, `start_time`, `end_time`, optional `code` (Appendix C); period of day derived from time ranges (M/T/N ↔ MORNING/AFTERNOON/EVENING) |
| **Active matrix** | At most one matrix with `active = true`; students enroll only in classes from active matrix |
| **Database** | All tables must be implemented; seed data required for users, subjects, professors, time_slots, courses |

### 8.3 Resolved Edge Cases

| Edge Case | Decision |
|-----------|----------|
| **Coordinator removes course from authorized list** | If any enrolled student belongs to that course, block the entire edit (no changes to authorized courses allowed). |
| **Time slot change on edit** | Block the edit if the new time slot would cause a schedule conflict for any enrolled student. |
| **Multiple enrollments per student in same subject** | Not allowed. A student cannot enroll in the same subject more than once, even in different time slots. |

### 8.4 Remaining Open Questions

1. **Matrix soft delete:** Is there a soft delete for CurriculumMatrix? If so, should it cascade to its classes? (TBD)

---

## Appendix A: Time Slot Period of Day Ranges (Proposed)

| Period | Start | End | Turn (Appendix C) |
|--------|-------|-----|-------------------|
| Morning | 06:00 | 12:00 | M (Manhã) |
| Afternoon | 12:00 | 18:00 | T (Tarde) |
| Evening | 18:00 | 24:00 | N (Noite) |

A TimeSlot matches a period if its `start_time` falls within the range (or use overlap logic). The `periodOfDay` filter (MORNING | AFTERNOON | EVENING) aligns with turn codes M, T, N in Appendix C.

---

## Appendix B: Keycloak Roles (Proposed)

| Role | Description |
|------|-------------|
| coordinator | Can manage curriculum matrices |
| student | Can view enrollments and enroll in classes |

Realm roles or client roles as per Keycloak setup.

---

## Appendix C: Time slot rules (Unifor — turn, days, block, code)

Time slots follow a fixed structure: **turn** (M/T/N) + **days** (246 or 35) + **block** (AB, CD, or EF). Each block spans two 50-minute periods.

**Turn codes (TURNO):**
| Code | Name |
|------|------|
| M | Manhã |
| T | Tarde |
| N | Noite |

**Days codes:**
| Code | Days |
|------|------|
| 246 | Segunda, quarta e sexta (SEG, QUA, SEX) |
| 35 | Terça e quinta (TER, QUI) |

**Block times (start–end for the combined block):**

| Turn | Block | Time range |
|------|-------|------------|
| Manhã | A/B | 7h30–9h10 (7h30–8h20 + 8h20–9h10) |
| Manhã | C/D | 9h30–11h10 (9h30–10h20 + 10h20–11h10) |
| Manhã | E/F | 11h20–13h (11h20–12h10 + 12h10–13h) |
| Tarde | A/B | 13h30–15h10 |
| Tarde | C/D | 15h30–17h10 |
| Tarde | E/F | 17h20–19h |
| Noite | A/B | 19h–20h40 |
| Noite | C/D | 21h–22h40 |
| Noite | E/F | — (no slot) |

**Code format:** `{turn}{days}{block}` — e.g. **M24AB** = Manhã, seg/qua/sex, 7h30–9h10.

The database stores one row per (day_of_week, start_time, end_time); the same logical code (e.g. M24AB) appears on multiple rows (one per day: SEG, QUA, SEX). Schedule conflict uses same day + overlapping time intervals. The `periodOfDay` filter (Appendix A) maps: M → MORNING, T → AFTERNOON, N → EVENING.

**Seed:** 40 rows (M24AB×3, M35AB×2, M24CD×3, M35CD×2, M24EF×3, M35EF×2, T24AB×3, T35AB×2, T24CD×3, T35CD×2, T24EF×3, T35EF×2, N24AB×3, N35AB×2, N24CD×3, N35CD×2).

---

*End of PRD*
