-- Rollback Steps
 -----------------------------------------
 update cft_task_db.tasks set role_category = null where jurisdiction = 'IA' and task_id in (select * from tmp_task_ids_for_ia)
