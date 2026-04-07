ALTER TABLE cft_task_db.tasks ADD COLUMN external_task_id TEXT;

CREATE UNIQUE INDEX uq_tasks_external_task_id_case_type_id
ON cft_task_db.tasks (external_task_id, case_type_id)
WHERE external_task_id IS NOT NULL AND case_type_id IS NOT NULL;
