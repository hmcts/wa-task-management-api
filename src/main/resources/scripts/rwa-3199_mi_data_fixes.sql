-- PRE - CHECKS
----------------------
select  a.service , count(distinct a.task_id) from
cft_task_db.cft_task_db.task_assignments a
where not exists (select task_id  from cft_task_db.cft_task_db.reportable_task  b
where a.task_id  = b.task_id) group by a.service;

-- service   |count|
-- ----------+-----+
-- CIVIL     |  287|
-- IA        | 8694|
-- PRIVATELAW|   16|
-- Total 8997

-- Get list of task id's
select  distinct (a.task_id) from
cft_task_db.cft_task_db.task_assignments a
where not exists (select task_id  from cft_task_db.cft_task_db.reportable_task  b
where a.task_id  = b.task_id);

-- delete data from task_assignments table when data is not present in reportable_task table
delete from cft_task_db.cft_task_db.task_assignments where task_id in (select  distinct (a.task_id) from
cft_task_db.cft_task_db.task_assignments a
where not exists (select task_id  from cft_task_db.cft_task_db.reportable_task  b
where a.task_id  = b.task_id));

--POST-CHECKS

select  a.service , count(distinct a.task_id) from
cft_task_db.cft_task_db.task_assignments a
where not exists (select task_id  from cft_task_db.cft_task_db.reportable_task  b
where a.task_id  = b.task_id) group by a.service;

--should return zero rows





