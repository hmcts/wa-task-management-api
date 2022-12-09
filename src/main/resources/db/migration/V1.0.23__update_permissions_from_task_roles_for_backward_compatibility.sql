UPDATE task_roles SET unassign=true, assign=true, complete=true WHERE manage=true;

UPDATE task_roles SET claim=true WHERE own=true;

UPDATE task_roles SET claim=true WHERE execute=true;
