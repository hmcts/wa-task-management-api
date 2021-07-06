INSERT INTO execution_types(execution_code, description, execution_name) VALUES('MANUAL', 'Manual', 'Manual Description');

INSERT INTO tasks
(task_id, assignee, assignment_expiry, auto_assigned, business_context, case_id, case_name, case_type_id, created,
 description, due_date_time, has_warnings, jurisdiction, "location", location_name, major_priority, minor_priority, notes,
 region, region_name, role_category, security_classification, state, task_name, task_system, task_type, termination_reason, title, work_type, execution_code)
VALUES('8d6cc5cf-c973-11eb-bdba-0242ac11001e', 'SELF', '2022-05-09T20:15:45.345875+01:00', false, 'CFT_TASK', '1623278362430412', 'TestCase', 'Asylum', '2021-05-09T20:15:45.345875+01:00',
       'dedsc', '2022-05-09T20:15:45.345875+01:00', false, 'IA', '765324', 'Taylor House', 0, 0, '{"user": "userVal", "noteType": "noteTypeVal"}'::jsonb,
       '1', 'TestRegion', 'JUDICIAL', 'RESTRICTED', 'ASSIGNED', 'taskName', 'SELF', 'startAppeal', 'No Reason', 'title', 'workType', 'MANUAL');


INSERT INTO task_roles
(task_role_id, assignment_priority, authorisations, auto_assignable, cancel, created,
 "execute", manage, own, "read", refer, role_category, role_name, task_id)
VALUES('2d6cc5cf-c973-11eb-bdba-0242ac11000e', 0, '{SPECIFIC, BASIC}', false, false, '2021-05-09T20:15:45.345875+01:00',
       false, false, false, true, false, 'JUDICIAL', 'tribunal-caseofficer', '8d6cc5cf-c973-11eb-bdba-0242ac11001e');


