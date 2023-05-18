/*
 To be removed : expiry date is 1 day ago
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
values(
    'f5db6fcf-b470-419d-bf26-ebfecea53b4d','000000000001','000000000001','000000000001','000000000001','some_message','{}','[{}]',
    (CURRENT_TIMESTAMP - '1 days'::interval day),
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );

/*
 To be removed : expiry date is 10 days ago
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
VALUES(
    '13bb163b-57d0-43cd-8d05-5e21a9bcd47e', '000000000002', '000000000002', '000000000002', '000000000002', 'some_message', '{}', '[{}]',
    (CURRENT_TIMESTAMP - '10 days'::interval day),
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );

/*
 To be stayed : expiry date is 1 hour later
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
VALUES(
    '64b6b676-6fc0-4281-96f9-5fe4b237ea06', '000000000003', '000000000003', '000000000003', '000000000003', 'some_message', '{}', '[{
                                                                                                               "taskId" : "task_id",
                                                                                                               "taskName" : "someTaskName",
                                                                                                               "taskType" : "someTaskType",
                                                                                                               "dueDateTime" : null,
                                                                                                               "state" : "UNASSIGNED",
                                                                                                               "taskSystem" : null,
                                                                                                               "securityClassification" : null,
                                                                                                               "title" : null,
                                                                                                               "description" : null,
                                                                                                               "notes" : [ {
                                                                                                                 "code" : "Code1",
                                                                                                                 "noteType" : "WARNING",
                                                                                                                 "userId" : null,
                                                                                                                 "content" : "Text1"
                                                                                                               } ],
                                                                                                               "majorPriority" : null,
                                                                                                               "minorPriority" : null,
                                                                                                               "assignee" : null,
                                                                                                               "autoAssigned" : false,
                                                                                                               "workTypeResource" : null,
                                                                                                               "roleCategory" : null,
                                                                                                               "hasWarnings" : false,
                                                                                                               "assignmentExpiry" : null,
                                                                                                               "caseId" : null,
                                                                                                               "caseTypeId" : null,
                                                                                                               "caseName" : null,
                                                                                                               "caseCategory" : null,
                                                                                                               "jurisdiction" : null,
                                                                                                               "region" : null,
                                                                                                               "regionName" : null,
                                                                                                               "location" : null,
                                                                                                               "locationName" : null,
                                                                                                               "businessContext" : null,
                                                                                                               "terminationReason" : null,
                                                                                                               "created" : null,
                                                                                                               "executionTypeCode" : null,
                                                                                                               "taskRoleResources" : null,
                                                                                                               "additionalProperties" : null,
                                                                                                               "reconfigureRequestTime" : null,
                                                                                                               "lastReconfigurationTime" : null,
                                                                                                               "nextHearingId" : null,
                                                                                                               "nextHearingDate" : null,
                                                                                                               "priorityDate" : null,
                                                                                                               "lastUpdatedTimestamp" : null,
                                                                                                               "lastUpdatedUser" : null,
                                                                                                               "lastUpdatedAction" : null,
                                                                                                               "indexed" : false
                                                                                                             }]',
    (CURRENT_TIMESTAMP + '1 hours'::interval hour),
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );


/*
 To be removed : expiry date is now
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
VALUES(
    '231133e3-3e88-41d0-a8be-450cc4dba960', '000000000004', '000000000004', '000000000004', '000000000004', 'some_message', '{}', '[{}]',
    CURRENT_TIMESTAMP ,
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );

