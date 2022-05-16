INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
    VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111017', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431017', 'TestCase4', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
        values ('8d6cc5cf-c973-11eb-bdba-0242ac115019', 'tribunal-caseworker', true, false , false , false , true, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111017', '2021-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
    VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111018', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431018', 'TestCase4', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
        VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115020', 'challenged-access-admin',
        true, false, false, false, true, false, '{"DIVORCE", "373"}',0, false, 'ADMIN',
        '8d6cc5cf-c973-11eb-bdba-0242ac111018', '2021-05-09T20:15:45.345875+01:00');


INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('8a224730-d2ad-11ec-a1e4-0242ac11000c', 'review specific access request legal ops', 'reviewSpecificAccessRequestLegalOps', '2022-05-14 12:12:17.483','ASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'review specific access request legal ops', '', NULL, NULL, NULL, NULL, false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'access_requests', 'LEGAL_OPERATIONS', false, NULL,'1652440325253034', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 12:12:17.483', '{"roleAssignmentId": "assignmentId"}'::jsonb);

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,"read", own, "execute", manage, cancel, refer, authorizations,assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('2fc4e4b9-5020-4748-9f7f-d9dbceeb5f09'::uuid, 'challenged-access-admin', true, true, false, true, true, false, '{"DIVORCE", "373"}',1, false, 'ADMIN', '8a224730-d2ad-11ec-a1e4-0242ac11000c', '2022-05-13 12:12:18.430');

INSERT INTO cft_task_db.tasks (task_id, task_name, task_type, due_date_time, state, task_system, security_classification, title, description, notes, major_priority, minor_priority, assignee, auto_assigned, execution_type_code, work_type, role_category, has_warnings, assignment_expiry, case_id, case_type_id, case_category, case_name, jurisdiction, region, region_name, "location", location_name, business_context, termination_reason, created, additional_properties)
    VALUES('0985588a-d2b5-11ec-a1e4-0242ac11000c', 'process application', 'processApplication', '2022-05-14 13:05:59.786', 'ASSIGNED'::cft_task_db."task_state_enum", 'SELF'::cft_task_db."task_system_enum", 'PUBLIC'::cft_task_db."security_classification_enum", 'process application', '[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)', NULL, NULL, NULL, 'd1988f21-0748-3988-801f-caf5655f328e', false, 'CASE_EVENT'::cft_task_db."execution_type_enum", 'hearing_work', 'LEGAL_OPERATIONS', false, NULL, '1652443548439927', 'WaCaseType', 'Protection', 'Bob Smith', 'WA', '1', NULL, '765324', 'Taylor House', NULL, NULL, '2022-05-13 13:05:59.786', '{"key1": "value1", "key2": "value2", "key3": "value3", "key4": "value4"}'::jsonb);

INSERT INTO cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
    VALUES('466115be-0fd5-48ec-9afa-76bacd9cc83d'::uuid, 'ftpa-judge', true, false, true, false, false, false, '{}', 1, false, 'JUDICIAL', '0985588a-d2b5-11ec-a1e4-0242ac11000c', '2022-05-13 13:06:00.776');
