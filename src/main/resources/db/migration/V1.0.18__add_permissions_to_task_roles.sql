ALTER TABLE task_roles
    ADD COLUMN complete BOOLEAN,
    ADD COLUMN complete_own BOOLEAN,
    ADD COLUMN cancel_own BOOLEAN,
    ADD COLUMN claim BOOLEAN,
    ADD COLUMN unclaim BOOLEAN,
    ADD COLUMN assign BOOLEAN,
    ADD COLUMN unassign BOOLEAN,
    ADD COLUMN unclaim_assign BOOLEAN,
    ADD COLUMN unassign_claim BOOLEAN,
    ADD COLUMN unassign_assign BOOLEAN;
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


