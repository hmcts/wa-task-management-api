alter table tasks add column priority_date timestamp without time zone;
update tasks set priority_date = due_date_time, major_priority = 5000, minor_priority = 500;
alter table tasks alter column priority_date set not null;
alter table tasks alter column major_priority set default 5000;
alter table tasks alter column minor_priority set default 500;
