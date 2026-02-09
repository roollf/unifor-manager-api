# unifor-manager

Academic Course Registration System — a web application for coordinators to manage curriculum matrices and for students to view enrollments and enroll in classes.

## Tech Stack

- **Java 21**
- **Quarkus 3.x**
- **PostgreSQL**
- **Keycloak** (OIDC authentication)
- **Hibernate ORM / Panache**
- **Flyway** (migrations)

## Prerequisites

- JDK 21+
- Maven 3.9+
- PostgreSQL
- Keycloak (for production-like auth; optional for dev)

## Quick Start

### 1. Database

Create database and user:

```sql
CREATE DATABASE unifor_manager;
CREATE USER unifor WITH PASSWORD 'unifor';
GRANT ALL PRIVILEGES ON DATABASE unifor_manager TO unifor;
```

Flyway runs migrations on startup (tables + seed data).

### 2. Run the application

**With Keycloak** (recommended for full auth):

```bash
# Ensure Keycloak is running at http://localhost:8081
./mvnw quarkus:dev
```

**Without Keycloak** (local dev only):

```bash
./mvnw quarkus:dev -Dquarkus.profile=dev-without-keycloak
```

With `dev-without-keycloak`, OIDC is disabled and endpoints accept unauthenticated requests. Use only for local development.

### 3. API Documentation

- **OpenAPI:** http://localhost:8080/q/openapi
- **Swagger UI:** http://localhost:8080/q/swagger-ui

## Keycloak Setup

For full authentication, configure Keycloak:

1. Realm: `unifor`
2. Client: `unifor-manager` (confidential)
3. Roles: `coordinator`, `student`
4. User emails must match seeded `users.email` (see `V2__seed_data.sql`)

See `application.properties` and PRD Appendix B for details.

## API Overview

| Role        | Endpoints                    |
|-------------|------------------------------|
| Coordinator | `/api/coordinator/matrices`, `/api/coordinator/matrices/{id}/classes` |
| Student     | `/api/student/enrollments`, `/api/student/classes/available`         |

Coordinator: create/update/delete matrices and classes.  
Student: list enrollments, list available classes, enroll.

## Tests

```bash
./mvnw test
```

Tests use **Testcontainers** for PostgreSQL; no local DB required. See `Phase2Test`–`Phase6Test` for phase validation.

## Project Structure

```
org.unifor
├── api/coordinator      # Coordinator REST endpoints
├── api/student          # Student REST endpoints
├── service              # Business logic (coordinator, student)
├── repository           # Panache repositories
├── entity               # JPA entities
├── dto                  # Request/Response DTOs
├── exception            # Custom exceptions + mappers
└── security             # CurrentUserService, Keycloak mapping
```

## Documentation

- **PRD.md** — Product requirements
- **ARCHITECTURE.md** — Technical architecture
- **PRD_COMPLIANCE.md** — Implementation compliance report

## Docker Compose

Run the full stack (PostgreSQL, Keycloak, application) with Docker Compose:

```bash
# Build the JAR first (required by Dockerfile)
./mvnw package

# Start all services
docker compose up --build
```

**Port mappings:**

| Service   | Host Port | Container Port |
|-----------|-----------|----------------|
| App       | 8080      | 8080           |
| Keycloak  | 8081      | 8080           |

**Credentials:**

- **Keycloak Admin Console:** http://localhost:8081 — user `admin`, password `admin`
- **Seeded users (coordinator/student):** password `secret`
  - Coordinators: carmen.lima@unifor.br, roberto.alves@unifor.br, fernanda.souza@unifor.br
  - Students: lucas.ferreira@unifor.br, beatriz.rodrigues@unifor.br, rafael.pereira@unifor.br, juliana.martins@unifor.br, gabriel.costa@unifor.br

The realm `unifor` and client `unifor-manager` are imported automatically from `keycloak/unifor-realm.json`.

## Packaging

```bash
# JAR
./mvnw package

# Native
./mvnw package -Dnative -Dquarkus.native.container-build=true
```
