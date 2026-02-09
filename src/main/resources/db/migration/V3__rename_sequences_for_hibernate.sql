-- Hibernate expects sequences named {table}_seq, but PostgreSQL BIGSERIAL creates {table}_id_seq.
-- Rename sequences to match Hibernate's default naming and update column defaults.

ALTER SEQUENCE subjects_id_seq RENAME TO subjects_seq;
ALTER TABLE subjects ALTER COLUMN id SET DEFAULT nextval('subjects_seq');

ALTER SEQUENCE professors_id_seq RENAME TO professors_seq;
ALTER TABLE professors ALTER COLUMN id SET DEFAULT nextval('professors_seq');

ALTER SEQUENCE time_slots_id_seq RENAME TO time_slots_seq;
ALTER TABLE time_slots ALTER COLUMN id SET DEFAULT nextval('time_slots_seq');

ALTER SEQUENCE courses_id_seq RENAME TO courses_seq;
ALTER TABLE courses ALTER COLUMN id SET DEFAULT nextval('courses_seq');

ALTER SEQUENCE users_id_seq RENAME TO users_seq;
ALTER TABLE users ALTER COLUMN id SET DEFAULT nextval('users_seq');

ALTER SEQUENCE curriculum_matrices_id_seq RENAME TO curriculum_matrices_seq;
ALTER TABLE curriculum_matrices ALTER COLUMN id SET DEFAULT nextval('curriculum_matrices_seq');

ALTER SEQUENCE matrix_classes_id_seq RENAME TO matrix_classes_seq;
ALTER TABLE matrix_classes ALTER COLUMN id SET DEFAULT nextval('matrix_classes_seq');

ALTER SEQUENCE enrollments_id_seq RENAME TO enrollments_seq;
ALTER TABLE enrollments ALTER COLUMN id SET DEFAULT nextval('enrollments_seq');
