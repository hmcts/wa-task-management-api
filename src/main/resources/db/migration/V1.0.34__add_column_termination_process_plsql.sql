ALTER TABLE cft_task_db.tasks ADD COLUMN termination_process TEXT
  CHECK (
  termination_process IS NULL OR
  termination_process IN ('EXUI_USER_COMPLETION', 'EXUI_CASE-EVENT_COMPLETION')
  );
