--Connect to replica database and run below queries
-- Update outcome to null for the tasks in cft_task_db.cft_task_db.reportable_task table where task_id is in the backup tables created for outcome update to Cancelled/Completed
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = null where task_id in (select task_id from cft_task_db.cft_task_db.tasks_to_update_outcome_1);
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = null where task_id in (select task_id from cft_task_db.cft_task_db.tasks_to_update_outcome_2);
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = null where task_id in (select task_id from cft_task_db.cft_task_db.tasks_to_update_outcome_3);
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = null where task_id in (select task_id from cft_task_db.cft_task_db.tasks_to_update_outcome_4);
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = null where task_id in (select task_id from cft_task_db.cft_task_db.tasks_to_update_outcome_5);

-- Get the count of terminated/completed/cancelled tasks where outcome is null
select count(*) from cft_task_db.cft_task_db.reportable_task rt where outcome is null and state in ('TERMINATED', 'COMPLETED', 'CANCELLED');
-- 3133234

-- Drop backup tables
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_1;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_2;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_3;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_4;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_5;

-- End of script




