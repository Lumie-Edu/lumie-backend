-- irreversible data normalization / constraint relaxation: blank parent_phone values
-- are normalized to null and the legacy uniqueness constraint is removed.

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'students'
          and column_name = 'parent_phone'
    ) then
        update public.students
        set parent_phone = null
        where parent_phone = '';
    end if;
end
$$;

drop index if exists public.idx_students_parent_phone;
