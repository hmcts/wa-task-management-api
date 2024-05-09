--Pre Deployment Data preparations - volumes obtained 07/05/24
select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL';

--count|
-------+
--    2

--  take a backup of all the task_id's into a CSV file OR create a temporary table as below
CREATE TABLE tmp_task_ids_for_civil_role_category AS SELECT task_id FROM cft_task_db.tasks WHERE role_category is null and jurisdiction = 'CIVIL';

select count(*), task_type  from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL' group by task_type;

--count|task_type                          |
-------+-----------------------------------+
--    1|FastTrackDirections                |
--    1|reviewSpecificAccessRequestLegalOps|--note need to be amended below ***

select count(*) from cft_task_db.tasks where work_type is null and jurisdiction = 'CIVIL';

--count|
-------+
--   24
CREATE TABLE tmp_task_ids_for_civil_work_type AS SELECT task_id FROM cft_task_db.tasks WHERE work_type is null and jurisdiction = 'CIVIL';

select count(*), task_type  from cft_task_db.tasks where work_type is null and jurisdiction = 'CIVIL'  group by task_type ;

--count|task_type                        |
-------+---------------------------------+
--    3|FastTrackDirections              |
--    1|LegalAdvisorRevisitApplication   |--note need to add this below
--    7|transferCaseOffline              |
--   13|transferCaseOfflineNotSuitableSDO|

-- IMPLEMENTATION STEPS
-----------------------------
update cft_task_db.tasks set role_category = 'JUDICIAL' where role_category is null and jurisdiction = 'CIVIL'  and task_type in ('FastTrackDirections');
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'CIVIL' and task_type in ('reviewSpecificAccessRequestLegalOps');

update cft_task_db.tasks set work_type = 'decision_making_work' where work_type is null and jurisdiction = 'CIVIL' and task_type in ('FastTrackDirections');
update cft_task_db.tasks set work_type = 'hearing_work' where work_type is null and jurisdiction = 'CIVIL' and task_type in ('transferCaseOfflineNotSuitableSDO','transferCaseOffline');

-- POST IMPLEMENTATION VERIFICATION STEPS
--------------------------------------------

select count from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL';
--  0
select count from cft_task_db.tasks where work_type is null and jurisdiction = 'CIVIL';
--  0

drop table tmp_task_ids_for_civil_role_category;
drop table tmp_task_ids_for_civil_work_type;
