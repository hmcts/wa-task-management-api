#DB Update

ALTER table cft_task_db.task_roles add column migrated boolean;

select count(*) from cft_task_db.task_roles WHERE complete=false and complete_own=false and cancel_own=false and claim=false and unclaim=false and unassign=false and assign=false and unassign=false and unclaim_assign=false and unassign_claim=false and unassign_assign=false;
--38555 on demo

select count(*) from cft_task_db.task_roles WHERE complete=false and complete_own=false and cancel_own=false and claim=false and unclaim=false and unassign=false and assign=false and unassign=false and unclaim_assign=false and unassign_claim=false and unassign_assign=false and manage=true;
--29635  on demo

select count(*) from cft_task_db.task_roles WHERE manage=true and unassign=false and complete=false and assign=false;
--29635  on demo

select count(*) from cft_task_db.task_roles WHERE complete=false and complete_own=false and cancel_own=false and claim=false and unclaim=false and unassign=false and assign=false and unassign=false and unclaim_assign=false and unassign_claim=false and unassign_assign=false and own=true;
--24798  on demo

select count(*) from cft_task_db.task_roles WHERE own=true and claim=false;
--24798  on demo

select count(*) from cft_task_db.task_roles WHERE complete=false and complete_own=false and cancel_own=false and claim=false and unclaim=false and unassign=false and assign=false and unassign=false and unclaim_assign=false and unassign_claim=false and unassign_assign=false and execute=true;
--8235  on demo
select count(*) from cft_task_db.task_roles WHERE execute=true and claim=false;
--8235  on demo


UPDATE cft_task_db.task_roles SET unassign=true, assign=true, complete=true, migrated=true WHERE manage=true and unassign=false and complete=false and assign=false;
---UPDATE 29635  on demo

UPDATE cft_task_db.task_roles SET claim=true, migrated=true WHERE own=true and claim=false;

---UPDATE 24798  on demo

UPDATE cft_task_db.task_roles SET claim=true, migrated=true WHERE execute=true and claim=false;

---UPDATE 8235  on demo


#Rollback


UPDATE cft_task_db.task_roles SET unassign=false, assign=false, complete=false WHERE manage=true and unassign=true and  assign=true and  complete=true and migrated=true;
---UPDATE 29635  on demo

UPDATE cft_task_db.task_roles SET claim=false WHERE own=true and claim=true and migrated=true;
---UPDATE 24798   on demo


UPDATE cft_task_db.task_roles SET claim=false WHERE execute=true and claim=true and migrated=true;
---UPDATE 8235  on demo

UPDATE cft_task_db.task_roles SET migrated=false;
