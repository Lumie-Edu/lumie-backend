-- =====================================================================
-- V28: Billing platform tables (Lumie ↔ Tenant)
--
-- Purpose: introduce the core Lumie-side billing domain — plan catalogue,
-- subscriptions, invoices, billing keys (Toss auto-debit tokens),
-- alimtalk credits, PG audit log, and tax invoices.
--
-- Scope / ownership:
--   ALL tables in this migration are PLATFORM-SCOPED.  Lumie is the
--   owner/operator of this data across all tenants.  RLS is deliberately
--   NOT applied (same rationale as tenants, plans, owner_directory).
--   The lumie_app runtime role (NOSUPERUSER NOBYPASSRLS) can read any
--   row; access control is enforced at the application layer by the
--   billing module's security guards, not at the DB row level.
--
-- Circular FK note (billing_keys ↔ subscriptions):
--   subscriptions.billing_key_id → billing_keys.id is nullable, which
--   means billing_keys can be created first (independent entity), and
--   subscriptions can exist without a billing key (e.g., during trial or
--   manual invoicing).  This is a deliberate one-directional nullable
--   reference — NOT a circular dependency.  No DEFERRABLE constraint is
--   needed.  See flag §4 in the migration summary for details.
--
-- Rollback: this migration is NOT safely reversible once applied.
--   Tables may be referenced by application code and foreign keys.
--   To reverse in a dev environment, drop tables in dependency order:
--   tax_invoices → payment_transactions → invoices → subscriptions
--   → billing_keys → alimtalk_credits → plans.
--   Do NOT attempt to reverse on a live database that has received data.
-- =====================================================================

-- -----------------------------------------------------------------------
-- 1. plans — plan catalogue (platform-global, no tenant_id)
-- -----------------------------------------------------------------------
-- Enum candidates for reference:
--   id: 'basic' | 'starter' | 'pro' | 'enterprise' (application-defined)
create table if not exists plans (
    id                   varchar(20)        not null,
    name                 varchar(100)       not null,
    description          varchar(500),
    monthly_price        decimal(15, 0)     not null,
    yearly_price         decimal(15, 0)     not null,
    max_students         integer,
    max_academies        integer,
    max_admins           integer,
    omr_monthly_quota    integer,
    custom_domains       boolean            not null default false,
    advanced_analytics   boolean            not null default false,
    priority_support     boolean            not null default false,
    api_access           boolean            not null default false,
    white_labeling       boolean            not null default false,
    display_order        integer            not null default 0,
    active               boolean            not null default true,
    created_at           timestamptz        not null default now(),
    updated_at           timestamptz        not null default now(),
    constraint plans_pkey primary key (id)
);

comment on column plans.id                is 'Human-readable plan key: basic | starter | pro | enterprise';
comment on column plans.omr_monthly_quota is 'Max OMR grading jobs allowed per billing month. NULL = unlimited.';

-- -----------------------------------------------------------------------
-- 2. billing_keys — Toss auto-debit tokens (one active key per tenant)
--
-- Created BEFORE subscriptions so subscriptions.billing_key_id can FK here.
-- -----------------------------------------------------------------------
-- Enum candidates: status: 'ACTIVE' | 'REVOKED'
create table if not exists billing_keys (
    id                   bigserial          not null,
    tenant_id            bigint             not null references tenants(id) on delete restrict,
    pg_billing_key       varchar(200)       not null,
    masked_card_number   varchar(20)        not null,
    card_company         varchar(50),
    status               varchar(20)        not null default 'ACTIVE',
    revoked_at           timestamptz,
    version              bigint             not null default 0,
    created_at           timestamptz        not null default now(),
    updated_at           timestamptz        not null default now(),
    constraint billing_keys_pkey primary key (id),
    constraint billing_keys_status_check check (status in ('ACTIVE', 'REVOKED'))
);

comment on column billing_keys.status          is 'ACTIVE | REVOKED';
comment on column billing_keys.pg_billing_key  is 'Toss Payments billingKey value — treat as secret, never log.';
comment on column billing_keys.masked_card_number is 'Display-safe masked number e.g. 1234-****-****-5678';

-- One active billing key per tenant (partial unique index)
create unique index if not exists uq_billing_keys_tenant_active
    on billing_keys (tenant_id)
    where status = 'ACTIVE';

create index if not exists idx_billing_keys_tenant_id
    on billing_keys (tenant_id);

-- -----------------------------------------------------------------------
-- 3. subscriptions — Lumie's recurring contract with a tenant
-- -----------------------------------------------------------------------
-- Enum candidates:
--   status: 'ACTIVE' | 'PAUSED' | 'PAST_DUE' | 'CANCELLED' | 'EXPIRED' | 'PENDING'
--   suspension_reason: 'BILLING_KEY_REVOKED' | 'PAYMENT_FAILED_3X' | 'MANUAL_PAUSE'
create table if not exists subscriptions (
    id                   bigserial          not null,
    tenant_id            bigint             not null references tenants(id) on delete restrict,
    tenant_slug          varchar(30)        not null,
    plan_id              varchar(20)        not null references plans(id) on delete restrict,
    status               varchar(20)        not null,
    suspension_reason    varchar(50),
    billing_key_id       bigint             references billing_keys(id) on delete set null,
    started_at           timestamptz        not null,
    next_charge_at       timestamptz,
    expires_at           timestamptz,
    cancelled_at         timestamptz,
    billing_cycle_day    integer,
    version              bigint             not null default 0,
    created_at           timestamptz        not null default now(),
    updated_at           timestamptz        not null default now(),
    constraint subscriptions_pkey primary key (id),
    constraint subscriptions_tenant_id_key unique (tenant_id),
    constraint subscriptions_status_check check (
        status in ('ACTIVE', 'PAUSED', 'PAST_DUE', 'CANCELLED', 'EXPIRED', 'PENDING')
    )
);

comment on column subscriptions.status            is 'ACTIVE | PAUSED | PAST_DUE | CANCELLED | EXPIRED | PENDING';
comment on column subscriptions.suspension_reason is 'BILLING_KEY_REVOKED | PAYMENT_FAILED_3X | MANUAL_PAUSE — populated when status = PAUSED';
comment on column subscriptions.tenant_slug       is 'Denormalized from tenants.slug for debugging; kept in sync by application.';
comment on column subscriptions.billing_key_id    is 'Nullable: subscription may exist without an auto-debit key (trial, manual payment).';
comment on column subscriptions.billing_cycle_day is 'Day-of-month for recurring charge (1–28). NULL = non-recurring or manually managed.';

create index if not exists idx_subscriptions_plan_id
    on subscriptions (plan_id);
create index if not exists idx_subscriptions_billing_key_id
    on subscriptions (billing_key_id)
    where billing_key_id is not null;
create index if not exists idx_subscriptions_status_next_charge
    on subscriptions (status, next_charge_at)
    where status in ('ACTIVE', 'PAST_DUE');

-- -----------------------------------------------------------------------
-- 4. invoices — Lumie → Tenant subscription billing invoices
--    (distinct from tuition_invoices which are Academy → Guardian)
-- -----------------------------------------------------------------------
-- Enum candidates: status: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED'
create table if not exists invoices (
    id                     bigserial          not null,
    invoice_number         varchar(50)        not null,
    subscription_id        bigint             not null references subscriptions(id) on delete restrict,
    tenant_id              bigint             not null references tenants(id) on delete restrict,
    amount                 decimal(15, 0)     not null,
    vat_amount             decimal(15, 0)     not null default 0,
    status                 varchar(20)        not null,
    description            varchar(500),
    billing_period_start   timestamptz,
    billing_period_end     timestamptz,
    due_date               timestamptz,
    paid_at                timestamptz,
    payment_key            varchar(200),
    order_id               varchar(100),
    failure_reason         varchar(500),
    version                bigint             not null default 0,
    created_at             timestamptz        not null default now(),
    updated_at             timestamptz        not null default now(),
    constraint invoices_pkey primary key (id),
    constraint invoices_invoice_number_key unique (invoice_number),
    constraint invoices_status_check check (
        status in ('PENDING', 'PAID', 'FAILED', 'REFUNDED')
    )
);

comment on column invoices.status       is 'PENDING | PAID | FAILED | REFUNDED';
comment on column invoices.invoice_number is 'Idempotency key — generated by application, unique across Lumie.';
comment on column invoices.payment_key  is 'Toss one-time paymentKey (populated on PAID).';
comment on column invoices.order_id     is 'Toss orderId — idempotency key for PG retry.';

create index if not exists idx_invoices_subscription_id
    on invoices (subscription_id);
create index if not exists idx_invoices_tenant_id_status
    on invoices (tenant_id, status);
create index if not exists idx_invoices_created_at
    on invoices (created_at desc);

-- -----------------------------------------------------------------------
-- 5. alimtalk_credits — Kakao AlimTalk balance per tenant
-- -----------------------------------------------------------------------
create table if not exists alimtalk_credits (
    id                         bigserial      not null,
    tenant_id                  bigint         not null references tenants(id) on delete restrict,
    balance                    integer        not null default 0,
    auto_recharge_enabled      boolean        not null default false,
    auto_recharge_threshold    integer,
    auto_recharge_package      integer,
    version                    bigint         not null default 0,
    created_at                 timestamptz    not null default now(),
    updated_at                 timestamptz    not null default now(),
    constraint alimtalk_credits_pkey primary key (id),
    constraint alimtalk_credits_tenant_id_key unique (tenant_id),
    constraint alimtalk_credits_balance_non_negative check (balance >= 0)
);

comment on column alimtalk_credits.balance               is 'Message credit count. Never negative (CHECK enforced).';
comment on column alimtalk_credits.auto_recharge_threshold is 'Trigger recharge when balance drops below this value. Meaningful only when auto_recharge_enabled = true.';
comment on column alimtalk_credits.auto_recharge_package   is 'Package size (message count) to purchase on auto-recharge.';

-- -----------------------------------------------------------------------
-- 6. payment_transactions — PG call audit log (immutable, append-only)
-- -----------------------------------------------------------------------
-- Enum candidates:
--   type: 'BILLING_CHARGE' | 'TUITION_PAYMENT' | 'REFUND' | 'BILLING_KEY_ISSUE' | 'MERCHANT_REGISTER'
--   direction: 'REQUEST' | 'RESPONSE' | 'WEBHOOK'
create table if not exists payment_transactions (
    id                bigserial          not null,
    transaction_id    varchar(64)        not null,
    tenant_id         bigint             references tenants(id) on delete set null,
    type              varchar(40)        not null,
    direction         varchar(20)        not null,
    idempotency_key   varchar(64),
    owner_entity      varchar(40),
    owner_id          bigint,
    pg_payload        jsonb              not null,
    http_status       integer,
    created_at        timestamptz        not null default now(),
    constraint payment_transactions_pkey primary key (id),
    constraint payment_transactions_transaction_id_key unique (transaction_id),
    constraint payment_transactions_direction_check check (
        direction in ('REQUEST', 'RESPONSE', 'WEBHOOK')
    )
);

comment on table  payment_transactions              is 'Immutable PG call audit log — no UPDATE or DELETE should ever occur on this table.';
comment on column payment_transactions.type         is 'BILLING_CHARGE | TUITION_PAYMENT | REFUND | BILLING_KEY_ISSUE | MERCHANT_REGISTER';
comment on column payment_transactions.direction    is 'REQUEST | RESPONSE | WEBHOOK';
comment on column payment_transactions.tenant_id    is 'NULL for Lumie-internal transactions (no tenant context).';
comment on column payment_transactions.owner_entity is 'Owning domain entity type: INVOICE | TUITION_INVOICE | BILLING_KEY';
comment on column payment_transactions.owner_id     is 'PK of the owner_entity row.';
comment on column payment_transactions.pg_payload   is 'Raw PG request/response or webhook body — stored as-is for audit.';

create index if not exists idx_payment_transactions_owner
    on payment_transactions (owner_entity, owner_id)
    where owner_entity is not null;
create index if not exists idx_payment_transactions_idempotency_key
    on payment_transactions (idempotency_key)
    where idempotency_key is not null;
create index if not exists idx_payment_transactions_created_at
    on payment_transactions (created_at desc);
create index if not exists idx_payment_transactions_tenant_id
    on payment_transactions (tenant_id)
    where tenant_id is not null;

-- -----------------------------------------------------------------------
-- 7. tax_invoices — 세금계산서 (Lumie ↔ Tenant, issued per subscription invoice)
-- -----------------------------------------------------------------------
-- Enum candidates: issue_status: 'PENDING' | 'ISSUED' | 'FAILED'
create table if not exists tax_invoices (
    id                      bigserial          not null,
    invoice_id              bigint             references invoices(id) on delete restrict,
    tenant_id               bigint             not null references tenants(id) on delete restrict,
    buyer_business_no       varchar(20)        not null,
    supplier_business_no    varchar(20)        not null,
    amount                  decimal(15, 0)     not null,
    vat_amount              decimal(15, 0)     not null,
    issue_status            varchar(20)        not null default 'PENDING',
    external_invoice_id     varchar(100),
    issued_at               timestamptz,
    version                 bigint             not null default 0,
    created_at              timestamptz        not null default now(),
    updated_at              timestamptz        not null default now(),
    constraint tax_invoices_pkey primary key (id),
    constraint tax_invoices_issue_status_check check (
        issue_status in ('PENDING', 'ISSUED', 'FAILED')
    )
);

comment on column tax_invoices.issue_status         is 'PENDING | ISSUED | FAILED';
comment on column tax_invoices.buyer_business_no    is '학원 사업자등록번호';
comment on column tax_invoices.supplier_business_no is 'Lumie 사업자등록번호 (configuration-sourced, stored for immutable record)';
comment on column tax_invoices.external_invoice_id  is 'PG or 팝빌 issued invoice ID for external reference.';

create index if not exists idx_tax_invoices_invoice_id
    on tax_invoices (invoice_id)
    where invoice_id is not null;
create index if not exists idx_tax_invoices_tenant_id
    on tax_invoices (tenant_id);

-- -----------------------------------------------------------------------
-- 8. Grants — runtime role (lumie_app) full DML on all V28 tables
--    Platform tables: no RLS, no tenant_id filter needed by lumie_app.
-- -----------------------------------------------------------------------
grant select, insert, update, delete on plans                to lumie_app;
grant select, insert, update, delete on billing_keys         to lumie_app;
grant select, insert, update, delete on subscriptions        to lumie_app;
grant select, insert, update, delete on invoices             to lumie_app;
grant select, insert, update, delete on alimtalk_credits     to lumie_app;
grant select, insert, update, delete on payment_transactions to lumie_app;
grant select, insert, update, delete on tax_invoices         to lumie_app;

grant usage, select on sequence billing_keys_id_seq         to lumie_app;
grant usage, select on sequence subscriptions_id_seq        to lumie_app;
grant usage, select on sequence invoices_id_seq             to lumie_app;
grant usage, select on sequence alimtalk_credits_id_seq     to lumie_app;
grant usage, select on sequence payment_transactions_id_seq to lumie_app;
grant usage, select on sequence tax_invoices_id_seq         to lumie_app;
