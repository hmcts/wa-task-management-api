--check count of rows in backup table
select count(*) from cft_task_db.cft_task_db.automated_completed_tasks_ids_backup;
-- more than 0

select count(*) from cft_task_db.cft_task_db.manual_completed_tasks_ids_backup;
-- more than 0

select count(*) from cft_task_db.cft_task_db.automated_cancelled_tasks_ids_backup;
-- more than 0

select count(*) from cft_task_db.cft_task_db.manual_cancelled_tasks_ids_backup;
-- more than 0

update cft_task_db.tasks set termination_process = 'Manual' where task_id in (select task_id from cft_task_db.cft_task_db.manual_completed_tasks_ids_backup);
-- Updated Rows greater than 0

update cft_task_db.tasks set termination_process = 'Automated' where task_id in (select task_id from cft_task_db.cft_task_db.automated_completed_tasks_ids_backup);
-- Updated Rows greater than 0

update cft_task_db.tasks set termination_process = null and termination_process_label= null where task_id in (select task_id from cft_task_db.cft_task_db.automated_cancelled_tasks_ids_backup);
-- Updated Rows greater than 0

update cft_task_db.tasks set termination_process = null and termination_process_label= null where task_id in (select task_id from cft_task_db.cft_task_db.manual_cancelled_tasks_ids_backup);
-- Updated Rows greater than 0


-- Verify the update
select count(*) from cft_task_db.cft_task_db.reportable_task where termination_process_label in ('Automated', 'Manual') and termination_process in ('EXUI_CASE_EVENT_COMPLETION', 'EXUI_USER_COMPLETION');
--greater than  0

select count(*) from cft_task_db.cft_task_db.reportable_task where final_state_label ='USER_CANCELLATION' and termination_process is null  and outcome = 'Cancelled';
-- greater than 0

select count(*) from cft_task_db.cft_task_db.reportable_task where final_state_label is null and termination_process is null and agent_name = 'aeabbbcb-4909-4346-acd5-4e95df4357a3' and outcome = 'Cancelled';
-- greater than 0

--Drop the backup tables if not needed
drop table if exists cft_task_db.cft_task_db.automated_completed_tasks_ids_backup;
drop table if exists cft_task_db.cft_task_db.manual_completed_tasks_ids_backup;
drop table if exists cft_task_db.cft_task_db.automated_cancelled_tasks_ids_backup;
drop table if exists cft_task_db.cft_task_db.manual_cancelled_tasks_ids_backup;

