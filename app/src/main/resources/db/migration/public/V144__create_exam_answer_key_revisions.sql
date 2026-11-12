-- Immutable Tenant-scoped audit of confirmed answer-key corrections.
-- Existing exam/results/OMR data is not backfilled by this migration.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

CREATE TABLE IF NOT EXISTS exam_answer_key_revisions (
    id                           BIGSERIAL PRIMARY KEY,
    tenant_id                    BIGINT NOT NULL,
    exam_id                      BIGINT NOT NULL,
    actor_user_id                BIGINT NOT NULL,
    reason                       VARCHAR(1000) NOT NULL,
    exam_version_before          BIGINT NOT NULL,
    exam_version_after           BIGINT NOT NULL,
    changed_answers              JSONB NOT NULL,
    regraded_result_count        INTEGER NOT NULL,
    skipped_manual_override_count INTEGER NOT NULL,
    updated_submission_count     INTEGER NOT NULL,
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_exam_answer_key_revisions_exam
        FOREIGN KEY (exam_id, tenant_id) REFERENCES exams(id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT fk_exam_answer_key_revisions_actor
        FOREIGN KEY (actor_user_id, tenant_id) REFERENCES users(id, tenant_id) ON DELETE RESTRICT,
    CONSTRAINT ck_exam_answer_key_revisions_reason
        CHECK (length(btrim(reason)) > 0),
    CONSTRAINT ck_exam_answer_key_revisions_counts
        CHECK (regraded_result_count >= 0
           AND skipped_manual_override_count >= 0
           AND updated_submission_count >= 0),
    CONSTRAINT ck_exam_answer_key_revisions_changed_answers
        CHECK (jsonb_typeof(changed_answers) = 'object' AND changed_answers <> '{}'::jsonb)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_exam_answer_key_revisions_id_tenant
    ON exam_answer_key_revisions (id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_exam_answer_key_revisions_tenant_exam_created
    ON exam_answer_key_revisions (tenant_id, exam_id, created_at DESC, id DESC);

ALTER TABLE exam_answer_key_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_answer_key_revisions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON exam_answer_key_revisions;
CREATE POLICY tenant_isolation ON exam_answer_key_revisions
    USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
    WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

GRANT SELECT, INSERT ON exam_answer_key_revisions TO lumie_app;
GRANT USAGE, SELECT ON SEQUENCE exam_answer_key_revisions_id_seq TO lumie_app;
REVOKE UPDATE, DELETE ON exam_answer_key_revisions FROM lumie_app;

COMMENT ON TABLE exam_answer_key_revisions IS
    'Append-only Tenant-scoped record of confirmed exam answer-key corrections.';
