-- Add tenant-scoped configurable exam question types.
-- Existing exams and templates keep their question_types JSONB snapshots; this
-- table is only the source list for future form selections.
set local lock_timeout = '5s';
set local statement_timeout = '30s';

create table if not exists exam_question_types (
    id            bigserial primary key,
    tenant_id     bigint       not null,
    version       bigint       not null default 0,
    name          varchar(50)  not null,
    display_order integer      not null,
    created_at    timestamptz  not null default now(),
    updated_at    timestamptz  not null default now(),
    constraint fk_exam_question_types_tenant
        foreign key (tenant_id) references tenants(id) on delete cascade,
    constraint uq_exam_question_types_tenant_name unique (tenant_id, name)
);

create index if not exists idx_exam_question_types_tenant_id
    on exam_question_types (tenant_id);

insert into exam_question_types (tenant_id, name, display_order)
select t.id, defaults.name, defaults.display_order
from tenants t
cross join (values
    ('듣기', 1),
    ('기초유형', 2),
    ('내용파악', 3),
    ('빈칸', 4),
    ('순서', 5),
    ('삽입', 6),
    ('어법', 7),
    ('어휘', 8),
    ('단어', 9)
) as defaults(name, display_order)
on conflict (tenant_id, name) do nothing;

alter table exam_question_types enable row level security;
alter table exam_question_types force row level security;
drop policy if exists tenant_isolation on exam_question_types;
create policy tenant_isolation on exam_question_types
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

grant select, insert, update, delete on exam_question_types to lumie_app;
grant usage, select on sequence exam_question_types_id_seq to lumie_app;
