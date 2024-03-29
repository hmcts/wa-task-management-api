--GRANT_TYPE : STANDARD
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
    VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111001', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431001', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'LEGAL_OPERATIONS',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
            "read", own, "execute", manage, cancel,
            authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
        values ('8d6cc5cf-c973-11eb-bdba-0242ac115001', 'tribunal-caseworker',
            true, false , false , false , false,
            null,0, false, 'LEGAL_OPERATIONS', '8d6cc5cf-c973-11eb-bdba-0242ac111001', '2021-05-09T20:15:45.345875+01:00');


--AUTHORIZATIONS ("DIVORCE", "373")
INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
    VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111004', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

    INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
            "read", own, "execute", manage, cancel,
            authorizations, assignment_priority, auto_assignable, role_category, task_id, created)
        VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115004', 'challenged-access-admin',
        true, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111004', '2021-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111005', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115005', 'challenged-access-admin',
        false, true, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111005', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, true, false, false, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111006', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115006', 'challenged-access-admin',
        false, false, true, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111006', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, true, false, false, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111007', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115007', 'challenged-access-admin',
        false, false, true, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111007', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, true, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111008', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115008', 'challenged-access-admin',
        false, true, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111008', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, true, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111009', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115009', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111009', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, true, false, true, false, false, false, false);

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115019', 'judge',
        false, true, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111009', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, true, false, false, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111010', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115010', 'challenged-access-admin',
        false, true, true, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111010', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, false, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111011', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115011', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111011', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, true, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111012', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115012', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111012', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, true, false, false, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111013', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115013', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111013', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, false, false, false, true, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111014', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115014', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111014', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, true, false, false, true, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111015', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115015', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111015', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, false, false, false, false, true);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111016', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115016', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111016', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, true, true, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111017', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115017', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111017', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, false, false, true, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111018', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115018', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111018', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, true, true, false, false, false, false);

INSERT INTO cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, next_hearing_date, priority_date)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac111019', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase', 'WaCaseType', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'WA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'ADMIN',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'processApplication', null, 'title', 'hearing_work', 'MANUAL','2022-05-10T20:15:45.345875+01:00', '2022-05-09T20:15:45.345875+01:00');

INSERT INTO cft_task_db.task_roles (task_role_id, role_name,
                                    "read", own, "execute", manage, cancel,
                                    authorizations, assignment_priority, auto_assignable, role_category, task_id, created,
                                    complete, complete_own, cancel_own, claim, unclaim, assign, unassign, unclaim_assign,
                                    unassign_claim, unassign_assign)
VALUES ('8d6cc5cf-c973-11eb-bdba-0242ac115119', 'challenged-access-admin',
        false, false, false, false, false,
        '{"DIVORCE", "373"}',0, false, 'ADMIN', '8d6cc5cf-c973-11eb-bdba-0242ac111019', '2021-05-09T20:15:45.345875+01:00',
        false, false, false, false, false, false, true, true, false, false);
