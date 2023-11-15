
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

-- DATA CHANGE IMPLEMENTATION BY JURISDICTION - IA
---------------------------------------------------------------

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
