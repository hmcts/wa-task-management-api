--GRANT_TYPE : STANDARD
INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties, priority_date)
VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110001', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, 5000, 5000, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
       '1652446087857201', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb, '2022-05-14 13:48:18.972');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('b8b7e6f3-cfa8-423b-860c-241c19eb4e01'::uuid, 'tribunal-caseworker',
    true, true, true, false, false, true, '{}', 2, false, 'LEGAL_OPERATIONS', 'f60400a8-d2ba-11ec-a1e4-0242ac110001', '2022-05-13 13:48:19.808');


--GRANT_TYPE : CHALLENGED
INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties, priority_date)
VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110002', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, 5000, 5000, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
       '1652446087857202', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb, '2022-05-14 13:48:18.972');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('b8b7e6f3-cfa8-423b-860c-241c19eb4e02'::uuid, 'challenged-access-admin',
    true, true, true, false, false, true, '{}', 2, false, 'ADMIN', 'f60400a8-d2ba-11ec-a1e4-0242ac110002', '2022-05-13 13:48:19.808');


--GRANT_TYPE : SPECIFIC
INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties, priority_date)
VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110003', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, 5000, 5000, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
       '1652446087857203', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb, '2022-05-14 13:48:18.972');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('b8b7e6f3-cfa8-423b-860c-241c19eb4e03'::uuid, 'ftpa-judge',
    true, true, true, false, false, true, '{}', 2, false, 'JUDICIAL', 'f60400a8-d2ba-11ec-a1e4-0242ac110003', '2022-05-13 13:48:19.808');
