insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111018', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431018', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115020', 'tribunal-caseworker', true, true, true, false, false, false, '{"DIVORCE", "373"}',
        8, true, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111018', '2021-05-09T20:15:45.345875+01:00');


insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name, major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name, task_system, task_type,
                               termination_reason, title, work_type, execution_type_code, priority_date)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111019', 'SELF','2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431019', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description', '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1', 'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'hearing_work', 'MANUAL', '2022-05-09T20:15:45.345875+01:00');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer, authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115021', 'tribunal-caseworker', true, true, true, false, false, false, '{}',
        8, true, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111019', '2021-05-09T20:15:45.345875+01:00');

insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id,
                               case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name,
                               major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name,
                               task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111020', 'SELF', '2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431020', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description',
        '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1',
        'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'hearing_work', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer,
                                    authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115022', 'tribunal-caseworker', true, true, true, false, false, false,
        '{"DIVORCE"}',
        8, true, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111020', '2021-05-09T20:15:45.345875+01:00');


insert into cft_task_db.tasks (task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id,
                               case_name, case_type_id, created,
                               description, due_date_time, has_warnings, jurisdiction, location, location_name,
                               major_priority, minor_priority,
                               notes, region, region_name, role_category, security_classification, state, task_name,
                               task_system, task_type,
                               termination_reason, title, work_type, execution_type_code)
values ('8d6cc5cf-c973-11eb-bdba-0242ac111021', 'SELF', '2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK',
        '1623278362431021', 'TestCase4', 'Asylum', '2021-05-09T20:15:45.345875+01:00', 'description',
        '2022-05-09T20:15:45.345875+01:00',
        false, 'IA', '765324', 'Taylor House', 0, 0, '[{"user": "userVal", "noteType": "noteTypeVal"}]', '1',
        'TestRegion', 'JUDICIAL',
        'PUBLIC', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', null, 'title', 'hearing_work', 'MANUAL');
insert into cft_task_db.task_roles (task_role_id, role_name, "read", own, "execute", manage, cancel, refer,
                                    authorizations,
                                    assignment_priority, auto_assignable, role_category, task_id, created)
values ('8d6cc5cf-c973-11eb-bdba-0242ac115023', 'tribunal-caseworker', true, true, true, false, false, false,
        '{"DIVORCE"}', 8, true, 'JUDICIAL', '8d6cc5cf-c973-11eb-bdba-0242ac111021', '2021-05-09T20:15:45.345875+01:00');
