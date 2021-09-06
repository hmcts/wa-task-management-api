insert into cft_task_db.execution_types (execution_code, execution_name, description)
values  ('MANUAL', 'MANUAL', 'Manual Description'),
        ('BUILT_IN', 'BUILT_IN', 'BUILT_IN'),
        ('CASE_EVENT', 'CASE_EVENT', 'CASE_EVENT');

/*
 roleName: tribunal-caseworker, securityClassification: PUBLIC,
 */
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111000', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431000', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115000', 'tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111000', '2021-05-09T20:15:45.345875+01:00');

/*
 roleName: tribunal-caseworker, securityClassification: PRIVATE,
 */
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111001', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431001', 'TestCase', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-06-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PRIVATE', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115001', 'tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111001', '2021-05-09T20:15:45.345875+01:00');

/*
 roleName: tribunal-caseworker, securityClassification: RESTRICTED,
 */
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111002', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431002', 'TestCase6', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-07-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115002', 'tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111002', '2021-05-09T20:15:45.345875+01:00');


/*
 roleName: senior-tribunal-caseworker, securityClassification: PUBLIC,
 */
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111003', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431003', 'TestCase2', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-08-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115003', 'senior-tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111003', '2021-05-09T20:15:45.345875+01:00');

/*
 roleName: senior-tribunal-caseworker, securityClassification: PRIVATE,
 */
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111004', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431004', 'TestCase2', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-09-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PRIVATE', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115004', 'senior-tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111004', '2021-05-09T20:15:45.345875+01:00');

/*
 roleName: senior-tribunal-caseworker, securityClassification: RESTRICTED,
 */
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111005', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431005', 'TestCase3', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-10-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115005', 'senior-tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111005', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115006', 'tribunal-caseworker', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111005', '2021-05-09T20:15:45.345875+01:00');

/*
GrantType Challenged
*/
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111006', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431006', 'TestCase', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115007', 'senior-tribunal-caseworker', 'true', false, false, false, false, false, '{"DIVORCE", "PROBATE"}',
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111006', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115008', 'tribunal-caseworker', 'true', false, false, false, false, false, '{"DIVORCE", "PROBATE"}',
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111006', '2021-05-09T20:15:45.345875+01:00');


/*
GrantType Basic - public
*/
insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111007', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431007', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115009', 'hmcts-judiciary', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111007', '2021-05-09T20:15:45.345875+01:00');

/*
GrantType Basic - private
*/

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111008', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431008', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PRIVATE', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115010', 'hmcts-judiciary', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111008', '2021-05-09T20:15:45.345875+01:00');

/*
GrantType Basic - RESTRICTED
*/

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111009', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431009', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115011', 'hmcts-judiciary', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111009', '2021-05-09T20:15:45.345875+01:00');


/*
Used by pagination test
*/

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111010', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431010', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115012', 'pagination-role', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111010', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111011', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431011', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115013', 'pagination-role', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111011', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111012', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431012', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115014', 'pagination-role', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111012', '2021-05-09T20:15:45.345875+01:00');


insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111013', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431013', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115015', 'pagination-role', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111013', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111014', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431014', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115016', 'pagination-role', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111014', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111015', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431015', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115017', 'inActiveRole', 'true', false, false, false, false, false, null,
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111015', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111016', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431016', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'workType', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115018', 'standard-caseworker', 'true', false, false, false, false, false, '{"DIVORCE", "PROBATE"}',
        0, false, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111016', '2021-05-09T20:15:45.345875+01:00');