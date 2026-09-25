-- Contract phase for joossameng AI cache extraction.
--
-- Deploy order:
-- 1. Copy the tenant-1 rows to the joossameng_ai database and verify counts/content.
-- 2. Deploy analysis-svc configured only with JOOSSAMENG_AI_DATABASE_DSN.
-- 3. Verify the authenticated batch ZIP endpoint against the new service.
--
-- The source tables are no longer read by Lumie application code. This
-- migration intentionally removes their schema and RLS policies from the
-- chatbot-visible public database.

DROP TABLE IF EXISTS student_ai_feedbacks;
DROP TABLE IF EXISTS exam_ai_analyses;
