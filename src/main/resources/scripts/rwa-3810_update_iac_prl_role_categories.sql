-- PRE DEPLOYMENT DATA PREPARATION STEPS
-----------------------------------------
select count(*) from cft_task_db.tasks where role_category is null;
-- 534
select task_id from cft_task_db.tasks where role_category is null;
--  take a backup of all the task_id's and copy to rollback script

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';
-- 414

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' group by task_name ;
-- count|task_name            |
-------+---------------------+
--   414|Review Hearing bundle|

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'PRIVATELAW';
-- 120

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'PRIVATELAW' group by task_name ;

--count|task_name              |
-------+-----------------------+
--  110|Hearing has been listed|
--    1|Produce hearing bundle |
--    7|Reply to the Message   |
--    2|Request Solicitor Order|

-- IMPLEMENTATION STEPS
-----------------------------

update cft_task_db.tasks set role_category = 'JUDICIAL' where role_category is null and jurisdiction = 'IA' and task_name in ('Review Hearing bundle');
-- Updated Rows = 414

update cft_task_db.tasks set role_category = 'ADMIN' where role_category is null and jurisdiction = 'PRIVATELAW' and task_name in ('Hearing has been listed', 'Produce hearing bundle', 'Reply to the Message', 'Request Solicitor Order');
-- Updated Rows = 120

-- POST IMPLEMENTATION VERIFICATION STEPS
--------------------------------------------

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_name in ('Review Hearing bundle');
-- 0

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'PRIVATELAW' and task_name in ('Hearing has been listed', 'Produce hearing bundle', 'Reply to the Message', 'Request Solicitor Order');
-- 0
