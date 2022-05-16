INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110001', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857291', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('2799a725-d062-4a95-9799-7866d08f6c33'::uuid, 'challenged-access-legal-ops',
    false, false, false, true, false, false, '{}', 1, false, 'LEGAL_OPERATIONS', 'f60400a8-d2ba-11ec-a1e4-0242ac110001', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110003', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857293', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('0ce5ae29-ddf8-4f31-b580-635d74dc7879'::uuid, 'challenged-access-judiciary',
    true, false, false, false, false, false, '{}', 1, false, 'JUDICIAL', 'f60400a8-d2ba-11ec-a1e4-0242ac110003', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110004', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857294', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('e1881064-7845-4f07-8eef-c7f53bc7f078'::uuid, 'lead-judge',
    true, false, false, false, false, false, '{}', 1, false, 'JUDICIAL', 'f60400a8-d2ba-11ec-a1e4-0242ac110004', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110005', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857295', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('a64c4999-b393-430c-a51c-ed32ddd0eeb9'::uuid, 'challenged-access-admin',
    false, false, true, false, false, false, '{}', 1, false, 'ADMIN', 'f60400a8-d2ba-11ec-a1e4-0242ac110005', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110006', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857296', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('abefaf84-37f7-4a62-95ba-fb6a983c9a74'::uuid, 'ftpa-judge',
    false, false, true, false, false, false, '{}', 1, false, 'JUDICIAL', 'f60400a8-d2ba-11ec-a1e4-0242ac110006', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110007', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857297', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('3ed85e38-3383-445e-b8d9-944d57bd7849'::uuid, 'senior-tribunal-caseworker',
    true, true, true, false, false, true, '{}', 2, false, 'LEGAL_OPERATIONS', 'f60400a8-d2ba-11ec-a1e4-0242ac110007', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110008', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857298', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('b8b7e6f3-cfa8-423b-860c-241c19eb4e22'::uuid, 'tribunal-caseworker',
    true, true, true, false, false, true, '{}', 2, false, 'LEGAL_OPERATIONS', 'f60400a8-d2ba-11ec-a1e4-0242ac110008', '2022-05-13 13:48:19.808');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('f60400a8-d2ba-11ec-a1e4-0242ac110009', 'process application', 'processApplication', '2022-05-14 13:48:18.972', 'UNASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL,
    '1652446087857299', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:48:18.972', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('bb41586d-1dfe-40c7-a3ff-7c54dc3f941a'::uuid, 'case-manager',
    false, true, false, false, false, false, '{}', 1, false, 'LEGAL_OPERATIONS', 'f60400a8-d2ba-11ec-a1e4-0242ac110009', '2022-05-13 13:48:19.808');
