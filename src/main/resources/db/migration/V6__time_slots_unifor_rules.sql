-- Time slot rules per PRD Appendix C: turn (M/T/N), days (246/35), block (AB/CD/EF), code e.g. M24AB
-- Replaces previous time_slots seed with 40 slots. Clears enrollments and matrix_classes (FK dependency).

DELETE FROM enrollments;
DELETE FROM matrix_classes;
DELETE FROM time_slots;

ALTER TABLE time_slots ADD COLUMN code VARCHAR(20) NULL;

-- Manhã (M): A/B 7h30-9h10, C/D 9h30-11h10, E/F 11h20-13h. Days 246 (SEG,QUA,SEX) and 35 (TER,QUI)
INSERT INTO time_slots (day_of_week, start_time, end_time, code, created_at, updated_at) VALUES
    ('SEG', '07:30', '09:10', 'M24AB', now(), now()),
    ('QUA', '07:30', '09:10', 'M24AB', now(), now()),
    ('SEX', '07:30', '09:10', 'M24AB', now(), now()),
    ('TER', '07:30', '09:10', 'M35AB', now(), now()),
    ('QUI', '07:30', '09:10', 'M35AB', now(), now()),
    ('SEG', '09:30', '11:10', 'M24CD', now(), now()),
    ('QUA', '09:30', '11:10', 'M24CD', now(), now()),
    ('SEX', '09:30', '11:10', 'M24CD', now(), now()),
    ('TER', '09:30', '11:10', 'M35CD', now(), now()),
    ('QUI', '09:30', '11:10', 'M35CD', now(), now()),
    ('SEG', '11:20', '13:00', 'M24EF', now(), now()),
    ('QUA', '11:20', '13:00', 'M24EF', now(), now()),
    ('SEX', '11:20', '13:00', 'M24EF', now(), now()),
    ('TER', '11:20', '13:00', 'M35EF', now(), now()),
    ('QUI', '11:20', '13:00', 'M35EF', now(), now()),
    -- Tarde (T): A/B 13h30-15h10, C/D 15h30-17h10, E/F 17h20-19h
    ('SEG', '13:30', '15:10', 'T24AB', now(), now()),
    ('QUA', '13:30', '15:10', 'T24AB', now(), now()),
    ('SEX', '13:30', '15:10', 'T24AB', now(), now()),
    ('TER', '13:30', '15:10', 'T35AB', now(), now()),
    ('QUI', '13:30', '15:10', 'T35AB', now(), now()),
    ('SEG', '15:30', '17:10', 'T24CD', now(), now()),
    ('QUA', '15:30', '17:10', 'T24CD', now(), now()),
    ('SEX', '15:30', '17:10', 'T24CD', now(), now()),
    ('TER', '15:30', '17:10', 'T35CD', now(), now()),
    ('QUI', '15:30', '17:10', 'T35CD', now(), now()),
    ('SEG', '17:20', '19:00', 'T24EF', now(), now()),
    ('QUA', '17:20', '19:00', 'T24EF', now(), now()),
    ('SEX', '17:20', '19:00', 'T24EF', now(), now()),
    ('TER', '17:20', '19:00', 'T35EF', now(), now()),
    ('QUI', '17:20', '19:00', 'T35EF', now(), now()),
    -- Noite (N): A/B 19h-20h40, C/D 21h-22h40 (no E/F)
    ('SEG', '19:00', '20:40', 'N24AB', now(), now()),
    ('QUA', '19:00', '20:40', 'N24AB', now(), now()),
    ('SEX', '19:00', '20:40', 'N24AB', now(), now()),
    ('TER', '19:00', '20:40', 'N35AB', now(), now()),
    ('QUI', '19:00', '20:40', 'N35AB', now(), now()),
    ('SEG', '21:00', '22:40', 'N24CD', now(), now()),
    ('QUA', '21:00', '22:40', 'N24CD', now(), now()),
    ('SEX', '21:00', '22:40', 'N24CD', now(), now()),
    ('TER', '21:00', '22:40', 'N35CD', now(), now()),
    ('QUI', '21:00', '22:40', 'N35CD', now(), now());
