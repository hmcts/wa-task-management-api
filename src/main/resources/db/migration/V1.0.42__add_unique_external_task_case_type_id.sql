ALTER TABLE cft_task_db.tasks
ADD CONSTRAINT uq_tasks_external_task_id_case_type_id
UNIQUE (external_task_id, case_type_id);
