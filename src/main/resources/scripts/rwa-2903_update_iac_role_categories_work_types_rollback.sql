-- Rollback Steps
 -----------------------------------------
 update cft_task_db.tasks set role_category = null where jurisdiction = 'IA' and task_id in (select task_id from tmp_task_ids_for_ia_role_category);

 update cft_task_db.tasks set work_type = null where jurisdiction = 'IA' and task_id in (select task_id from tmp_task_ids_for_ia_work_type);

