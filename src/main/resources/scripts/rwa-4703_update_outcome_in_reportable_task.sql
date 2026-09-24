--Connect to replica database and run below queries
-- Get count of tasks in completed/cancelled/terminated state from cft_task_db.cft_task_db.reportable_task table where outcome is null
select count(*) from cft_task_db.cft_task_db.reportable_task rt where outcome is null and state in ('TERMINATED', 'COMPLETED', 'CANCELLED');
-- 3133234

-- Get count of tasks in cft_task_db.cft_task_db.reportable_task table with outcome is null and final_state_label is null and state = 'TERMINATED' and update_action = 'TerminateException'
-- We can set outcome to cancelled for these tasks as update_action is TerminateException for auto cancel tasks before the changes made in Dec 2025 in TaskManagementService.
select count(*)  from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action = 'TerminateException';
-- 135126

-- Create a backup table to save these task id's
CREATE TABLE IF NOT EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_1 AS
select task_id from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action = 'TerminateException';


--Get the count of the tasks that need to be updated which have outcome is null and final_state_label is null and state = 'TERMINATED' and update_action != 'TerminateException'
-- We can update outcome to Cancelled for these tasks as well as I checked the data in task_history table and can see that there is no event for COMPLETED. So they are Auto Cancelled tasks.
select count(*)  from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action != 'TerminateException';
-- 626

-- Create a backup table to save these task id's
CREATE TABLE IF NOT EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_2 AS
select task_id from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action != 'TerminateException';


-- Get the count  of task id's that will be updated in the cft_task_db.cft_task_db.reportable_task table for outcome update to Cancelled where final_state_label is USER_CANCELLED
select count(*) from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label = 'USER_CANCELLED' and state = 'TERMINATED';
-- 255329

-- Create a backup table to save these task id's
CREATE TABLE IF NOT EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_3 AS
select task_id from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label = 'USER_CANCELLED' and state = 'TERMINATED';

--Get the count of task id's that will be updated in the cft_task_db.cft_task_db.reportable_task table for outcome update to Completed where final_state_label is COMPLETED
select count(*) from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label = 'COMPLETED' and state = 'TERMINATED';
-- 2739032

-- Create a backup table to save these task id's
CREATE TABLE IF NOT EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_4 AS
select task_id from cft_task_db.cft_task_db.reportable_task rt where outcome is null and final_state_label = 'COMPLETED' and state = 'TERMINATED';

-- Get the count of task id's that will be updated in the cft_task_db.cft_task_db.reportable_task table for outcome update to Completed/Cancelled where state in ('COMPLETED', 'CANCELLED')
-- These tasks are not terminated for some reason but are Completed or Cancelled
select count(*) from cft_task_db.cft_task_db.reportable_task rt where outcome is null and state in ('COMPLETED', 'CANCELLED');
-- 3121

-- Create a backup table to save these task id's
CREATE TABLE IF NOT EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_5 AS
select task_id from cft_task_db.cft_task_db.reportable_task rt where outcome is null and state in ('COMPLETED', 'CANCELLED');

-- Update the outcome to Cancelled for the tasks in cft_task_db.cft_task_db.reportable_task table where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action = 'TerminateException'
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = 'Cancelled' where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action = 'TerminateException';

-- Update the outcome to Cancelled for the tasks in cft_task_db.cft_task_db.reportable_task table where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action != 'TerminateException'
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = 'Cancelled' where outcome is null and final_state_label is null and state = 'TERMINATED' and update_action != 'TerminateException';


-- Update the outcome to Cancelled for the tasks in cft_task_db.cft_task_db.reportable_task table where outcome is null and final_state_label = 'USER_CANCELLED' and state = 'TERMINATED'
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = 'Cancelled' where outcome is null and final_state_label = 'USER_CANCELLED' and state = 'TERMINATED';

-- Update the outcome to Completed for the tasks in cft_task_db.cft_task_db.reportable_task table where outcome is null and final_state_label = 'COMPLETED' and state = 'TERMINATED'
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = 'Completed' where outcome is null and final_state_label = 'COMPLETED' and state = 'TERMINATED';

-- Update the outcome to Completed for the tasks in cft_task_db.cft_task_db.reportable_task table where outcome is null and state = 'COMPLETED' i.e task is not terminated but completed
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = 'Completed' where outcome is null and state = 'COMPLETED';

-- Update the outcome to Cancelled for the tasks in cft_task_db.cft_task_db.reportable_task table where outcome is null and state = 'CANCELLED' i.e task is not terminated but cancelled
UPDATE cft_task_db.cft_task_db.reportable_task rt set outcome = 'Cancelled' where outcome is null and state = 'CANCELLED';

-- Verify the update by getting the count of tasks in completed/cancelled/terminated state from cft_task_db.cft_task_db.reportable_task table where outcome is null
select count(*) from cft_task_db.cft_task_db.reportable_task rt where outcome is null and state in ('TERMINATED', 'COMPLETED', 'CANCELLED');
-- 0

-- drop the backup tables after the update is done
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_1;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_2;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_3;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_4;
DROP TABLE IF EXISTS cft_task_db.cft_task_db.tasks_to_update_outcome_5;
-- End of script




