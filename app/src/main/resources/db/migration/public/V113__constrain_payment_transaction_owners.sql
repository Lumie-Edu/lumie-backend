-- ============================================================================
-- V113: constrain payment transaction owners
--
-- Purpose:
--   Record tax-invoice issuance as a distinct external-billing audit type and
--   constrain new audit records to the supported type/owner combinations.
--
-- Historical audit records:
--   The constraint is deliberately NOT VALID. PostgreSQL enforces it for all
--   future INSERTs and UPDATEs but does not reinterpret or rewrite preexisting
--   append-only records whose former owner vocabulary may differ. Historical
--   records remain available for audit and are never deleted by this migration.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'payment_transactions_type_owner_check'
          and conrelid = 'payment_transactions'::regclass
    ) then
        alter table payment_transactions
            add constraint payment_transactions_type_owner_check
            check (
                owner_entity is not null
                and (
                    (type in ('SUBSCRIPTION_CHARGE', 'SUBSCRIPTION_REFUND')
                        and owner_entity in ('Invoice', 'Subscription'))
                    or (type = 'ALIMTALK_RECHARGE'
                        and owner_entity = 'AlimtalkCredit')
                    or (type in ('BILLING_KEY_ISSUE', 'BILLING_KEY_REVOKE')
                        and owner_entity = 'BillingKey')
                    or (type = 'TAX_INVOICE_ISSUE'
                        and owner_entity = 'TaxInvoice')
                )
            ) not valid;
    end if;
end $$;

comment on constraint payment_transactions_type_owner_check
    on payment_transactions is
    'Applies to new writes only until historical payment audit records are separately reviewed. Tax invoice issuance is owned by TaxInvoice, not Invoice.';
