/*
 * Calculates the signatures of all role assignments which would match
 * the permissions on this task.  Each signature is a unique text string
 * which captures the role assignment attributes, the permision configured
 * for the relevant role, the authorisations required for the permission.
 *
 * Since some role assignment attributes are optional, there are signatures
 * with all possible combinations of the attributes being present or not.
 * There are 5 optional attributes, making for 32 signature variants for
 * each task/permission.
 */

create or replace function cft_task_db.role_signatures(l_task_id text, l_jurisdiction text, l_region text,
l_location text, l_case_id text, l_security_classification cft_task_db.security_classification_enum)
  returns text[] language plpgsql immutable
as $$
declare
l_signatures text[];
begin

-- Generate all the role assignment signatures which will match this task
select array_agg(
                   sig.jurisdiction
                   || ':' || sig.region
                   || ':' || sig.location
                   || ':' || sig.role_name
                   || ':' || sig.case_id
                   || ':' || sig.permission
                   || ':' || sig.classification
                   || ':' || sig.authorization)
into l_signatures
from (
         with
             jurisdictions (jurisdiction) as (
                 select unnest(cft_task_db.add_wildcard(l_jurisdiction))),
             regions (region) as (
                 select unnest(cft_task_db.add_wildcard(l_region))),
             locations (location) as (
                 select unnest(cft_task_db.add_wildcard(l_location))),
             case_ids (case_id) as (
                 select unnest(cft_task_db.add_wildcard(l_case_id))),
             classifications (classification) as (
                 select cft_task_db.abbreviate_classification(higher)
                 from cft_task_db.classifications
                 where lower = l_security_classification::text),
             -- Org role permissions use authorizations.  Note that authorisations
             -- are only present in the view for the "a" (available) permission.
             org_role_permissions (role_name, "permission", "authorization") as (
                 select distinct role_name, "permission", "authorization"
                 from cft_task_db.task_permissions
                 where task_id = l_task_id),
             -- Case role permissions do not use authorisations (=> wildcard)
             case_role_permissions (role_name, "permission", "authorization") as (
                 select distinct role_name, "permission", '*' as authorization
         from cft_task_db.task_permissions
         where task_id = l_task_id)
-- Org role permissions
select
    j.jurisdiction as jurisdiction,
    r.region as region,
    l.location as location,
    -- Org role assignments do not have a case ID
    '*' as case_id,
    p.role_name as role_name,
    p.permission as "permission",
    p.authorization as "authorization",
    c.classification as classification
from
    jurisdictions j,
    regions r,
    locations l,
    case_ids i,
    -- Use the permissions data for org roles (has authorisations).
    org_role_permissions p,
    classifications c
union all
-- Case role permissions
select
    j.jurisdiction as jurisdiction,
    r.region as region,
    l.location as location,
    -- Case roles have the case ID specified (no wildcard)
    l_case_id as case_id,
    p.role_name as role_name,
    p.permission as "permission",
    p.authorization as "authorization",
    c.classification as classification
from
    jurisdictions j,
    regions r,
    locations l,
    case_ids i,
    -- Use the permissions data for case roles (no authorisations).
    case_role_permissions p,
    classifications c) sig;
return l_signatures;
end;
$$;


/*
 * Calculates the signatures of all combined filter conditions which
 * would match the task.  Each signature is a unique text string which
 * encodes the filter.  Wildcards are reprsented with '*'.
 */
create or replace function cft_task_db.filter_signatures(l_task_id text, l_state cft_task_db.task_state_enum,
l_jurisdiction text, l_role_category text, l_work_type text, l_region text, l_location text)
  returns text[] language plpgsql immutable
as $$
declare
l_signatures text[];
begin

-- The signatures of client filters which will match this task
select array_agg(
                   sig.state
                   || ':' || sig.jurisdiction
                   || ':' || sig.role_category
                   || ':' || sig.work_type
                   || ':' || sig.region
                   || ':' || sig.location)
into l_signatures
from (
         with
             states (state) as (select unnest(cft_task_db.add_wildcard(cft_task_db.abbreviate_state(l_state::text)))),
             jurisdictions (jurisdiction) as (select unnest(cft_task_db.add_wildcard(l_jurisdiction))),
             role_categories (role_category) as(select unnest(cft_task_db.add_wildcard(cft_task_db.abbreviate_role_category(l_role_category)))),
             work_types (work_type) as (select unnest(cft_task_db.add_wildcard(l_work_type))),
             regions (region) as (select unnest(cft_task_db.add_wildcard(l_region))),
             locations (location) as (select unnest(cft_task_db.add_wildcard(l_location)))
         select
             s.state as state,
             j.jurisdiction as jurisdiction,
             c.role_category as role_category,
             w.work_type as work_type,
             r.region as region,
             l.location as location
         from
             states s,
             jurisdictions j,
             role_categories c,
             work_types w,
             regions r,
             locations l) sig;
return l_signatures;
end;
$$;

/*
 * GIN index to look up tasks based on role signatures and filter signatures.
 * Only covers assigned and unassigned tasks which have been flagged as
 * indexed.  There is no value in indexing old tasks, since we never search
 * them.
 */
drop index if exists cft_task_db.search_index;
create index search_index on cft_task_db.tasks using gin (
    cft_task_db.filter_signatures(task_id, state, jurisdiction, role_category, work_type, region, location),
    cft_task_db.role_signatures(task_id, jurisdiction, region, location, case_id, security_classification),
    case_id,
    assignee)
    where state in ('ASSIGNED','UNASSIGNED') and indexed;
