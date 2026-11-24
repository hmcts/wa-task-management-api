INSERT INTO cft_task_db.tasks
    (task_id, task_name, task_type, state, case_id, jurisdiction, created, due_date_time, priority_date)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'Resolve Stopped Case', 'ResolveStoppedCase',
     'UNASSIGNED', '1793986596864362', 'PROBATE', '2026-11-11 09:00:00', '2026-11-12 16:00:00', '2026-11-12 16:00:00'),
    ('70000000-0000-0000-0000-000000000002', 'Examine Digital Case - Probate', 'ExamineDigitalCaseProbate',
     'CANCELLED', '1793986596864362', 'PROBATE', '2026-11-11 09:00:00', '2026-11-12 16:00:00', '2026-11-12 16:00:00'),
    ('70000000-0000-0000-0000-000000000003', 'Review Appeal', 'reviewAppeal',
     'UNASSIGNED', '1793986596864362', 'IA', '2026-11-11 09:00:00', '2026-11-12 16:00:00', '2026-11-12 16:00:00'),
    ('70000000-0000-0000-0000-000000000004', 'Resolve Stopped Case', 'ResolveStoppedCase',
     'UNASSIGNED', '1793986596864363', 'PROBATE', '2026-11-11 09:00:00', '2026-11-12 16:00:00', '2026-11-12 16:00:00');
