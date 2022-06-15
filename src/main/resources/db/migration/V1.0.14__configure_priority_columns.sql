alter table cft_task_db.tasks add column priority_date_time timestamp without time zone;
update cft_task_db.tasks set priority_date_time = due_date_time, major_priority = 5000, minor_priority = 5000;
alter table cft_task_db.tasks alter column priority_date_time set not null;
alter table cft_task_db.tasks alter column major_priority set not null;
alter table cft_task_db.tasks alter column minor_priority set not null;
