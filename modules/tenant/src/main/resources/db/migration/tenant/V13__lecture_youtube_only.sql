-- Remove direct upload columns, add YouTube URL support
ALTER TABLE lectures ADD COLUMN youtube_url VARCHAR(500);
ALTER TABLE lectures DROP COLUMN IF EXISTS video_file_key;
ALTER TABLE lectures DROP COLUMN IF EXISTS video_file_name;
ALTER TABLE lectures DROP COLUMN IF EXISTS video_file_size;
ALTER TABLE lectures DROP COLUMN IF EXISTS video_mime_type;
ALTER TABLE lectures DROP COLUMN IF EXISTS upload_id;
ALTER TABLE lectures DROP COLUMN IF EXISTS upload_completed;
ALTER TABLE lectures ALTER COLUMN processing_status SET DEFAULT 'PENDING_PROCESSING';
