DROP TYPE IF EXISTS task_state_enum;
CREATE TYPE task_state_enum as ENUM (
    'UNCONFIGURED',
    'PENDING_AUTO_ASSIGN',
    'ASSIGNED',
    'UNASSIGNED',
    'COMPLETED',
    'CANCELLED',
    'TERMINATED',
    'PENDING_RECOMMENDATION');

DROP TYPE IF EXISTS execution_type_enum;
CREATE TYPE execution_type_enum as ENUM (
    'MANUAL',
    'BUILT_IN',
    'CASE_EVENT');

DROP TYPE IF EXISTS security_classification_enum;
CREATE TYPE security_classification_enum as ENUM (
    'PUBLIC',
    'PRIVATE',
    'RESTRICTED');

DROP TYPE IF EXISTS task_system_enum;
CREATE TYPE task_system_enum as ENUM (
    'SELF',
    'CTSC');


DROP TYPE IF EXISTS business_context_enum;
CREATE TYPE business_context_enum as ENUM (
    'CFT_TASK');

COMMIT;
