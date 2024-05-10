-- Rollback Steps
 -----------------------------------------
 update cft_task_db.tasks set role_category = null where jurisdiction = 'CIVIL' and task_id in (select task_id from tmp_task_ids_for_civil_role_category);

 update cft_task_db.tasks set work_type = null where jurisdiction = 'CIVIL' and task_id in (select task_id from tmp_task_ids_for_civil_work_type);

