/*
 * Returns the abbreviation for the given role category.
 */
create or replace function cft_task_db.abbreviate_role_category(l_role_category text)
	returns text language plpgsql immutable
as $$
begin
return
    case
        when l_role_category = 'JUDICIAL' then 'J'
        when l_role_category = 'LEGAL_OPERATIONS' then 'L'
        when l_role_category = 'ADMIN' then 'A'
        when l_role_category = 'CTSC' then 'C'
        when l_role_category = 'ENFORCEMENT' then 'E'
        else null
        end;
end;
$$;
