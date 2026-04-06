--
-- Name: assignments; Type: TABLE
--

CREATE TABLE assignments (
    id bigint NOT NULL,
    class_id bigint NOT NULL,
    academy_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    type character varying(30) DEFAULT 'HOMEWORK'::character varying NOT NULL,
    due_date timestamp without time zone,
    max_score integer,
    attachments jsonb,
    created_by bigint NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: assignments_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: assignments_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE assignments_id_seq OWNED BY assignments.id;


--
-- Name: assignment_submissions; Type: TABLE
--

CREATE TABLE assignment_submissions (
    id bigint NOT NULL,
    assignment_id bigint NOT NULL,
    student_id bigint NOT NULL,
    content text,
    attachments jsonb,
    score integer,
    feedback text,
    submitted_at timestamp without time zone,
    graded_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: assignment_submissions_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE assignment_submissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: assignment_submissions_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE assignment_submissions_id_seq OWNED BY assignment_submissions.id;


--
-- Name: assignments id; Type: DEFAULT
--

ALTER TABLE ONLY assignments ALTER COLUMN id SET DEFAULT nextval('assignments_id_seq'::regclass);


--
-- Name: assignment_submissions id; Type: DEFAULT
--

ALTER TABLE ONLY assignment_submissions ALTER COLUMN id SET DEFAULT nextval('assignment_submissions_id_seq'::regclass);


--
-- Name: assignments assignments_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignments_pkey PRIMARY KEY (id);


--
-- Name: assignment_submissions assignment_submissions_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_pkey PRIMARY KEY (id);


--
-- Name: assignment_submissions assignment_submissions_assignment_id_student_id_key; Type: CONSTRAINT
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_assignment_id_student_id_key UNIQUE (assignment_id, student_id);


--
-- Name: idx_assignments_class_id; Type: INDEX
--

CREATE INDEX idx_assignments_class_id ON assignments USING btree (class_id);


--
-- Name: idx_assignments_academy_id; Type: INDEX
--

CREATE INDEX idx_assignments_academy_id ON assignments USING btree (academy_id);


--
-- Name: idx_assignments_created_by; Type: INDEX
--

CREATE INDEX idx_assignments_created_by ON assignments USING btree (created_by);


--
-- Name: idx_assignments_status; Type: INDEX
--

CREATE INDEX idx_assignments_status ON assignments USING btree (status);


--
-- Name: idx_assignment_submissions_assignment_id; Type: INDEX
--

CREATE INDEX idx_assignment_submissions_assignment_id ON assignment_submissions USING btree (assignment_id);


--
-- Name: idx_assignment_submissions_student_id; Type: INDEX
--

CREATE INDEX idx_assignment_submissions_student_id ON assignment_submissions USING btree (student_id);


--
-- Name: assignments assignments_class_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignments_class_id_fkey FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE;


--
-- Name: assignments assignments_academy_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignments_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: assignments assignments_created_by_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignments_created_by_fkey FOREIGN KEY (created_by) REFERENCES admins(id) ON DELETE SET NULL;


--
-- Name: assignment_submissions assignment_submissions_assignment_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_assignment_id_fkey FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE;


--
-- Name: assignment_submissions assignment_submissions_student_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;
