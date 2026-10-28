-- Rollback script for RWA-5147 update task state
-- Restore the values changed by the update script from the backup table

update cft_task_db.tasks t
set state = backup.state,
    termination_reason = backup.termination_reason,
    termination_process = backup.termination_process,
    last_updated_action = backup.last_updated_action
from cft_task_db.rwa_5147_task_ids_backup as backup
where t.task_id = backup.task_id;

-- Verify state of the tasks after rollback
select state from cft_task_db.tasks where task_id in ('cd1a64db-8b66-11f1-b3a9-665fd9cbf882', '7a1e917f-90b5-11f1-91ad-d219b77039a4');
-- should be ASSIGNED

-- Drop the backup table if not needed
drop table if exists cft_task_db.rwa_5147_task_ids_backup;
