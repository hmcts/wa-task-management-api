-- Rollback Steps
 -----------------------------------------
 update cft_task_db.tasks set role_category = null where jurisdiction = 'PRIVATELAW' and task_id in ('c3531487-5958-11ee-bb77-628514c82c3a', 'b149748c-6844-11ee-a0b2-b6312ed30a35', '98285dde-5d16-11ee-8178-32fa152835a3', '1a37373d-2ad9-11ee-87ca-22d3a1317fca');
