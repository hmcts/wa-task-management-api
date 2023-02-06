CREATE INDEX IF NOT EXISTS idx_manage ON cft_task_db.task_roles USING btree (manage);
CREATE INDEX IF NOT EXISTS idx_own_claim ON cft_task_db.task_roles USING btree (own, claim);
