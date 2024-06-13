ALTER TABLE cft_task_db.task_assignments
  ALTER COLUMN location DROP NOT NULL;

ALTER TABLE cft_task_db.task_assignments
  ALTER COLUMN task_name DROP NOT NULL;


