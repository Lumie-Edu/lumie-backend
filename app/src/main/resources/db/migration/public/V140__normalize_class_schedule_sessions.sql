set local lock_timeout = '5s';
set local statement_timeout = '60s';

-- Convert the retired shared-time schedule shape into the canonical per-day
-- sessions list before removing legacy fields from the HTTP and entity model.
-- Empty legacy values represent an unscheduled class and become an empty list.
do $$
declare
    invalid_rows bigint;
begin
    select count(*)
      into invalid_rows
      from classes c
     where c.schedule_pattern is not null
       and (case
                when jsonb_typeof(c.schedule_pattern->'sessions') = 'array'
                    then jsonb_array_length(c.schedule_pattern->'sessions')
                else 0
            end) = 0
       and c.schedule_pattern ?| array['days', 'startTime', 'endTime']
       and (
            (c.schedule_pattern ? 'sessions'
                and jsonb_typeof(c.schedule_pattern->'sessions') not in ('array', 'null'))
            or (c.schedule_pattern ? 'days'
                and jsonb_typeof(c.schedule_pattern->'days') not in ('array', 'null'))
            or case
                when jsonb_typeof(c.schedule_pattern->'days') is distinct from 'array' then false
                when jsonb_array_length(c.schedule_pattern->'days') = 0 then false
                else (
                    coalesce(c.schedule_pattern->>'startTime', '') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'
                    or coalesce(c.schedule_pattern->>'endTime', '') !~ '^([01][0-9]|2[0-3]):[0-5][0-9]$'
                    or c.schedule_pattern->>'endTime' <= c.schedule_pattern->>'startTime'
                    or exists (
                        select 1
                          from jsonb_array_elements_text(c.schedule_pattern->'days') as day(value)
                         where day.value not in ('SUN', 'MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT')
                    )
                    or jsonb_array_length(c.schedule_pattern->'days') <>
                       (select count(distinct day.value)
                          from jsonb_array_elements_text(c.schedule_pattern->'days') as day(value))
                )
            end
       );

    if invalid_rows > 0 then
        raise exception
            'V140 abort: % legacy class schedule row(s) cannot be converted without manual correction',
            invalid_rows;
    end if;
end $$;

update classes c
   set schedule_pattern =
       (c.schedule_pattern - 'days' - 'startTime' - 'endTime')
       || jsonb_build_object(
            'sessions',
            case
                when (case
                          when jsonb_typeof(c.schedule_pattern->'sessions') = 'array'
                              then jsonb_array_length(c.schedule_pattern->'sessions')
                          else 0
                      end) > 0
                then c.schedule_pattern->'sessions'
                when jsonb_typeof(c.schedule_pattern->'days') = 'array' then
                    case
                        when jsonb_array_length(c.schedule_pattern->'days') > 0 then (
                            select jsonb_agg(
                                       jsonb_build_object(
                                           'day', day.value,
                                           'startTime', c.schedule_pattern->>'startTime',
                                           'endTime', c.schedule_pattern->>'endTime'
                                       )
                                       order by day.ordinality
                                   )
                            from jsonb_array_elements_text(c.schedule_pattern->'days')
                                 with ordinality as day(value, ordinality)
                        )
                        else '[]'::jsonb
                    end
                else '[]'::jsonb
            end
       )
 where c.schedule_pattern is not null
   and c.schedule_pattern ?| array['days', 'startTime', 'endTime'];
