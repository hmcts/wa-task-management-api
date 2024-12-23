CREATE INDEX IF NOT EXISTS idx_tasks_case_id ON cft_task_db.tasks USING btree (case_id);
