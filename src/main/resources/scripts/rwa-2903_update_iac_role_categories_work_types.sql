-- PRE DEPLOYMENT DATA PREPARATION STEPS
 -----------------------------------------
 -- PRE DEPLOYMENT DATA PREPARATION STEPS volumes calculated 07/05/24

 select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';

--count|
-------+
--40032

--  take a backup of all the task_id's into a CSV file OR create a temporary table as below
CREATE TABLE tmp_task_ids_for_ia_role_category AS SELECT task_id FROM cft_task_db.tasks WHERE role_category is null and jurisdiction = 'IA';

select count(*), task_type  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' group by task_type;

--count|task_type                |
-------+-------------------------+
--   43|createCaseSummary        |
--    4|createHearingBundle      |
--34293|followUpExtendedDirection|
-- 5692|reviewAddendumEvidence   |

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created < '2022-12-01';
-- 5119

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created < '2023-03-01' and created >= '2022-12-01';
-- 4184

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created < '2023-06-01' and created >= '2023-03-01';
--4563

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created < '2023-09-01' and created >= '2023-06-01';
--4232

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created < '2023-11-01' and created >= '2023-09-01';
--3865

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created < '2024-01-01' and created >= '2023-11-01';
--5526

select count(*)  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type  = 'followUpExtendedDirection' and created >= '2024-01-01';
--6804

select count(*), task_type  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' and task_type in ('followUpExtendedDirection', 'createCaseSummary','createHearingBundle') group by task_type;

--count|task_type                |
-------+-------------------------+
--   43|createCaseSummary        |
--    4|createHearingBundle      |
--34293|followUpExtendedDirection|

select count(*), task_type  from cft_task_db.tasks where work_type is null and jurisdiction = 'IA' and task_type in ('createCaseSummary', 'createHearingBundle') group by task_type ;


--count|task_type          |
-------+-------------------+
--   43|createCaseSummary  |
--    4|createHearingBundle|

CREATE TABLE tmp_task_ids_for_ia_work_type AS SELECT task_id FROM cft_task_db.tasks WHERE work_type is null and jurisdiction = 'IA';



-- IMPLEMENTATION STEPS
-----------------------------

update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type in ('createCaseSummary','createHearingBundle');

update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created < '2022-12-01';
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created < '2023-03-01' and created >= '2022-12-01';
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created < '2023-06-01' and created >= '2023-03-01';
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created < '2023-09-01' and created >= '2023-06-01';
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created < '2023-11-01' and created >= '2023-09-01';
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created < '2024-01-01' and created >= '2023-11-01';
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_type ='followUpExtendedDirection' and created >= '2024-01-01';



update cft_task_db.tasks set role_category = 'JUDICIAL' where role_category is null and jurisdiction = 'IA'  and task_type in ('reviewAddendumEvidence');
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

