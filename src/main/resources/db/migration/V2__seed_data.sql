-- Seed data: PRD mínimos (15 subjects, 5 professors, 9 time_slots, 9 courses, 5 students, 3 coordinators)
-- Emails devem corresponder aos usuários configurados no Keycloak

-- Subjects (15)
INSERT INTO subjects (name, created_at, updated_at) VALUES
    ('Cálculo I', now(), now()),
    ('Cálculo II', now(), now()),
    ('Álgebra Linear', now(), now()),
    ('Física I', now(), now()),
    ('Física II', now(), now()),
    ('Química Geral', now(), now()),
    ('Programação I', now(), now()),
    ('Programação II', now(), now()),
    ('Estrutura de Dados', now(), now()),
    ('Banco de Dados', now(), now()),
    ('Engenharia de Software', now(), now()),
    ('Redes de Computadores', now(), now()),
    ('Sistemas Operacionais', now(), now()),
    ('Metodologia Científica', now(), now()),
    ('Estatística', now(), now());

-- Professors (5)
INSERT INTO professors (name, created_at, updated_at) VALUES
    ('Maria Silva', now(), now()),
    ('João Santos', now(), now()),
    ('Ana Costa', now(), now()),
    ('Pedro Oliveira', now(), now()),
    ('Carla Mendes', now(), now());

-- Time slots (9) - SEG/TER/QUA/QUI/SEX, manhã e tarde
INSERT INTO time_slots (day_of_week, start_time, end_time, created_at, updated_at) VALUES
    ('SEG', '07:00', '09:00', now(), now()),
    ('SEG', '09:00', '11:00', now(), now()),
    ('SEG', '14:00', '16:00', now(), now()),
    ('TER', '07:00', '09:00', now(), now()),
    ('TER', '09:00', '11:00', now(), now()),
    ('QUA', '07:00', '09:00', now(), now()),
    ('QUA', '14:00', '16:00', now(), now()),
    ('QUI', '07:00', '09:00', now(), now()),
    ('SEX', '09:00', '11:00', now(), now());

-- Courses (9)
INSERT INTO courses (name, created_at, updated_at) VALUES
    ('Ciência da Computação', now(), now()),
    ('Engenharia Civil', now(), now()),
    ('Engenharia Elétrica', now(), now()),
    ('Administração', now(), now()),
    ('Direito', now(), now()),
    ('Psicologia', now(), now()),
    ('Enfermagem', now(), now()),
    ('Design Digital', now(), now()),
    ('Engenharia de Software', now(), now());

-- Coordinators (3) - course_id NULL
INSERT INTO users (email, name, role, course_id, created_at, updated_at) VALUES
    ('carmen.lima@unifor.br', 'Carmen Lima', 'COORDINATOR', NULL, now(), now()),
    ('roberto.alves@unifor.br', 'Roberto Alves', 'COORDINATOR', NULL, now(), now()),
    ('fernanda.souza@unifor.br', 'Fernanda Souza', 'COORDINATOR', NULL, now(), now());

-- Students (5) - course_id referenciando courses (ids 1-9)
INSERT INTO users (email, name, role, course_id, created_at, updated_at) VALUES
    ('lucas.ferreira@unifor.br', 'Lucas Ferreira', 'STUDENT', 1, now(), now()),
    ('beatriz.rodrigues@unifor.br', 'Beatriz Rodrigues', 'STUDENT', 2, now(), now()),
    ('rafael.pereira@unifor.br', 'Rafael Pereira', 'STUDENT', 4, now(), now()),
    ('juliana.martins@unifor.br', 'Juliana Martins', 'STUDENT', 6, now(), now()),
    ('gabriel.costa@unifor.br', 'Gabriel Costa', 'STUDENT', 1, now(), now());
