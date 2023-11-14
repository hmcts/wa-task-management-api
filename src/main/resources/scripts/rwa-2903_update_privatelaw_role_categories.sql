-- PRE DEPLOYMENT DATA PREPARATION STEPS
 -----------------------------------------
 select * from cft_task_db.tasks where role_category is null and jurisdiction = 'PRIVATELAW';
 -- take a backup of all the task_id's
 -- c3531487-5958-11ee-bb77-628514c82c3a
 -- b149748c-6844-11ee-a0b2-b6312ed30a35
 -- 1a37373d-2ad9-11ee-87ca-22d3a1317fca
 -- 98285dde-5d16-11ee-8178-32fa152835a3

 select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'PRIVATELAW';
 -- 4

 -- IMPLEMENTATION STEPS
 -----------------------------

 update cft_task_db.tasks set role_category = 'ADMIN' where role_category is null and jurisdiction = 'PRIVATELAW' and task_id in ('c3531487-5958-11ee-bb77-628514c82c3a', 'b149748c-6844-11ee-a0b2-b6312ed30a35', '98285dde-5d16-11ee-8178-32fa152835a3');

 update cft_task_db.tasks set role_category = 'CTSC' where role_category is null and jurisdiction = 'PRIVATELAW' and task_id in ('1a37373d-2ad9-11ee-87ca-22d3a1317fca');


 -- POST IMPLEMENTATION VERIFICATION STEPS
 --------------------------------------------

 select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'PRIVATELAW';
 --  0

