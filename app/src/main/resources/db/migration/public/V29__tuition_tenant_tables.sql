-- =====================================================================
-- V29: Tuition tenant-scoped tables (Academy ↔ Guardian)
--
-- Purpose: introduce the academy-side payment domain — guardians,
-- student-guardian relationships, merchant profiles (Toss sub-merchant
-- KYC), tuition invoices, tuition payments, and cash receipts.
--
-- Scope / ownership:
--   ALL tables in this migration are TENANT-SCOPED.  Each row belongs to
--   exactly one tenant (academy).  Row-Level Security is applied to every
--   table using the canonical V18 pattern:
--     USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
--     WITH CHECK (...)
--   The migrator role (postgres, SUPERUSER) runs Flyway and sees all rows.
--   The runtime role (lumie_app, NOBYPASSRLS) is filtered by RLS.
--
-- Cross-module FK note:
--   tuition_invoices.student_id and tuition_payments.refunded_by_staff_id
--   reference users.id conceptually, but NO database-level FK is declared.
--   Cross-module referential integrity is maintained at the application
--   layer (BillingService validates via the internal-api StudentService /
--   UserService ports).  This avoids a hard coupling between the billing
--   and identity modules at the DB level.
--   student_guardians.student_id has the same pattern — no DB FK to users.
--
-- Rollback: NOT safely reversible after apply.
--   Drop order: cash_receipts → tuition_payments → tuition_invoices
--   → merchant_profiles → student_guardians → guardians.
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. guardians — student parents / legal guardians (tenant-scoped)
-- -----------------------------------------------------------------------
-- Enum candidates:
--   relationship: '부' | '모' | '조부' | '조모' | '법정대리인' | '기타'
create table if not exists guardians (
    id               bigserial          not null,
    tenant_id        bigint             not null,
    name             varchar(100)       not null,
    phone            varchar(30)        not null,
    email            varchar(255),
    relationship     varchar(30),
    version          bigint             not null default 0,
    created_at       timestamptz        not null default now(),
    updated_at       timestamptz        not null default now(),
    constraint guardians_pkey primary key (id)
);

comment on column guardians.relationship is '부 | 모 | 조부 | 조모 | 법정대리인 | 기타 — free-form string, not CHECK-constrained to allow future extension.';

create index if not exists idx_guardians_tenant_id
    on guardians (tenant_id);
create index if not exists idx_guardians_tenant_id_phone
    on guardians (tenant_id, phone);

alter table guardians enable row level security;
alter table guardians force  row level security;
create policy if not exists tenant_isolation on guardians
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 2. student_guardians — Student ↔ Guardian M:N join table
--
-- student_id references users.id semantically, but NO DB FK is declared
-- (cross-module boundary — see file header note).
-- guardian_id FK has ON DELETE CASCADE: removing a guardian removes all
-- their student associations.
-- -----------------------------------------------------------------------
create table if not exists student_guardians (
    student_id    bigint          not null,
    guardian_id   bigint          not null references guardians(id) on delete cascade,
    tenant_id     bigint          not null,
    is_primary    boolean         not null default false,
    created_at    timestamptz     not null default now(),
    constraint student_guardians_pkey primary key (student_id, guardian_id)
);

comment on column student_guardians.student_id is 'References users.id (STUDENT role). No DB FK — cross-module; integrity enforced at application layer.';
comment on column student_guardians.is_primary is 'true = primary contact for notifications. At most one primary per student (enforced at application layer).';

create index if not exists idx_student_guardians_guardian_id
    on student_guardians (guardian_id);
create index if not exists idx_student_guardians_tenant_id
    on student_guardians (tenant_id);

alter table student_guardians enable row level security;
alter table student_guardians force  row level security;
create policy if not exists tenant_isolation on student_guardians
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 3. merchant_profiles — Toss sub-merchant KYC record (one per tenant)
-- -----------------------------------------------------------------------
-- Enum candidates: kyc_status: 'PENDING' | 'APPROVED' | 'REJECTED'
create table if not exists merchant_profiles (
    id                          bigserial      not null,
    tenant_id                   bigint         not null,
    pg_seller_key               varchar(200),
    business_reg_number         varchar(20)    not null,
    representative_name         varchar(50)    not null,
    settlement_bank_code        varchar(10)    not null,
    settlement_account_number   varchar(30)    not null,
    settlement_account_holder   varchar(50)    not null,
    kyc_status                  varchar(20)    not null default 'PENDING',
    kyc_submitted_at            timestamptz,
    kyc_completed_at            timestamptz,
    rejection_reason            varchar(500),
    version                     bigint         not null default 0,
    created_at                  timestamptz    not null default now(),
    updated_at                  timestamptz    not null default now(),
    constraint merchant_profiles_pkey primary key (id),
    constraint merchant_profiles_tenant_id_key unique (tenant_id),
    constraint merchant_profiles_kyc_status_check check (
        kyc_status in ('PENDING', 'APPROVED', 'REJECTED')
    )
);

comment on column merchant_profiles.kyc_status      is 'PENDING | APPROVED | REJECTED';
comment on column merchant_profiles.pg_seller_key   is 'Toss-issued sellerKey — populated after KYC approval.';
comment on column merchant_profiles.business_reg_number is '사업자등록번호 (10 digits, dashes optional in storage)';

create index if not exists idx_merchant_profiles_kyc_status
    on merchant_profiles (kyc_status);

alter table merchant_profiles enable row level security;
alter table merchant_profiles force  row level security;
create policy if not exists tenant_isolation on merchant_profiles
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 4. tuition_invoices — Academy → Guardian billing invoice
--
-- guardian_id nullable: if NULL, the invoice is addressed to the student
-- directly (adult students without a registered guardian).
-- class_enrollment_id is a soft reference (no DB FK — cross-module).
-- -----------------------------------------------------------------------
-- Enum candidates: status: 'DRAFT' | 'ISSUED' | 'PAID' | 'OVERDUE' | 'CANCELLED'
create table if not exists tuition_invoices (
    id                    bigserial          not null,
    tenant_id             bigint             not null,
    student_id            bigint             not null,
    guardian_id           bigint             references guardians(id) on delete set null,
    class_enrollment_id   bigint,
    title                 varchar(200)       not null,
    items                 jsonb              not null,
    total_amount          decimal(15, 0)     not null,
    vat_amount            decimal(15, 0)     not null default 0,
    status                varchar(20)        not null default 'DRAFT',
    issued_at             timestamptz,
    due_date              timestamptz,
    paid_at               timestamptz,
    cancelled_at          timestamptz,
    cancellation_reason   varchar(500),
    idempotency_key       varchar(64),
    version               bigint             not null default 0,
    created_at            timestamptz        not null default now(),
    updated_at            timestamptz        not null default now(),
    constraint tuition_invoices_pkey primary key (id),
    constraint tuition_invoices_idempotency_key_key unique (idempotency_key),
    constraint tuition_invoices_status_check check (
        status in ('DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'CANCELLED')
    )
);

comment on column tuition_invoices.status             is 'DRAFT | ISSUED | PAID | OVERDUE | CANCELLED';
comment on column tuition_invoices.student_id         is 'References users.id (STUDENT role). No DB FK — cross-module boundary.';
comment on column tuition_invoices.class_enrollment_id is 'References class_enrollments.id (soft ref). No DB FK — cross-module boundary.';
comment on column tuition_invoices.items              is 'JSON array of LineItem {name, quantity, unit_price, amount}.';
comment on column tuition_invoices.idempotency_key    is 'Doubles as Toss orderId for payment initiation — unique across tenant.';
comment on column tuition_invoices.guardian_id        is 'NULL = invoice addressed to student directly (adult student without guardian).';

create index if not exists idx_tuition_invoices_tenant_status_due
    on tuition_invoices (tenant_id, status, due_date);
create index if not exists idx_tuition_invoices_tenant_student
    on tuition_invoices (tenant_id, student_id);
create index if not exists idx_tuition_invoices_tenant_created
    on tuition_invoices (tenant_id, created_at desc);
create index if not exists idx_tuition_invoices_guardian_id
    on tuition_invoices (guardian_id)
    where guardian_id is not null;

alter table tuition_invoices enable row level security;
alter table tuition_invoices force  row level security;
create policy if not exists tenant_isolation on tuition_invoices
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 5. tuition_payments — single payment event against a tuition_invoice
--
-- refunded_by_staff_id references users.id semantically (no DB FK —
-- cross-module boundary).
-- -----------------------------------------------------------------------
-- Enum candidates:
--   status: 'PENDING' | 'CAPTURED' | 'FAILED' | 'CANCELLED'
--   method: 'CARD' | 'KAKAOPAY' | 'EASY_PAY' | 'TRANSFER' | 'VIRTUAL_ACCOUNT'
create table if not exists tuition_payments (
    id                      bigserial          not null,
    tenant_id               bigint             not null,
    tuition_invoice_id      bigint             not null references tuition_invoices(id) on delete restrict,
    pg_transaction_id       varchar(200),
    order_id                varchar(100)       not null,
    amount                  decimal(15, 0)     not null,
    method                  varchar(30)        not null,
    status                  varchar(20)        not null default 'PENDING',
    captured_at             timestamptz,
    failed_at               timestamptz,
    failure_reason          varchar(500),
    refunded_amount         decimal(15, 0)     not null default 0,
    refund_reason           varchar(500),
    refunded_at             timestamptz,
    refunded_by_staff_id    bigint,
    version                 bigint             not null default 0,
    created_at              timestamptz        not null default now(),
    updated_at              timestamptz        not null default now(),
    constraint tuition_payments_pkey primary key (id),
    constraint tuition_payments_order_id_key unique (order_id),
    constraint tuition_payments_status_check check (
        status in ('PENDING', 'CAPTURED', 'FAILED', 'CANCELLED')
    ),
    constraint tuition_payments_refund_amount_check check (
        refunded_amount >= 0 and refunded_amount <= amount
    )
);

comment on column tuition_payments.status              is 'PENDING | CAPTURED | FAILED | CANCELLED';
comment on column tuition_payments.method              is 'CARD | KAKAOPAY | EASY_PAY | TRANSFER | VIRTUAL_ACCOUNT';
comment on column tuition_payments.pg_transaction_id   is 'Toss paymentKey — populated on CAPTURED.';
comment on column tuition_payments.order_id            is 'Toss orderId — idempotency key, unique across the system.';
comment on column tuition_payments.refunded_by_staff_id is 'References users.id (STAFF/ADMIN role). No DB FK — cross-module boundary.';

create index if not exists idx_tuition_payments_invoice_id
    on tuition_payments (tenant_id, tuition_invoice_id);
create index if not exists idx_tuition_payments_tenant_status
    on tuition_payments (tenant_id, status, created_at desc);

alter table tuition_payments enable row level security;
alter table tuition_payments force  row level security;
create policy if not exists tenant_isolation on tuition_payments
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 6. cash_receipts — 현금영수증 (issued against a tuition payment)
-- -----------------------------------------------------------------------
-- Enum candidates:
--   receipt_type: 'PERSONAL' | 'BUSINESS'  (소득공제 | 지출증빙)
--   issue_status: 'PENDING' | 'ISSUED' | 'FAILED'
create table if not exists cash_receipts (
    id                   bigserial          not null,
    tenant_id            bigint             not null,
    tuition_payment_id   bigint             references tuition_payments(id) on delete restrict,
    receipt_type         varchar(20)        not null,
    identifier           varchar(30)        not null,
    amount               decimal(15, 0)     not null,
    vat_amount           decimal(15, 0)     not null default 0,
    approval_number      varchar(50),
    issue_status         varchar(20)        not null default 'PENDING',
    issued_at            timestamptz,
    external_id          varchar(100),
    failure_reason       varchar(500),
    version              bigint             not null default 0,
    created_at           timestamptz        not null default now(),
    updated_at           timestamptz        not null default now(),
    constraint cash_receipts_pkey primary key (id),
    constraint cash_receipts_receipt_type_check check (
        receipt_type in ('PERSONAL', 'BUSINESS')
    ),
    constraint cash_receipts_issue_status_check check (
        issue_status in ('PENDING', 'ISSUED', 'FAILED')
    )
);

comment on column cash_receipts.receipt_type    is 'PERSONAL (소득공제) | BUSINESS (지출증빙)';
comment on column cash_receipts.issue_status    is 'PENDING | ISSUED | FAILED';
comment on column cash_receipts.identifier      is '휴대폰번호 (PERSONAL) or 사업자번호 (BUSINESS)';
comment on column cash_receipts.approval_number is '국세청 승인번호 — populated on ISSUED.';
comment on column cash_receipts.external_id     is 'PG or 팝빌 receipt ID for external reference.';

create index if not exists idx_cash_receipts_tenant_id
    on cash_receipts (tenant_id);
create index if not exists idx_cash_receipts_payment_id
    on cash_receipts (tuition_payment_id)
    where tuition_payment_id is not null;

alter table cash_receipts enable row level security;
alter table cash_receipts force  row level security;
create policy if not exists tenant_isolation on cash_receipts
    using      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint)
    with check (tenant_id = nullif(current_setting('app.tenant_id', true), '')::bigint);

-- -----------------------------------------------------------------------
-- 7. Grants — runtime role (lumie_app) full DML on all V29 tables
--    RLS policies above restrict rows; grants give column-level access.
-- -----------------------------------------------------------------------
grant select, insert, update, delete on guardians          to lumie_app;
grant select, insert, update, delete on student_guardians  to lumie_app;
grant select, insert, update, delete on merchant_profiles  to lumie_app;
grant select, insert, update, delete on tuition_invoices   to lumie_app;
grant select, insert, update, delete on tuition_payments   to lumie_app;
grant select, insert, update, delete on cash_receipts      to lumie_app;

grant usage, select on sequence guardians_id_seq         to lumie_app;
grant usage, select on sequence merchant_profiles_id_seq to lumie_app;
grant usage, select on sequence tuition_invoices_id_seq  to lumie_app;
grant usage, select on sequence tuition_payments_id_seq  to lumie_app;
grant usage, select on sequence cash_receipts_id_seq     to lumie_app;
