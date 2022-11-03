INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date,
                               reconfigure_request_time, last_reconfiguration_time)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac222001', 'SELF', '2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431001', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'LEGAL_OPERATIONS',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00',
        '2022-05-09T20:15:45.345875+01:00', '2022-10-18T10:19:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac122001', 'tribunal-caseworker',
        true, false, false, false, false,
        null, 0, false, 'LEGAL_OPERATIONS', '8d6cc5cf-c973-11eb-bdba-0242ac222001', '2021-05-09T20:15:45.345875+01:00');

