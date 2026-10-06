-- ============================================================================
-- V124: normalize historical PASS_FAIL exam configuration
--
-- Deploy order:
--   1. Deploy the compatible backend that writes NULL grading_type and
--      grade_scale for PASS_FAIL exams, and applies GRADED defaults itself.
--   2. Confirm no old backend pod can receive traffic.
--   3. Apply this migration.
--
-- Forward-only:
--   This data migration changes only the two obsolete grading-configuration
--   values on the audited historical PASS_FAIL exam rows. It never changes an
--   exam category, pass score, answer, question score, total score, or result.
--   It accepts a fresh/empty environment, the four audited development rows,
--   or the nineteen audited production rows. Any new or unexpected PASS_FAIL
--   row fails closed for review instead of being silently rewritten.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    pass_fail_count bigint;
begin
    if to_regclass('public.exams') is null then
        raise exception 'cannot normalize PASS_FAIL exams: public.exams does not exist';
    end if;

    select count(*)
      into pass_fail_count
      from exams
     where category = 'PASS_FAIL';

    if pass_fail_count = 0 then
        return;
    end if;

    if pass_fail_count = 19 then
        if exists (
            (select id, tenant_id from exams where category = 'PASS_FAIL')
            except
            (values
                (29::bigint, 1::bigint), (30::bigint, 1::bigint),
                (40::bigint, 1::bigint), (48::bigint, 1::bigint),
                (52::bigint, 1::bigint), (55::bigint, 1::bigint),
                (56::bigint, 1::bigint), (57::bigint, 1::bigint),
                (60::bigint, 1::bigint), (61::bigint, 1::bigint),
                (63::bigint, 1::bigint), (66::bigint, 1::bigint),
                (68::bigint, 1::bigint), (71::bigint, 1::bigint),
                (142::bigint, 1::bigint), (143::bigint, 1::bigint),
                (148::bigint, 1::bigint), (155::bigint, 1::bigint),
                (162::bigint, 1::bigint)
            )
        ) or exists (
            (values
                (29::bigint, 1::bigint), (30::bigint, 1::bigint),
                (40::bigint, 1::bigint), (48::bigint, 1::bigint),
                (52::bigint, 1::bigint), (55::bigint, 1::bigint),
                (56::bigint, 1::bigint), (57::bigint, 1::bigint),
                (60::bigint, 1::bigint), (61::bigint, 1::bigint),
                (63::bigint, 1::bigint), (66::bigint, 1::bigint),
                (68::bigint, 1::bigint), (71::bigint, 1::bigint),
                (142::bigint, 1::bigint), (143::bigint, 1::bigint),
                (148::bigint, 1::bigint), (155::bigint, 1::bigint),
                (162::bigint, 1::bigint)
            )
            except
            (select id, tenant_id from exams where category = 'PASS_FAIL')
        ) then
            raise exception 'cannot normalize PASS_FAIL exams: production row set differs from the audited 19 rows';
        end if;
    elsif pass_fail_count = 4 then
        if exists (
            (select id, tenant_id from exams where category = 'PASS_FAIL')
            except
            (values
                (80::bigint, 97::bigint), (81::bigint, 97::bigint),
                (85::bigint, 97::bigint), (133::bigint, 97::bigint)
            )
        ) or exists (
            (values
                (80::bigint, 97::bigint), (81::bigint, 97::bigint),
                (85::bigint, 97::bigint), (133::bigint, 97::bigint)
            )
            except
            (select id, tenant_id from exams where category = 'PASS_FAIL')
        ) then
            raise exception 'cannot normalize PASS_FAIL exams: development row set differs from the audited 4 rows';
        end if;
    else
        raise exception
            'cannot normalize PASS_FAIL exams: expected zero, 4 audited development rows, or 19 audited production rows; found %',
            pass_fail_count;
    end if;

    if exists (
        select 1
          from exams
         where category = 'PASS_FAIL'
           and (
               pass_score is null
               or not (
                   (grading_type is null and grade_scale is null)
                   or (grading_type = 'ABSOLUTE' and grade_scale = 'NINE_GRADE')
               )
           )
    ) then
        raise exception
            'cannot normalize PASS_FAIL exams: each audited row must have a pass score and either legacy ABSOLUTE/NINE_GRADE or already-null configuration';
    end if;

    update exams
       set grading_type = null,
           grade_scale = null
     where category = 'PASS_FAIL'
       and (grading_type is not null or grade_scale is not null);

    if exists (
        select 1
          from exams
         where category = 'PASS_FAIL'
           and (grading_type is not null or grade_scale is not null)
    ) then
        raise exception 'cannot normalize PASS_FAIL exams: grading configuration remains after update';
    end if;
end $$;
