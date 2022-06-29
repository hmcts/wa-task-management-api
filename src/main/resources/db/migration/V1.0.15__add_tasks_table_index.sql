CREATE INDEX IF NOT EXISTS idx_region_state ON cft_task_db.tasks USING btree (region, state);
CREATE INDEX IF NOT EXISTS idx_task_case_id ON cft_task_db.tasks USING btree (case_id, case_type_id);
CREATE INDEX IF NOT EXISTS idx_tasks_role_category ON cft_task_db.tasks USING btree (role_category);
CREATE INDEX IF NOT EXISTS tasks_due_date_time_idx ON cft_task_db.tasks USING btree (due_date_time);
CREATE INDEX IF NOT EXISTS tasks_task_name_idx ON cft_task_db.tasks USING btree (task_name);

CREATE INDEX IF NOT EXISTS idx_cancel ON cft_task_db.task_roles USING btree (cancel);
CREATE INDEX IF NOT EXISTS idx_read ON cft_task_db.task_roles USING btree (read);
CREATE INDEX IF NOT EXISTS idx_read_own ON cft_task_db.task_roles USING btree (read, own);
CREATE INDEX IF NOT EXISTS idx_role_name ON cft_task_db.task_roles USING btree (role_name);
CREATE INDEX IF NOT EXISTS task_roles_read_idx ON cft_task_db.task_roles USING btree (read, authorizations, role_name);
CREATE INDEX IF NOT EXISTS idx_fk_task_id ON cft_task_db.task_roles USING btree (task_id);
