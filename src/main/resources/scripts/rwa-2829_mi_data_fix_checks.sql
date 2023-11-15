-- PRE - IMPLEMENTATION CHECKS
-------------------------------------------
select count(*)  from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS');
-- 511,502

select count(*), jurisdiction  from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by jurisdiction
-- 14765	CIVIL
-- 495899	IA
-- 838	  PRIVATELAW

select count(*) from reportable_task rt  where task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS'))
-- 88,471 (tasks to be marked)

select count(*), state from reportable_task rt  where task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS'))  group by state
-- 426	  ASSIGNED    (tasks to be marked)
-- 20   	COMPLETED   (tasks to be marked)
-- 87053	TERMINATED  (tasks to be marked)
-- 972	  UNASSIGNED  (tasks to be marked)

select count(*), state, update_action  from reportable_task rt  where task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')) and state = 'TERMINATED'  group by state, update_action
-- 1	    TERMINATED	AutoAssign
-- 1	    TERMINATED	Configure
-- 53110	TERMINATED	Terminate
-- 33986	TERMINATED	TerminateException

select count(*), jurisdiction, state from tasks t  where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by jurisdiction, state
--69	    CIVIL	      ASSIGNED
--2	      CIVIL	      COMPLETED
--14567	  CIVIL	      TERMINATED
--127	    CIVIL	      UNASSIGNED
--371	    IA	        ASSIGNED
--101	    IA	        CANCELLED
--288	    IA	        COMPLETED
--494293	IA	        TERMINATED
--846	    IA	        UNASSIGNED
--3	      PRIVATELAW	ASSIGNED
--803  	  PRIVATELAW	TERMINATED
--32  	  PRIVATELAW	UNASSIGNED

select count(*), jurisdiction, state from reportable_task rt  where task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS'))  group by jurisdiction, state
-- 56    	CIVIL	      ASSIGNED
-- 4560  	CIVIL	      TERMINATED
-- 127	  CIVIL	      UNASSIGNED
-- 367	  IA	        ASSIGNED
-- 20	    IA        	COMPLETED
-- 82309	IA	        TERMINATED
-- 813	  IA	        UNASSIGNED
-- 3	    PRIVATELAW	ASSIGNED
-- 185	  PRIVATELAW	TERMINATED
-- 31   	PRIVATELAW	UNASSIGNED

select  count(*)  from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null)
  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'ASSIGNED';
-- 0 (all data populated correctly)

 select  count(*) from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null or
  completed_date is null or completed_date_time is null or processing_time_days is null or processing_time is null or  handling_time_days is null or handling_time is null or
  due_date_to_completed_diff_days is null or due_date_to_completed_diff_time is null or is_within_sla is null or
  role_category_label is null or jurisdiction_label is null or case_type_label is null or state_label is null or final_state_label is null)
  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'TERMINATED';
-- 71,405 (terminated tasks with missing data)

 select  count(*), update_action, termination_reason from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null or
  completed_date is null or completed_date_time is null or processing_time_days is null or processing_time is null or  handling_time_days is null or handling_time is null or
  due_date_to_completed_diff_days is null or due_date_to_completed_diff_time is null or is_within_sla is null or
  role_category_label is null or jurisdiction_label is null or case_type_label is null or state_label is null or final_state_label is null)
  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'TERMINATED' group by update_action, termination_reason;
-- 1	    AutoAssign          [NULL]
-- 1	    Configure           [NULL]
-- 37421	Terminate	          completed
-- 33982	TerminateException	deleted

select  count(*) from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null or
  completed_date is null or completed_date_time is null or processing_time_days is null or processing_time is null or  handling_time_days is null or handling_time is null or
  due_date_to_completed_diff_days is null or due_date_to_completed_diff_time is null or is_within_sla is null or
  role_category_label is null or jurisdiction_label is null or case_type_label is null or state_label is null or final_state_label is null)
  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'COMPLETED';
-- 4

 select  count(*), update_action, termination_reason from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null or
  completed_date is null or completed_date_time is null or processing_time_days is null or processing_time is null or  handling_time_days is null or handling_time is null or
  due_date_to_completed_diff_days is null or due_date_to_completed_diff_time is null or is_within_sla is null or
  role_category_label is null or jurisdiction_label is null or case_type_label is null or state_label is null or final_state_label is null)
  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'COMPLETED' group by update_action, termination_reason;
 -- 2	AddWarning	 [NULL]
 -- 2	Complete	   completed

 select count(*) from task_assignments ta where task_id in
   (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'PRIVATELAW'
     and task_id in (select distinct task_id from task_history th  where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')))
 -- 374

 select count(*) from task_assignments ta where task_id in
    (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'CIVIL'
      and task_id in (select distinct task_id from task_history th  where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')))
  -- 5,674

 select count(*) from task_assignments ta where task_id in
    (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'
      and task_id in (select distinct task_id from task_history th  where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')))
  -- 74,697

-- DATA CHANGE IMPLEMENTATION BY JURISDICTION - PRIVATELAW
---------------------------------------------------------------
CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'PRIVATELAW',null,null, '2023-09-18 16:00:00'::timestamp);

select count(*), state from tasks  where jurisdiction = 'PRIVATELAW' and report_refresh_request_time is not null and created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by state
--    Results should be like below
--3	      PRIVATELAW	ASSIGNED
--803  	  PRIVATELAW	TERMINATED
--32  	  PRIVATELAW	UNASSIGNED

select count(*), state from reportable_task rt where jurisdiction = 'PRIVATELAW' and report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')) group by state
--    Results should be like below
-- 3	    PRIVATELAW	ASSIGNED
-- 185	  PRIVATELAW	TERMINATED
-- 31   	PRIVATELAW	UNASSIGNED

select count(*) from task_assignments ta  where report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'PRIVATELAW')
--    Results should be like below
-- 374

select first_assigned_date, first_assigned_date_time, wait_time_days, wait_time, role_category_label, jurisdiction_label, case_type_label,  state_label, report_refresh_time  from reportable_task rt  where jurisdiction = 'PRIVATELAW' and state = 'ASSIGNED' and report_refresh_time is not null order by report_refresh_time desc
--  Inspect results for non nulls

select first_assigned_date, first_assigned_date_time, wait_time_days, wait_time, completed_date, completed_date_time, processing_time_days, processing_time,handling_time_days, handling_time, due_date_to_completed_diff_days, due_date_to_completed_diff_time, is_within_sla,role_category_label, jurisdiction_label, case_type_label,  state_label, final_state_label, report_refresh_time  from reportable_task rt  where jurisdiction = 'PRIVATELAW' and state = 'TERMINATED' and termination_reason = 'completed' and report_refresh_time is not null order by report_refresh_time desc
-- Inspect results for non nulls


-- DATA CHANGE IMPLEMENTATION BY JURISDICTION - CIVIL
---------------------------------------------------------------
CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'CIVIL',null,null, '2023-09-18 16:00:00'::timestamp);

select count(*), state from tasks  where jurisdiction = 'CIVIL' and report_refresh_request_time is not null and created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by state
--    Results should be like below
--69	    CIVIL	      ASSIGNED
--2	      CIVIL	      COMPLETED
--14567	  CIVIL	      TERMINATED
--127	    CIVIL	      UNASSIGNED

select count(*), state from reportable_task rt where jurisdiction = 'CIVIL' and report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')) group by state
--    Results should be like below
-- 56    	CIVIL	      ASSIGNED
-- 4560  	CIVIL	      TERMINATED
-- 127	  CIVIL	      UNASSIGNED

select count(*) from task_assignments ta  where report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'CIVIL')
--    Results should be like below
-- 5,674

select first_assigned_date, first_assigned_date_time, wait_time_days, wait_time, role_category_label, jurisdiction_label, case_type_label,  state_label, report_refresh_time  from reportable_task rt  where jurisdiction = 'CIVIL' and state = 'ASSIGNED' and report_refresh_time is not null order by report_refresh_time desc
--  Inspect results for non nulls

select first_assigned_date, first_assigned_date_time, wait_time_days, wait_time, completed_date, completed_date_time, processing_time_days, processing_time,handling_time_days, handling_time, due_date_to_completed_diff_days, due_date_to_completed_diff_time, is_within_sla,role_category_label, jurisdiction_label, case_type_label,  state_label, final_state_label, report_refresh_time  from reportable_task rt  where jurisdiction = 'CIVIL' and state = 'TERMINATED' and termination_reason = 'completed' and report_refresh_time is not null order by report_refresh_time desc
-- Inspect results for non nulls


-- DATA CHANGE IMPLEMENTATION BY JURISDICTION - IA
---------------------------------------------------------------
-- Some dates to pick from
select count(*) from tasks where created < to_timestamp('2022-10-20 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'  -- 99,584
select count(*) from reportable_task rt  where task_id in (select task_id  from tasks where created < to_timestamp('2022-10-20 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA') -- 31

select count(*) from tasks where created < to_timestamp('2023-03-10 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'  -- 254,232
select count(*) from reportable_task rt  where task_id in (select task_id  from tasks where created < to_timestamp('2023-03-10 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA') -- 1,124

select count(*) from tasks where created < to_timestamp('2023-06-20 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'  -- 371,224
select count(*) from reportable_task rt  where task_id in (select task_id  from tasks where created < to_timestamp('2023-06-20 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA') -- 2,961

select count(*) from tasks where created < to_timestamp('2023-07-20 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'  -- 412,202
select count(*) from reportable_task rt  where task_id in (select task_id  from tasks where created < to_timestamp('2023-07-20 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA') -- 10,590

select count(*) from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA'  -- 495,899
select count(*) from reportable_task rt  where task_id in (select task_id  from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA') -- 83,509
-- end of date pick up


CALL cft_task_db.mark_report_tasks_for_refresh(null,null,'IA',null,null, '2023-09-18 16:00:00'::timestamp);

select count(*), state from tasks  where jurisdiction = 'IA' and report_refresh_request_time is not null and created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') group by state
--    Results should be like below
--371	    IA	        ASSIGNED
--101	    IA	        CANCELLED
--288	    IA	        COMPLETED
--494293	IA	        TERMINATED
--846	    IA	        UNASSIGNED

select count(*), state from reportable_task rt where jurisdiction = 'IA' and report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS')) group by state
--    Results should be like below
-- 367	  IA	        ASSIGNED
-- 20	    IA        	COMPLETED
-- 82309	IA	        TERMINATED
-- 813	  IA	        UNASSIGNED

select count(*) from task_assignments ta  where report_refresh_time is not null and task_id in (select task_id from tasks where created < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and jurisdiction = 'IA')
--    Results should be like below
-- 74,697

select first_assigned_date, first_assigned_date_time, wait_time_days, wait_time, role_category_label, jurisdiction_label, case_type_label,  state_label, report_refresh_time  from reportable_task rt  where jurisdiction = 'IA' and state = 'ASSIGNED' and report_refresh_time is not null order by report_refresh_time desc
--  Inspect results for non nulls

select first_assigned_date, first_assigned_date_time, wait_time_days, wait_time, completed_date, completed_date_time, processing_time_days, processing_time,handling_time_days, handling_time, due_date_to_completed_diff_days, due_date_to_completed_diff_time, is_within_sla,role_category_label, jurisdiction_label, case_type_label,  state_label, final_state_label, report_refresh_time  from reportable_task rt  where jurisdiction = 'IA' and state = 'TERMINATED' and termination_reason = 'completed' and report_refresh_time is not null order by report_refresh_time desc
-- Inspect results for non nulls


-- EXTRA CHECKS - POST IMPLEMENTATION
---------------------------------------------------------------

select  count(*)  from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null) and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'ASSIGNED';
-- 0

select  count(*), state, update_action  from cft_task_db.reportable_task rt where (first_assigned_date is null or first_assigned_date_time is null or wait_time_days is null or wait_time is null or completed_date is null or completed_date_time is null or processing_time_days is null or processing_time is null or  handling_time_days is null or handling_time is null or due_date_to_completed_diff_days is null or due_date_to_completed_diff_time is null or is_within_sla is null or role_category_label is null or jurisdiction_label is null or case_type_label is null or state_label is null or final_state_label is null)  and created  < to_timestamp('2023-09-18 16:00:00','YYYY-MM-DD HH24:MI:SS') and state = 'TERMINATED' group by state, update_action ;
-- Results should be like below
-- 1     TERMINATED AutoAssign
-- 1     TERMINATED Configure
-- 0     TERMINATED Terminate
