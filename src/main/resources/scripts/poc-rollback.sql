-- Manual script to enable decentralisation-poc rollback in demo.

BEGIN;

-- Safe on replica as well: the unique index only exists on the primary migration path.
DROP INDEX IF EXISTS cft_task_db.uq_tasks_external_task_id_case_type_id;

ALTER TABLE cft_task_db.tasks
DROP
COLUMN IF EXISTS external_task_id;

DELETE
FROM cft_task_db.flyway_schema_history
WHERE version = '2.0.1'
  AND script = 'V2.0.1__decentralisation.sql';

COMMIT;
