DROP PUBLICATION IF EXISTS task_publication;
CREATE PUBLICATION task_publication FOR TABLE tasks WITH (publish = 'insert,update,delete');
