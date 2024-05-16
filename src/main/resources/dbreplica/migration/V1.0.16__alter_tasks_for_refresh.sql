ALTER TABLE cft_task_db.tasks ADD COLUMN report_refresh_request_time Timestamp;
ALTER TABLE cft_task_db.reportable_task ADD COLUMN report_refresh_time Timestamp;
ALTER TABLE cft_task_db.task_assignments ADD COLUMN report_refresh_time Timestamp;
