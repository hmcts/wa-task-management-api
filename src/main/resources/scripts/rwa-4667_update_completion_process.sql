--Connect to main database
select count(*) from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- 8

--Create a backup of all the task's and copy to rollback script
create table IF NOT EXISTS cft_task_db.sscs_completed_tasks_ids_backup AS
select task_id from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';

-- Check count of records in backup table
select count(*) from cft_task_db.sscs_completed_tasks_ids_backup;
-- 8

update cft_task_db.tasks set termination_reason = 'RWA-4667' where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- Updated Rows = 8

-- Verify the update
select count(*) from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'completed';
-- 0

-- Verify the new termination reason
select count(*) from cft_task_db.tasks where jurisdiction = 'SSCS' and state in ('COMPLETED','TERMINATED') and termination_reason = 'RWA-4667';
-- 8

-- Drop the backup table if not needed
drop table if exists cft_task_db.sscs_completed_tasks_ids_backup;


