-- Update task_permissions view

drop view if exists cft_task_db.task_permissions;

create view cft_task_db.task_permissions as
-- Read permission ignores authorisations
select task_id as task_id,
       role_name as role_name,
       '*' as authorization,
       'r' as permission
from cft_task_db.task_roles
where read
union
-- Manage permission ignores authorisations
select task_id as task_id,
       role_name as role_name,
       '*' as authorization,
       'm' as permission
from cft_task_db.task_roles
where manage
union
-- Available permission handles authorisations
select task_id as task_id,
       role_name as role_name,
       unnest(case when cardinality(authorizations) = 0 then array['*'] else authorizations end) as authorization,
       'a' as permission
from cft_task_db.task_roles
where own and claim;

-- Update uniform_role_signatures function

create or replace function cft_task_db.uniform_role_signatures(l_task_id text)
  returns text[] language plpgsql immutable
as $$
declare
  l_signatures text[];
  l_task_data record;
begin
	-- Get the task data needed to generate signatures
	select jurisdiction, region, location, case_id, security_classification::text
	into l_task_data
	from cft_task_db.tasks
	where task_id = l_task_id;
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
				select unnest(cft_task_db.add_wildcard(l_task_data.jurisdiction))),
			regions (region) as (
				select unnest(cft_task_db.add_wildcard(l_task_data.region))),
			locations (location) as (
				select unnest(cft_task_db.add_wildcard(l_task_data.location))),
			case_ids (case_id) as (
				select unnest(cft_task_db.add_wildcard(l_task_data.case_id))),
			classifications (classification) as (
				select cft_task_db.abbreviate_classification(higher)
				from cft_task_db.classifications
				where lower = l_task_data.security_classification),
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
      l_task_data.case_id as case_id,
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
