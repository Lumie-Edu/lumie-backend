-- Drop legacy class_journals table
DROP TABLE IF EXISTS class_journals;

-- Create lectures table
CREATE TABLE lectures (
    id                  BIGSERIAL PRIMARY KEY,
    class_id            BIGINT NOT NULL,
    academy_id          BIGINT NOT NULL,
    teacher_id          BIGINT NOT NULL,
    title               VARCHAR(300) NOT NULL,
    description         TEXT,

    -- Video metadata
    video_file_key      VARCHAR(500),
    video_file_name     VARCHAR(255),
    video_file_size     BIGINT,
    video_duration      INTEGER,
    video_mime_type     VARCHAR(50),

    -- Processing results
    transcript          TEXT,
    subtitle_file_key   VARCHAR(500),
    ai_summary          TEXT,
    topics_extracted    JSONB,

    -- Status
    processing_status   VARCHAR(30) DEFAULT 'UPLOADING' NOT NULL,
    processing_error    TEXT,
    visibility          VARCHAR(20) DEFAULT 'TEACHER_ONLY' NOT NULL,
    status              VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,

    -- Upload tracking
    upload_id           VARCHAR(500),
    upload_completed    BOOLEAN DEFAULT FALSE NOT NULL,

    -- Dates
    lecture_date        DATE,
    created_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    -- Foreign keys
    CONSTRAINT fk_lectures_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT fk_lectures_academy FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE,
    CONSTRAINT fk_lectures_teacher FOREIGN KEY (teacher_id) REFERENCES admins(id) ON DELETE SET NULL
);

CREATE INDEX idx_lectures_class_id ON lectures(class_id);
CREATE INDEX idx_lectures_academy_id ON lectures(academy_id);
CREATE INDEX idx_lectures_teacher_id ON lectures(teacher_id);
CREATE INDEX idx_lectures_status ON lectures(status);
CREATE INDEX idx_lectures_processing ON lectures(processing_status);
CREATE INDEX idx_lectures_lecture_date ON lectures(lecture_date);
