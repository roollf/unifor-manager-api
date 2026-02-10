# Technical Architecture
## Academic Course Registration System

**Version:** 1.0  
**Date:** February 8, 2025  
**References:** PRD.md

---

## 1. Package Structure

### 1.1 Layered + Feature-Oriented Organization

Packages are organized by **layer** first, with **feature subpackages** where it adds clarity. This balances separation of concerns with navigability.

```
org.unifor
├── api                    # REST resources (controllers)
│   ├── coordinator       # Coordinator endpoints
│   └── student           # Student endpoints
├── service               # Application/business logic
│   ├── coordinator       # Coordinator-specific services
│   └── student           # Student-specific services
├── repository            # Data access (Panache repositories)
├── entity                # JPA entities
├── dto                   # Request/Response DTOs
│   ├── request           # Create/Update request DTOs
│   └── response          # Response DTOs
├── exception             # Custom exceptions + exception mappers
├── security              # Security-related (current user resolution, etc.)
└── config                # Application configuration (if needed beyond application.properties)
```

### 1.2 Rationale

- **api**: REST layer only. Delegates to services, maps DTOs. Thin controllers.
- **service**: Business rules, orchestration, validation. Coordinates repositories.
- **repository**: Data access. Panache repositories extend `PanacheRepository<Entity>`.
- **entity**: Domain model. No business logic.
- **dto**: Clear split between request and response. Nested DTOs live in `response` when reused.
- **exception**: Centralized error handling and mapping to HTTP responses.
- **security**: Logic to resolve `SecurityIdentity` / Keycloak principal to `User` entity.

### 1.3 Cross-Cutting Concerns

- Validation (Bean Validation): Applied on DTOs in `api` and/or `dto.request`.
- Transaction boundaries: `@Transactional` on service methods, not on resources.

---

## 2. Entity Design

### 2.1 PanacheEntity Usage

All persistent entities with a generated primary key extend `PanacheEntity` (from `io.quarkus.hibernate.orm.panache.PanacheEntity`). This provides:

- `id` (Long) and `persist()` out of the box
- Consistent identity handling
- Panache repository support

**Entities extending PanacheEntity:**

| Entity | Table | Notes |
|--------|-------|-------|
| User | users | role enum, course_id nullable |
| Subject | subjects | Reference data |
| Professor | professors | Reference data |
| TimeSlot | time_slots | Reference data |
| Course | courses | Reference data |
| CurriculumMatrix | curriculum_matrices | Soft delete support |
| MatrixClass | matrix_classes | Soft delete support |
| Enrollment | enrollments | No soft delete |

**No separate entity for:** `matrix_class_authorized_courses`. Use `@ManyToMany` with `@JoinTable` on `MatrixClass`.

### 2.2 Relationship Strategy: Lazy vs Eager

**Default: LAZY for all associations.**

| Entity | Association | Fetch Type | Rationale |
|--------|-------------|------------|-----------|
| CurriculumMatrix | coordinator (User) | LAZY | Loaded only when needed; coordinator operations fetch explicitly |
| CurriculumMatrix | classes (MatrixClass) | LAZY | List endpoints use repository queries with fetch joins |
| MatrixClass | matrix, subject, professor, timeSlot | LAZY | Fetch join in list/find queries to avoid N+1 |
| MatrixClass | authorizedCourses | LAZY | Loaded when building response DTOs; explicit fetch in queries |
| Enrollment | matrixClass, student | LAZY | Fetch join in student enrollment listing |

**Implementation approach:**
- Keep default `FetchType.LAZY` on all `@OneToMany`, `@ManyToOne`, `@ManyToMany`.
- Use `JOIN FETCH` in repository methods (e.g., `findByIdWithDetails`) when a specific use case needs related data.
- Avoid `EAGER` to prevent accidental N+1 and large loads.

### 2.3 Soft Delete Implementation

**Approach: `@Where` clause on entities**

Entities that support soft delete (`CurriculumMatrix`, `MatrixClass`) use Hibernate's `@Where`:

```java
@Where(clause = "deleted_at IS NULL")
public class MatrixClass extends PanacheEntity { ... }
```

**Mechanics:**
- `deleted_at` column: `Instant` or `OffsetDateTime`, nullable. Non-null = soft deleted.
- On delete: Update `deleted_at = now()` instead of physical delete.
- All queries (including `findById`) automatically exclude soft-deleted rows.
- When "include deleted" is needed (e.g., coordinator filter): use `@FilterDef` + `@Filter` and enable the filter only for that query, or use native/criteria queries that omit the `@Where` condition.

**Alternative considered:** `@SQLDelete` with override to UPDATE. Chosen approach (`@Where`) is simpler and ensures all reads filter by default.

**Base class (optional):** A `SoftDeletableEntity` extending `PanacheEntity` with `deletedAt`, `@Where`, and `softDelete()` method could reduce duplication. Document the decision; implementation can add it if desired.

---

## 3. DTO Strategy

### 3.1 Naming Patterns

| Pattern | Example | Use |
|---------|---------|-----|
| `CreateXxxRequest` | CreateMatrixClassRequest | POST body for creation |
| `UpdateXxxRequest` | UpdateMatrixClassRequest | PUT body for updates |
| `XxxResponse` | MatrixClassResponse | Full response for single resource |
| `XxxSummaryResponse` | MatrixSummaryResponse | Reduced fields for list items |
| `XxxListResponse` | MatrixClassListResponse | Paginated/wrapped list with `items` + `total` |

**Consistent suffixes:** `Request` for input, `Response` for output.

### 3.2 When to Use Nested DTOs

**Use nested DTOs when:**
- The same structure appears in multiple responses (e.g., Subject + Professor + TimeSlot in MatrixClass and Enrollment responses).
- You want to control the JSON shape and avoid exposing full entities.
- You need to flatten or transform (e.g., `TimeSlotDto` with `dayOfWeek`, `startTime`, `endTime` as strings).

**Shared nested DTOs (in `dto.response`):**
- `SubjectDto` — id, name
- `ProfessorDto` — id, name
- `TimeSlotDto` — id, dayOfWeek, startTime, endTime
- `CourseDto` — id, name (when listing authorized courses)

**Composed responses:**
- `MatrixClassResponse`: includes `SubjectDto`, `ProfessorDto`, `TimeSlotDto`, `List<CourseDto>`, etc.
- `EnrollmentResponse`: includes `SubjectDto`, `ProfessorDto`, `TimeSlotDto` (from the enrolled class).

**Avoid nested DTOs when:**
- Only an ID is needed (e.g., `matrixClassId` in enrollment response).
- The structure is trivial (e.g., single id/name) and used once — inline fields may suffice, but reuse is preferred for consistency.

### 3.3 Pagination

Use a generic wrapper for list endpoints that support pagination:

```java
public record PageResponse<T>(List<T> items, long total) {}
```

Query params: `page`, `size` (or `limit`/`offset`). Defaults (e.g., page=0, size=20) defined in the API layer.

---

## 4. Security Architecture

### 4.1 Keycloak Configuration Approach

**Quarkus OIDC** (`quarkus-oidc`) with Keycloak as the OpenID Connect provider.

**Configuration (application.properties):**
- `quarkus.oidc.auth-server-url` — Keycloak realm URL
- `quarkus.oidc.client-id` — Application client ID
- `quarkus.oidc.credentials.secret` — Client secret (for confidential clients)
- `quarkus.http.auth.permission.*` — Path-based rules (optional; method-level `@RolesAllowed` is primary)

**Roles:** Realm or client roles `coordinator` and `student`. Keycloak assigns roles to users; tokens include them in `realm_access.roles` or `resource_access.<client>.roles`.

**Token usage:** Bearer JWT. Quarkus validates the token and exposes `SecurityIdentity` with roles and claims (e.g., `email` from token).

### 4.2 Role-Based Access at Method Level

**Resource layer:** Use `@RolesAllowed` on each endpoint:

```java
@Path("/api/coordinator/matrices")
@RolesAllowed("coordinator")
public class MatrixResource { ... }

@Path("/api/coordinator/reference")
@RolesAllowed("coordinator")
public class ReferenceDataResource { ... }  // GET /subjects, /professors, /time-slots, /courses (for Add class form dropdowns)

@Path("/api/student/enrollments")
@RolesAllowed("student")
public class EnrollmentResource { ... }
```

- Unauthenticated requests → 401 (handled by OIDC).
- Authenticated but wrong role → 403 (handled by Quarkus security).
- Authenticated + correct role → proceed to resource logic.

### 4.3 Validating User Context in Services

**Problem:** Services must enforce resource-level rules (e.g., coordinator owns matrix, student accesses only own enrollments). For that they need the current `User` entity, not only the Keycloak principal.

**Approach: Resolve User in Resource, Pass to Service**

1. **Resource layer:** Inject `SecurityIdentity`, extract email from token (e.g., `preferred_username` or custom claim mapped to email).
2. **Resolve User:** Call `UserRepository.findByEmail(email)` or `UserService.findCurrentUser(email)`.
3. **Handle missing user:** If no `User` with that email exists (e.g., Keycloak user not seeded) → return 403.
4. **Pass to service:** Pass `User` (or `userId`) as a parameter to service methods that need it.

**Optional: `@RequestScoped` CurrentUser producer**

- A CDI producer reads `SecurityIdentity`, resolves `User` by email, and exposes `User currentUser` for injection.
- Services inject `User currentUser` instead of receiving it as a parameter.
- Trade-off: Cleaner service signatures vs. explicit dependency on request context. Document the chosen approach.

**Resource-level checks:**
- Coordinator operations: Service receives `User coordinator`. Before any matrix operation, verify `matrix.getCoordinatorId().equals(coordinator.id)` (or equivalent). Throw `ForbiddenException` if not.
- Student operations: Service receives `User student`. Enrollment and listing use `student.id`; no need to pass student ID from path.

---

## 5. Concurrency Control

### 5.1 Strategy for Seat Availability

**Approach: Pessimistic locking on MatrixClass during enrollment**

When a student enrolls:
1. Start transaction.
2. `SELECT matrix_class ... FOR UPDATE` (or `LockModeType.PESSIMISTIC_WRITE` via Panache/JPA) to lock the class row.
3. Check seat availability: `SELECT COUNT(*) FROM enrollments WHERE matrix_class_id = ?`.
4. Validate all other rules: course authorization, schedule conflict, duplicate subject.
5. Insert enrollment.
6. Commit.

This ensures only one enrollment transaction can hold the lock on a given class at a time, preventing overbooking.

**Isolation level:** PostgreSQL default READ COMMITTED. `SELECT ... FOR UPDATE` provides row-level locking; no need for SERIALIZABLE. Lock is held until transaction commit.

**Implementation:** Use `MatrixClassRepository.findByIdForUpdate(matrixClassId)` which issues `SELECT ... FOR UPDATE` (PESSIMISTIC_WRITE) on the matrix_class row. Ensure the lock is acquired before any validation that depends on current enrollment count.

### 5.2 Transaction Boundaries

- **Scope:** Service layer. Use `@Transactional` on service methods that perform writes.
- **Read-only operations:** List, get-by-id. Use `@Transactional(readOnly = true)` when applicable to allow DB optimizations.
- **Enrollment:** Single `@Transactional` method that performs lock, validation, and insert. No nested transactions; keep it one unit of work.
- **Coordinator writes:** Create/update/delete matrix class — each in its own `@Transactional` method.
- **Transaction propagation:** Default `REQUIRED`; no need for nested transactions.

**Rule:** Do not start transactions in the resource layer; services own transaction boundaries.

---

## 6. Error Handling

### 6.1 Custom Exception Hierarchy

```
UniforException (abstract, base)
├── NotFoundException          → 404 Not Found
├── ForbiddenException         → 403 Forbidden
├── ConflictException          → 409 Conflict
├── ValidationException        → 400 Bad Request
└── UnauthorizedException      → 401 Unauthorized (if needed beyond OIDC)
```

**Usage:**
- `NotFoundException`: Entity not found (e.g., matrix, class, user by email).
- `ForbiddenException`: Valid auth but no permission (e.g., coordinator does not own matrix).
- `ConflictException`: Business rule violation (no seats, schedule conflict, duplicate subject, cannot delete class with enrollments).
- `ValidationException`: Invalid input (e.g., invalid IDs, constraint violation).

**Structure:** All extend `UniforException`, which holds an optional `errorCode` (string) and `message`. Subclasses can add `details` (e.g., `Map<String, Object>`) for structured error info.

### 6.2 Error Response Format

Align with PRD Section 4.4:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable message",
  "details": {}
}
```

**Mapping:**
- `code`: From exception (e.g., `NOT_FOUND`, `CONFLICT_NO_SEATS`, `CONFLICT_SCHEDULE`, `FORBIDDEN_NOT_OWNER`).
- `message`: User-facing message; can be overridden per exception.
- `details`: Optional; e.g., `{ "matrixClassId": 123 }` for conflict context.

### 6.3 Exception Mappers

Use JAX-RS `ExceptionMapper`:

- `NotFoundExceptionMapper` → 404
- `ForbiddenExceptionMapper` → 403
- `ConflictExceptionMapper` → 409
- `ValidationExceptionMapper` → 400
- `UniforExceptionMapper` → catch-all for base type
- Optional: `ConstraintViolationExceptionMapper` for Bean Validation → 400

Each mapper produces the standard JSON structure and the appropriate HTTP status. Keep mappers thin; logic stays in exceptions and services.

---

## Appendix: Dependency Overview

| Layer | Depends On |
|-------|------------|
| api | service, dto, exception |
| service | repository, entity, exception, security (if CurrentUser used) |
| repository | entity |
| entity | — |
| dto | — |
| exception | — |
| security | repository (User), entity |

---

*End of Architecture Document*
