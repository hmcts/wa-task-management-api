ALTER TABLE task_roles
    ADD COLUMN complete BOOLEAN   default false,
    ADD COLUMN complete_own BOOLEAN   default false,
    ADD COLUMN cancel_own BOOLEAN   default false,
    ADD COLUMN claim BOOLEAN   default false,
    ADD COLUMN unclaim BOOLEAN   default false,
    ADD COLUMN assign BOOLEAN   default false,
    ADD COLUMN unassign BOOLEAN   default false,
    ADD COLUMN unclaim_assign BOOLEAN   default false,
    ADD COLUMN unassign_claim BOOLEAN   default false,
    ADD COLUMN unassign_assign BOOLEAN   default false;
