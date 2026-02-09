-- Referência: PRD Section 5 - Database Schema
-- Ordem de criação respeitando dependências de FK

CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE professors (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE time_slots (
    id BIGSERIAL PRIMARY KEY,
    day_of_week VARCHAR(15) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('COORDINATOR', 'STUDENT')),
    course_id BIGINT REFERENCES courses(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_course_id ON users(course_id);

CREATE TABLE curriculum_matrices (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    coordinator_id BIGINT NOT NULL REFERENCES users(id),
    active BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_curriculum_matrices_coordinator_id ON curriculum_matrices(coordinator_id);
CREATE INDEX idx_curriculum_matrices_active ON curriculum_matrices(active);
CREATE INDEX idx_curriculum_matrices_deleted_at ON curriculum_matrices(deleted_at);

CREATE TABLE matrix_classes (
    id BIGSERIAL PRIMARY KEY,
    matrix_id BIGINT NOT NULL REFERENCES curriculum_matrices(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    professor_id BIGINT NOT NULL REFERENCES professors(id),
    time_slot_id BIGINT NOT NULL REFERENCES time_slots(id),
    max_students INTEGER NOT NULL CHECK (max_students > 0),
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (matrix_id, subject_id, time_slot_id)
);

CREATE INDEX idx_matrix_classes_matrix_id ON matrix_classes(matrix_id);
CREATE INDEX idx_matrix_classes_time_slot_id ON matrix_classes(time_slot_id);
CREATE INDEX idx_matrix_classes_deleted_at ON matrix_classes(deleted_at);

CREATE TABLE matrix_class_authorized_courses (
    matrix_class_id BIGINT NOT NULL REFERENCES matrix_classes(id),
    course_id BIGINT NOT NULL REFERENCES courses(id),
    PRIMARY KEY (matrix_class_id, course_id)
);

CREATE TABLE enrollments (
    id BIGSERIAL PRIMARY KEY,
    matrix_class_id BIGINT NOT NULL REFERENCES matrix_classes(id),
    student_id BIGINT NOT NULL REFERENCES users(id),
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (matrix_class_id, student_id)
);

CREATE INDEX idx_enrollments_student_id ON enrollments(student_id);
CREATE INDEX idx_enrollments_matrix_class_id ON enrollments(matrix_class_id);

CREATE INDEX idx_time_slots_day_start_end ON time_slots(day_of_week, start_time, end_time);
