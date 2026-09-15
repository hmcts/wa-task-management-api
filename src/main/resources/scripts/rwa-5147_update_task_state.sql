-- Update task state to TERMINATED for tasks that were TERMINATED earlier and are assigned again because of RACE condition

-- Connect to primary cft_task_db database

select state from cft_task_db.tasks where task_id in ('cd1a64db-8b66-11f1-b3a9-665fd9cbf882', '7a1e917f-90b5-11f1-91ad-d219b77039a4');
--state   |
----------+
--ASSIGNED|
--ASSIGNED|

--Create a backup table
create table IF NOT EXISTS cft_task_db.rwa_5147_task_ids_backup AS
select * from cft_task_db.tasks where task_id in ('cd1a64db-8b66-11f1-b3a9-665fd9cbf882', '7a1e917f-90b5-11f1-91ad-d219b77039a4');

update cft_task_db.tasks set state = 'TERMINATED', termination_reason = 'deleted', termination_process = 'EXUI_CASE_EVENT_CANCELLATION', last_updated_action = 'Terminate' where task_id in ('cd1a64db-8b66-11f1-b3a9-665fd9cbf882', '7a1e917f-90b5-11f1-91ad-d219b77039a4');


--Verfy the update
select state from cft_task_db.tasks where task_id in ('cd1a64db-8b66-11f1-b3a9-665fd9cbf882', '7a1e917f-90b5-11f1-91ad-d219b77039a4');
-- should be TERMINATED


-- drop the backup table if not needed
drop table if exists cft_task_db.rwa_5147_task_ids_backup;
