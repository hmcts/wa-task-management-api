 ALTER TABLE cft_task_db.task_history ADD COLUMN description Text;
 ALTER TABLE cft_task_db.task_history ADD COLUMN notes JSONB;
 ALTER TABLE cft_task_db.task_history ADD COLUMN region_name Text;
 ALTER TABLE cft_task_db.task_history ADD COLUMN location_name Text;
 ALTER TABLE cft_task_db.task_history ADD COLUMN additional_properties JSONB;
 ALTER TABLE cft_task_db.task_history ADD COLUMN reconfigure_request_time Timestamp;
 ALTER TABLE cft_task_db.task_history ADD COLUMN next_hearing_id Text;
 ALTER TABLE cft_task_db.task_history ADD COLUMN next_hearing_date Timestamp;
 ALTER TABLE cft_task_db.task_history ADD COLUMN priority_date Timestamp;
 ALTER TABLE cft_task_db.task_history ADD COLUMN last_reconfiguration_time Timestamp;

 ALTER TABLE cft_task_db.reportable_task ADD COLUMN description Text;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN notes JSONB;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN region_name Text;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN location_name Text;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN additional_properties JSONB;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN reconfigure_request_time Timestamp;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN next_hearing_id Text;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN next_hearing_date Timestamp;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN priority_date Timestamp;
 ALTER TABLE cft_task_db.reportable_task ADD COLUMN last_reconfiguration_time Timestamp;