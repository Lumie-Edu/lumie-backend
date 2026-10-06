-- ============================================================================
-- V130: validate historical exam and billing constraints
--
-- The V94 current-submission FK and V113 payment audit CHECK have enforced
-- new writes since creation, but were left NOT VALID for historical rows.
-- Validation checks those existing rows without rewriting or deleting them.
-- A violation fails the migration and preserves the previous schema state.
-- ============================================================================

set local lock_timeout = '5s';
set local statement_timeout = '60s';

do $$
declare
    validated boolean;
begin
    select convalidated
      into validated
      from pg_constraint
     where conname = 'exam_results_current_submission_fkey'
       and conrelid = 'exam_results'::regclass
       and confrelid = 'omr_submissions'::regclass
       and contype = 'f';

    if not found then
        raise exception 'expected current-submission foreign key is missing';
    end if;

    if not validated then
        alter table exam_results
            validate constraint exam_results_current_submission_fkey;
    end if;

    select convalidated
      into validated
      from pg_constraint
     where conname = 'payment_transactions_type_owner_check'
       and conrelid = 'payment_transactions'::regclass
       and contype = 'c';

    if not found then
        raise exception 'expected payment audit owner check is missing';
    end if;

    if not validated then
        alter table payment_transactions
            validate constraint payment_transactions_type_owner_check;
    end if;
end $$;

comment on constraint payment_transactions_type_owner_check
    on payment_transactions is
    'Validated for historical and new audit rows; supported type/owner combinations are enforced.';
