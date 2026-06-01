/*
 * Build replacement indexes without blocking writes on tasks. Keep the legacy
 * GIN index available until all replacement indexes have finished building.
 *
 * Every statement in this migration is deliberately non-transactional because
 * PostgreSQL does not allow CONCURRENTLY inside a transaction block.
 */

CREATE INDEX CONCURRENTLY search_filter_signature_hashes_idx
    ON cft_task_db.tasks USING gist (filter_signature_hashes gist__intbig_ops)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_role_signature_hashes_idx
    ON cft_task_db.tasks USING gist (role_signature_hashes gist__intbig_ops)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

CREATE INDEX CONCURRENTLY search_assignee_idx
    ON cft_task_db.tasks USING btree (assignee)
    WHERE state IN ('ASSIGNED', 'UNASSIGNED') AND indexed;

DROP INDEX CONCURRENTLY IF EXISTS cft_task_db.search_index;
