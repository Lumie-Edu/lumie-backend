-- Announcement Reads Table
CREATE TABLE announcement_reads (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT NOT NULL REFERENCES announcements(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_announcement_reads UNIQUE (announcement_id, user_id)
);
CREATE INDEX idx_announcement_reads_announcement_id ON announcement_reads(announcement_id);

-- Textbook Reads Table
CREATE TABLE textbook_reads (
    id BIGSERIAL PRIMARY KEY,
    textbook_id BIGINT NOT NULL REFERENCES textbooks(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_textbook_reads UNIQUE (textbook_id, user_id)
);
CREATE INDEX idx_textbook_reads_textbook_id ON textbook_reads(textbook_id);

-- QnA Author Last Read At
ALTER TABLE qna_boards ADD COLUMN author_last_read_at TIMESTAMP WITH TIME ZONE;
