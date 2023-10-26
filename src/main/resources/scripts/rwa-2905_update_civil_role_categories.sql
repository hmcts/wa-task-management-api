-- PRE DEPLOYMENT DATA PREPARATION STEPS
-----------------------------------------
select task_id from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL';
--  take a backup of all the task_id's

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL'; 
--187

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL' group by task_name ;
--  13	  Confirm Case Offline
--  3	  Directions after Judgment (Damages)
--  127	  Fast Track Directions
--  3	  Legal Advisor Small Claims Track Directions
--  26	  Small Claims Track Directions
--  15	  Transfer Case Offline

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL' and task_name in ('Confirm Case Offline', 'Transfer Case Offline') group by task_name ;
--  13	  Confirm Case Offline
--  15	  Transfer Case Offline

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL' and task_name in ('Directions after Judgment (Damages)', 'Fast Track Directions', 'Small Claims Track Directions') group by task_name ;
--  3	  Directions after Judgment (Damages)
--  127	  Fast Track Directions
--  26	  Small Claims Track Directions

select count(*), task_name  from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL' and task_name in ('Legal Advisor Small Claims Track Directions') group by task_name ;
--  3	  Legal Advisor Small Claims Track Directions


-- IMPLEMENTATION STEPS
-----------------------------

update cft_task_db.tasks set role_category = 'ADMIN' where role_category is null and jurisdiction = 'CIVIL' and task_name in ('Confirm Case Offline', 'Transfer Case Offline');
update cft_task_db.tasks set role_category = 'JUDICIAL' where role_category is null and jurisdiction = 'CIVIL'  and task_name in ('Directions after Judgment (Damages)', 'Fast Track Directions', 'Small Claims Track Directions');
update cft_task_db.tasks set role_category = 'LEGAL_OPERATIONS' where role_category is null and jurisdiction = 'CIVIL' and task_name in ('Legal Advisor Small Claims Track Directions');


-- POST IMPLEMENTATION VERIFICATION STEPS
--------------------------------------------

select count(*) from cft_task_db.tasks where role_category is null and jurisdiction = 'CIVIL'; 
--  0
