DROP TABLE IF EXISTS sensitive_task_event_logs;
CREATE TABLE sensitive_task_event_logs
(
    id                      TEXT        NOT NULL,
    request_id              TEXT        NULL,
    correlation_id          TEXT        NULL,
    task_id                 TEXT        NOT NULL,
    case_id                 TEXT        NOT NULL,
    message                 TEXT        NOT NULL,
    user_data               JSONB       NOT NULL,
    task_data               JSONB       NOT NULL,
    expiry_time             TIMESTAMP   NOT NULL default CURRENT_TIMESTAMP + interval '90' day,
    log_event_time          TIMESTAMP   NOT NULL default CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

create index if not exists idx_sensitive_task_event_log_exp on cft_task_db.sensitive_task_event_logs using btree(expiry_time);

COMMIT;
