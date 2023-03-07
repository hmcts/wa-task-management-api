/*
 To be removed : expiry date is 1 day ago
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
values(
    '000000000001','000000000001','000000000001','000000000001','000000000001','some_message','[{}]','[{}]',
    (CURRENT_TIMESTAMP - '1 days'::interval day),
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );

/*
 To be removed : expiry date is 10 days ago
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
VALUES(
    '000000000002', '000000000002', '000000000002', '000000000002', '000000000002', 'some_message', '[{}]', '[{}]',
    (CURRENT_TIMESTAMP - '10 days'::interval day),
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );

/*
 To be stayed : expiry date is 1 hour later
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
VALUES(
    '000000000003', '000000000003', '000000000003', '000000000003', '000000000003', 'some_message', '[{}]', '[{}]',
    (CURRENT_TIMESTAMP + '1 hours'::interval hour),
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );


/*
 To be removed : expiry date is now
 */
INSERT INTO cft_task_db.sensitive_task_event_logs
(id, request_id, correlation_id, task_id, case_id, message, user_data, task_data, expiry_time, log_event_time)
VALUES(
    '000000000004', '000000000004', '000000000004', '000000000004', '000000000004', 'some_message', '[{}]', '[{}]',
    CURRENT_TIMESTAMP ,
    (CURRENT_TIMESTAMP - '90 days'::interval day)
    );

