-- Add category_name to qna_boards for legacy data import from JoossamDB.
-- The legacy Prisma schema had a QnACategory table (4 entries) and a
-- categoryName text column on QnABoard. Lumie has no QnA category concept,
-- so we preserve the raw label as a free-form string rather than introducing
-- a new entity. Nullable because existing Lumie rows carry no category.
-- No check constraint intentionally — the set of valid labels is open-ended
-- and may evolve during import. No index needed (legacy corpus: 137 boards).

alter table qna_boards
    add column if not exists category_name varchar(50) null; -- legacy JoossamDB QnACategory label (e.g. '수업', '교재')
