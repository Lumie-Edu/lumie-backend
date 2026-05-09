--
-- Name: class_journals; Type: TABLE
--

CREATE TABLE class_journals (
    id bigint NOT NULL,
    class_id bigint NOT NULL,
    academy_id bigint NOT NULL,
    teacher_id bigint NOT NULL,
    lesson_date date NOT NULL,
    title character varying(200) NOT NULL,
    content text,
    ai_summary text,
    ai_script text,
    media_file_id bigint,
    attachments jsonb,
    topics_covered jsonb,
    homework_note text,
    visibility character varying(20) DEFAULT 'TEACHER_ONLY'::character varying NOT NULL,
    processing_status character varying(20) DEFAULT 'COMPLETED'::character varying NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: class_journals_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE class_journals_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: class_journals_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE class_journals_id_seq OWNED BY class_journals.id;


--
-- Name: class_journals id; Type: DEFAULT
--

ALTER TABLE ONLY class_journals ALTER COLUMN id SET DEFAULT nextval('class_journals_id_seq'::regclass);


--
-- Name: class_journals class_journals_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY class_journals
    ADD CONSTRAINT class_journals_pkey PRIMARY KEY (id);


--
-- Name: class_journals class_journals_class_id_lesson_date_key; Type: CONSTRAINT
--

ALTER TABLE ONLY class_journals
    ADD CONSTRAINT class_journals_class_id_lesson_date_key UNIQUE (class_id, lesson_date);


--
-- Name: idx_class_journals_class_id; Type: INDEX
--

CREATE INDEX idx_class_journals_class_id ON class_journals USING btree (class_id);


--
-- Name: idx_class_journals_academy_id; Type: INDEX
--

CREATE INDEX idx_class_journals_academy_id ON class_journals USING btree (academy_id);


--
-- Name: idx_class_journals_teacher_id; Type: INDEX
--

CREATE INDEX idx_class_journals_teacher_id ON class_journals USING btree (teacher_id);


--
-- Name: idx_class_journals_lesson_date; Type: INDEX
--

CREATE INDEX idx_class_journals_lesson_date ON class_journals USING btree (lesson_date);


--
-- Name: idx_class_journals_status; Type: INDEX
--

CREATE INDEX idx_class_journals_status ON class_journals USING btree (status);


--
-- Name: class_journals class_journals_class_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY class_journals
    ADD CONSTRAINT class_journals_class_id_fkey FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE;


--
-- Name: class_journals class_journals_academy_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY class_journals
    ADD CONSTRAINT class_journals_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: class_journals class_journals_teacher_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY class_journals
    ADD CONSTRAINT class_journals_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES admins(id) ON DELETE SET NULL;
