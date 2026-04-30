-- Remove transcription/summary pipeline columns from lectures.
-- The auto-transcription feature has been retired; only the YouTube URL is kept.

DROP INDEX IF EXISTS idx_lectures_processing;

ALTER TABLE lectures DROP COLUMN IF EXISTS transcript;
ALTER TABLE lectures DROP COLUMN IF EXISTS subtitle_file_key;
ALTER TABLE lectures DROP COLUMN IF EXISTS ai_summary;
ALTER TABLE lectures DROP COLUMN IF EXISTS topics_extracted;
ALTER TABLE lectures DROP COLUMN IF EXISTS processing_status;
ALTER TABLE lectures DROP COLUMN IF EXISTS processing_error;
ALTER TABLE lectures DROP COLUMN IF EXISTS video_duration;
