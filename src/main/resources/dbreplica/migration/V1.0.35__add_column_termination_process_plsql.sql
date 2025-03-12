ALTER TABLE cft_task_db.task_history ADD COLUMN termination_process TEXT;
ALTER TABLE cft_task_db.reportable_task ADD COLUMN termination_process TEXT;
ALTER TABLE cft_task_db.tasks ADD COLUMN termination_process TEXT;
