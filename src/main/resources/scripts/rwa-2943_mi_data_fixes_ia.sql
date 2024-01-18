
-- DATA CHANGE IMPLEMENTATION BY JURISDICTION - IA
---------------------------------------------------------------
-- PRE - CHECKS
----------------------
select count(*), state from tasks t  where jurisdiction  = 'IA' and created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by state
--285	    ASSIGNED
--101	    CANCELLED
--288	    COMPLETED
--494451	TERMINATED
--774	    UNASSIGNED

select count(*), state from reportable_task rt  where jurisdiction = 'IA' and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS'))  group by state
--278	  ASSIGNED
--20	  COMPLETED
--82468	TERMINATED
--743	  UNASSIGNED

 select  count(*), state, update_action  from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null or
  completed_date is null or completed_date_time is null or processing_time_days is null or processing_time is null or  handling_time_days is null or handling_time is null or
  due_date_to_completed_diff_days is null or due_date_to_completed_diff_time is null or is_within_sla is null or
  role_category_label is null or jurisdiction_label is null or case_type_label is null or state_label is null or final_state_label is null)
  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'TERMINATED' and jurisdiction = 'IA' group by state, update_action ;
--1	    TERMINATED	AutoAssign
--1   	TERMINATED	Configure
--33689	TERMINATED	Terminate
--33923	TERMINATED	TerminateException

 select count(*) from task_assignments ta where task_id in
    (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'
      and task_id in (select distinct task_id from task_history th  where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')))
-- 74,795

-- IMPLEMENTATION IN BATCHES
--------------------------------
CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'IA',null,null, '2022-10-20 16:00:00'::timestamp, '2015-09-18 16:00:00'::timestamp);

CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'IA',null,null, '2023-03-10 16:00:00'::timestamp, '2022-10-19 16:00:00'::timestamp);

CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'IA',null,null, '2023-06-20 16:00:00'::timestamp, '2023-03-09 16:00:00'::timestamp);

CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'IA',null,null, '2023-07-20 16:00:00'::timestamp, '2023-06-19 16:00:00'::timestamp);

CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'IA',null,null, '2023-09-18 16:00:00'::timestamp, '2023-07-19 16:00:00'::timestamp);

-- POST - CHECKS
----------------------

select count(*), state from tasks  where jurisdiction = 'IA' and report_refresh_request_time is not null and created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by state
--    Results should be like below
--285	    ASSIGNED
--101	    CANCELLED
--288	    COMPLETED
--494451	TERMINATED
--774	    UNASSIGNED

select count(*), state from reportable_task rt where jurisdiction = 'IA' and report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')) group by state
--    Results should be like below
--278	  ASSIGNED
--20	  COMPLETED
--82468	TERMINATED
--743	  UNASSIGNED

select count(*) from task_assignments ta  where report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA')
--    Results should be like below
-- 74,795
