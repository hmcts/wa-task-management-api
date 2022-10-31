--GRANT_TYPE : STANDARD
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111001', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431001', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'LEGAL_OPERATIONS',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
            "read", own, "execute", manage, cancel,
            authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
        values ('8d6cc5cf-c973-11eb-bdba-0242ac115001', 'tribunal-caseworker',
            true, true , true , false , true,
            null,0, false, 'LEGAL_OPERATIONS', '8d6cc5cf-c973-11eb-bdba-0242ac111001', '2021-05-09T20:15:45.345875+01:00');


INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111011', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431011', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'LEGAL_OPERATIONS',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115011', 'tribunal-caseworker',
        false, false , true , false , false,
        null,0, false, 'LEGAL_OPERATIONS', '8d6cc5cf-c973-11eb-bdba-0242ac111011', '2021-05-09T20:15:45.345875+01:00');


INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111021', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431021', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'LEGAL_OPERATIONS',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115021', 'tribunal-caseworker',
        true, false , false , true , true,
        null,0, false, 'LEGAL_OPERATIONS', '8d6cc5cf-c973-11eb-bdba-0242ac111021', '2021-05-09T20:15:45.345875+01:00');

--GRANT_TYPE : CHALLENGED
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111002', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431002', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
            "read", own, "execute", manage, cancel,
            authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
        values ('8d6cc5cf-c973-11eb-bdba-0242ac115002', 'challenged-access-admin',
            true, true , true , false , true,
            null,0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111002', '2021-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111022', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431022', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115022', 'challenged-access-admin',
        false, false , true , false , false,
        null,0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111022', '2021-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111032', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431032', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115032', 'challenged-access-admin',
        true, false , false , true , true,
        null,0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111032', '2021-05-09T20:15:45.345875+01:00');

--GRANT_TYPE : SPECIFIC
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111003', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431003', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
            "read", own, "execute", manage, cancel,
            authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
        values ('8d6cc5cf-c973-11eb-bdba-0242ac115003', 'ftpa-judge',
            true, true , true , false , true,
            null,0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111003', '2021-05-09T20:15:45.345875+01:00');


--GRANT_TYPE : SPECIFIC
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111004', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115004', 'ftpa-judge',
        false, true , false , false , false,
        null,0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111004', '2021-05-09T20:15:45.345875+01:00');


--GRANT_TYPE : SPECIFIC
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111005', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115005', 'ftpa-judge',
        true, false , false , true , true,
        null,0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111005', '2021-05-09T20:15:45.345875+01:00');
