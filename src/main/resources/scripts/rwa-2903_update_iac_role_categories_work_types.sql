-- PRE DEPLOYMENT DATA PREPARATION STEPS
 -----------------------------------------
 -- PRE DEPLOYMENT DATA PREPARATION STEPS volumes calculated 07/05/24

 select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';

--count|
-------+
--39846

--  take a backup of all the task_id's into a CSV file OR create a temporary table as below
CREATE TABLE tmp_task_ids_for_ia_role_category AS SELECT task_id FROM cft_task_db.tasks WHERE role_category is null and jurisdiction = 'IA';

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' group by task_name

--count|task_name                   |
-------+----------------------------+
--   43|Create Case Summary         |
--    4|Create Hearing Bundle       |
--34293|Follow-up extended direction|
-- 5506|Review Addendum Evidence    |

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_name in ('Follow-up extended direction', 'Create Case Summary','Create Hearing Bundle') group by task_name ;

--count|task_name                   |
-------+----------------------------+
--   43|Create Case Summary         |
--    4|Create Hearing Bundle       |
--34293|Follow-up extended direction|

select count(*), task_type  from cft_task_db.tasks where work_type is null and jurisdiction = 'IA' and task_type in ('createCaseSummary', 'createHearingBundle') group by task_type ;


--count|task_type          |
-------+-------------------+
--   43|createCaseSummary  |
--    4|createHearingBundle|

CREATE TABLE tmp_task_ids_for_ia_work_type AS SELECT task_id FROM cft_task_db.tasks WHERE work_type is null and jurisdiction = 'IA';



-- IMPLEMENTATION STEPS
-----------------------------

update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_name in ('Follow-up extended direction', 'Create Case Summary','Create Hearing Bundle');

update cft_task_db.tasks set role_category = 'JUDICIAL' where role_category is null and jurisdiction = 'IA'  and task_name in ('Review Addendum Evidence');
update cft_task_db.tasks set work_type = 'routine_work' where work_type is null and jurisdiction = 'IA' and task_type in ('createCaseSummary');
update cft_task_db.tasks set work_type = 'hearing_work' where work_type is null and jurisdiction = 'IA' and task_type in ('createHearingBundle');

– POST IMPLEMENTATION VERIFICATION STEPS
--------------------------------------------

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';
-- 0
select count(*) from cft_task_db.tasks where work_type is null and jurisdiction = 'IA';
-- 0
drop table tmp_task_ids_for_ia_role_category;
drop table tmp_task_ids_for_ia_work_type;

