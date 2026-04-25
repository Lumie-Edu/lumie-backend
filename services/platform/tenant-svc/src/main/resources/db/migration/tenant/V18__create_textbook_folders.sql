-- textbook_folders 테이블
CREATE TABLE textbook_folders (
    id UUID DEFAULT gen_random_uuid() NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_id UUID,
    depth INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(255),
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    updated_by VARCHAR(255),
    CONSTRAINT textbook_folders_pkey PRIMARY KEY (id),
    CONSTRAINT fk_textbook_folders_parent FOREIGN KEY (parent_id)
        REFERENCES textbook_folders(id) ON DELETE RESTRICT,
    CONSTRAINT chk_textbook_folders_depth CHECK (depth >= 1 AND depth <= 5)
);

CREATE INDEX idx_textbook_folders_parent_id ON textbook_folders(parent_id);
CREATE UNIQUE INDEX uq_textbook_folders_root_name ON textbook_folders(name) WHERE parent_id IS NULL;
CREATE UNIQUE INDEX uq_textbook_folders_parent_name ON textbook_folders(parent_id, name) WHERE parent_id IS NOT NULL;

-- file_metadata에 folder_id 컬럼 추가
ALTER TABLE file_metadata ADD COLUMN folder_id UUID;
ALTER TABLE file_metadata ADD CONSTRAINT fk_file_metadata_folder
    FOREIGN KEY (folder_id) REFERENCES textbook_folders(id) ON DELETE RESTRICT;
CREATE INDEX idx_file_metadata_folder_id ON file_metadata(folder_id);
