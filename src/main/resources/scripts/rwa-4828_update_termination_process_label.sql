--Connect to main database

--Get count of tasks where termination_process_label needs to be updated for Automated completed tasks
select count(*) from cft_task_db.cft_task_db.reportable_task where termination_process_label = 'Automated' and termination_process = 'EXUI_CASE_EVENT_COMPLETION';
-- will be more than 0

--Get count of tasks where termination_process_label needs to be updated for Manual completed tasks
select count(*) from cft_task_db.cft_task_db.reportable_task where termination_process_label = 'Manual' and termination_process = 'EXUI_USER_COMPLETION';
-- will be more than 0


-- Get count of tasks where termination_process and termination_process_label updated for User cancelled tasks
select count(*) from cft_task_db.cft_task_db.reportable_task where final_state_label ='USER_CANCELLATION' and termination_process is null  and outcome = 'Cancelled';

-- Get count of tasks where termination_process and termination_process_label updated for System cancelled tasks
select count(*) from cft_task_db.cft_task_db.reportable_task where final_state_label is null and termination_process is null and agent_name = 'aeabbbcb-4909-4346-acd5-4e95df4357a3' and outcome = 'Cancelled';


--Create a backup tables with task id for all the tasks that need to be updated
create table IF NOT EXISTS cft_task_db.cft_task_db.automated_completed_tasks_ids_backup AS
select task_id from cft_task_db.cft_task_db.reportable_tasks where termination_process_label = 'Automated' and termination_process = 'EXUI_CASE_EVENT_COMPLETION';

create table IF NOT EXISTS cft_task_db.cft_task_db.manual_completed_tasks_ids_backup AS
select task_id from cft_task_db.cft_task_db.reportable_tasks where termination_process_label = 'Manual' and termination_process = 'EXUI_USER_COMPLETION';

create table IF NOT EXISTS cft_task_db.cft_task_db.automated_cancelled_tasks_ids_backup AS
select task_id from cft_task_db.cft_task_db.reportable_tasks where final_state_label is null and termination_process is null and agent_name = 'aeabbbcb-4909-4346-acd5-4e95df4357a3' and outcome = 'Cancelled';

create table IF NOT EXISTS cft_task_db.cft_task_db.manual_cancelled_tasks_ids_backup AS
select task_id from cft_task_db.cft_task_db.reportable_tasks where final_state_label ='USER_CANCELLATION' and termination_process is null  and outcome = 'Cancelled';

-- Check count of records in backup tables
select count(*) from cft_task_db.cft_task_db.automated_completed_tasks_ids_backup;

select count(*) from cft_task_db.cft_task_db.manual_completed_tasks_ids_backup;

select count(*) from cft_task_db.cft_task_db.automated_cancelled_tasks_ids_backup;

select count(*) from cft_task_db.cft_task_db.manual_cancelled_tasks_ids_backup;

update cft_task_db.cft_task_db.reportable_task set termination_process_label = 'Automated Completion' where  termination_process_label = 'Automated' and termination_process = 'EXUI_CASE_EVENT_COMPLETION';

update cft_task_db.cft_task_db.reportable_task set termination_process_label = 'Manual Completion' where  termination_process_label = 'Manual' and termination_process = 'EXUI_USER_COMPLETION';

update cft_task_db.cft_task_db.reportable_task set termination_process_label = 'Automated Cancellation' and termination_process = 'EXUI_CASE_EVENT_CANCELLATION' where final_state_label is null and termination_process is null and agent_name = 'aeabbbcb-4909-4346-acd5-4e95df4357a3' and outcome = 'Cancelled';

update cft_task_db.cft_task_db.reportable_task set termination_process_label = 'Manual Cancellation' and termination_process = 'EXUI_USER_CANCELLATION' where final_state_label ='USER_CANCELLATION' and termination_process is null  and outcome = 'Cancelled';
select count(*) from cft_task_db.cft_task_db.reportable_task where termination_process_label in ('Automated', 'Manual') and termination_process in ('EXUI_CASE_EVENT_COMPLETION', 'EXUI_USER_COMPLETION');
-- 0

select count(*) from cft_task_db.cft_task_db.reportable_task where final_state_label ='USER_CANCELLATION' and termination_process is null  and outcome = 'Cancelled';
-- 0
select count(*) from cft_task_db.cft_task_db.reportable_task where final_state_label is null and termination_process is null and agent_name = 'aeabbbcb-4909-4346-acd5-4e95df4357a3' and outcome = 'Cancelled';
-- 0
-- Drop the backup table if not needed
drop table if exists cft_task_db.cft_task_db.automated_completed_tasks_ids_backup;
drop table if exists cft_task_db.cft_task_db.manual_completed_tasks_ids_backup;
drop table if exists cft_task_db.cft_task_db.automated_cancelled_tasks_ids_backup;
drop table if exists cft_task_db.cft_task_db.manual_cancelled_tasks_ids_backup;


