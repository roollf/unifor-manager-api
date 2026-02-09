-- One-time sync for sequences. See R__sync_sequences.sql for ongoing sync.
-- Third param false when empty: next nextval returns 1 (PostgreSQL rejects setval(0)).
SELECT setval('curriculum_matrices_seq', COALESCE((SELECT MAX(id) FROM curriculum_matrices), 1), (SELECT COUNT(*) > 0 FROM curriculum_matrices));
SELECT setval('matrix_classes_seq', COALESCE((SELECT MAX(id) FROM matrix_classes), 1), (SELECT COUNT(*) > 0 FROM matrix_classes));
SELECT setval('enrollments_seq', COALESCE((SELECT MAX(id) FROM enrollments), 1), (SELECT COUNT(*) > 0 FROM enrollments));
