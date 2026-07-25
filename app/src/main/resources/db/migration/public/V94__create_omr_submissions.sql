-- =====================================================================
-- V94: additive OMR submission history and official-result pointer
--
-- Historical exam_results are deliberately not backfilled. New batch OMR
-- sheets are persisted independently so an unregistered sheet never needs a
-- placeholder exam_result row.
-- =====================================================================

create table if not exists omr_submissions (
    id                    bigserial not null,
    tenant_id             bigint not null,
    version               bigint not null default 0,
    exam_id               bigint not null,
    job_id                bigint not null,
    image_index           integer not null,
    batch_accepted_at     timestamptz not null,
    source_image_key      varchar(500) not null,
    omr_image_key         varchar(500),
    ocr_phone_number      varchar(20),
    matched_student_id    bigint,
    total_score           integer,
    grade                 integer,
    grading_status        varchar(20) not null,
    reconciliation_status varchar(20),
    reconciliation_reason varchar(100),
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    constraint omr_submissions_pkey primary key (id),
    constraint uq_omr_submissions_job_image unique (job_id, image_index),
    constraint fk_omr_submissions_tenant
        foreign key (tenant_id) references tenants(id) on delete cascade,
    constraint fk_omr_submissions_exam
        foreign key (exam_id, tenant_id) references exams(id, tenant_id) on delete cascade,
    constraint fk_omr_submissions_job
        foreign key (job_id, tenant_id) references omr_grading_jobs(id, tenant_id) on delete cascade
);

create unique index if not exists idx_omr_submissions_id_tenant_id
    on omr_submissions (id, tenant_id);
create index if not exists idx_omr_submissions_tenant_exam_reconciliation_created
    on omr_submissions (tenant_id, exam_id, reconciliation_status, created_at desc);
create index if not exists idx_omr_submissions_tenant_phone_reconciliation
    on omr_submissions (tenant_id, ocr_phone_number, reconciliation_status)
    where ocr_phone_number is not null;
create index if not exists idx_omr_submissions_tenant_job_image
    on omr_submissions (tenant_id, job_id, image_index);
create index if not exists idx_omr_submissions_tenant_exam_source_order
    on omr_submissions (tenant_id, exam_id, batch_accepted_at, image_index);

alter table omr_submissions enable row level security;
alter table omr_submissions force row level security;
drop policy if exists tenant_isolation on omr_submissions;
create policy tenant_isolation on omr_submissions
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);
grant select, insert, update, delete on omr_submissions to lumie_app;
grant usage, select on sequence omr_submissions_id_seq to lumie_app;

create table if not exists omr_submission_questions (
    id                bigserial not null,
    tenant_id         bigint not null,
    omr_submission_id bigint not null,
    question_number   integer not null,
    student_answer    varchar(20),
    correct_answer    varchar(20),
    max_score         integer not null,
    earned_score      integer not null,
    question_type     varchar(30),
    constraint omr_submission_questions_pkey primary key (id),
    constraint uq_omr_submission_questions_submission_question
        unique (omr_submission_id, question_number),
    constraint fk_omr_submission_questions_tenant
        foreign key (tenant_id) references tenants(id) on delete cascade,
    constraint fk_omr_submission_questions_submission
        foreign key (omr_submission_id, tenant_id)
        references omr_submissions(id, tenant_id) on delete cascade
);

create index if not exists idx_omr_submission_questions_tenant_submission
    on omr_submission_questions (tenant_id, omr_submission_id);

alter table omr_submission_questions enable row level security;
alter table omr_submission_questions force row level security;
drop policy if exists tenant_isolation on omr_submission_questions;
create policy tenant_isolation on omr_submission_questions
    using (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);
grant select, insert, update, delete on omr_submission_questions to lumie_app;
grant usage, select on sequence omr_submission_questions_id_seq to lumie_app;

set local lock_timeout = '5s';
set local statement_timeout = '60s';

alter table if exists exam_results
    add column if not exists current_submission_id bigint;
alter table if exists exam_results
    add column if not exists manual_override_at timestamptz;
do $$
begin
    if not exists (
        select 1 from pg_constraint
        where conname = 'exam_results_current_submission_fkey'
          and conrelid = 'exam_results'::regclass
    ) then
        alter table exam_results
            add constraint exam_results_current_submission_fkey
            foreign key (current_submission_id, tenant_id)
            references omr_submissions(id, tenant_id)
            on delete set null (current_submission_id)
            not valid;
    end if;
end $$;
