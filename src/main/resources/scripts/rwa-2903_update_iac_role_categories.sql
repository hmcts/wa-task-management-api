-- PRE DEPLOYMENT DATA PREPARATION STEPS
 -----------------------------------------
 --  select task_id from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';
 --  take a backup of all the task_id's
 CREATE TABLE tmp_task_ids_for_ia AS SELECT task_id FROM tasks WHERE role_category is null;

 select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';
 --23,120 or even more by then

 select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'IA' group by task_name ;
 --  43	      Create Case Summary
 --  4	      Create Hearing Bundle
 --  23073	  Follow-up extended direction

 -- IMPLEMENTATION STEPS
 -----------------------------

 update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'IA' and task_name in ('Follow-up extended direction', 'Create Hearing Bundle', 'Create Case Summary');


 -- POST IMPLEMENTATION VERIFICATION STEPS
 --------------------------------------------

 select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'IA';
 --  0
