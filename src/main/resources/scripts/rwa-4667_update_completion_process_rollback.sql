--check count of rows in backup table
select count(*) from cft_task_db.sscs_completed_tasks_ids_backup and jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- 8

update cft_task_db.tasks set termination_reason = 'completed' where task_id in (select task_id from cft_task_db.cft_task_db.sscs_completed_tasks_ids_backup);
-- Updated Rows = 8

-- Verify the update
select count(*) from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'https://tools.hmcts.net/jira/browse/RWA-4667';
-- 0

--Drop the backup table if not needed
drop table if exists cft_task_db.sscs_completed_tasks_ids_backup;

