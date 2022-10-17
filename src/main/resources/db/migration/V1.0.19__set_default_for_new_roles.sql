ALTER TABLE task_roles ALTER COLUMN complete SET default false;
ALTER TABLE task_roles ALTER COLUMN complete_own SET default false;
ALTER TABLE task_roles ALTER COLUMN cancel_own SET default false;
ALTER TABLE task_roles ALTER COLUMN claim SET default false;
ALTER TABLE task_roles ALTER COLUMN unclaim SET default false;
ALTER TABLE task_roles ALTER COLUMN assign SET default false;
ALTER TABLE task_roles ALTER COLUMN unassign SET default false;
ALTER TABLE task_roles ALTER COLUMN unclaim_assign SET default false;
ALTER TABLE task_roles ALTER COLUMN unassign_claim SET default false;
ALTER TABLE task_roles ALTER COLUMN unassign_assign SET default false;
UPDATE task_roles SET
    complete = false,
    complete_own = false,
    cancel_own = false,
    claim = false,
    unclaim = false,
    assign = false,
    unassign = false,
    unclaim_assign = false,
    unassign_claim = false,
    unassign_assign = false;



