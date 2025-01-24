--GRANT_TYPE : STANDARD
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date, termination_process, last_updated_action)
    VALUES ('9a6cc5cf-c973-11eb-bdba-0242ac111001', 'SELF','2024-01-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431001', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'LEGAL_OPERATIONS',
        'PUBLIC', 'UNASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00',
         'EXUI_USER_COMPLETION', 'Configure');
