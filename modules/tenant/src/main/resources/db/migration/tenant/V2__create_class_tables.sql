--
-- Name: classes; Type: TABLE
--

CREATE TABLE classes (
    id bigint NOT NULL,
    academy_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    subject character varying(100),
    teacher_id bigint,
    max_students integer DEFAULT 30 NOT NULL,
    tuition_amount bigint DEFAULT 0 NOT NULL,
    schedule_pattern jsonb,
    start_date date,
    end_date date,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    description text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: classes_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE classes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: classes_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE classes_id_seq OWNED BY classes.id;


--
-- Name: class_enrollments; Type: TABLE
--

CREATE TABLE class_enrollments (
    id bigint NOT NULL,
    class_id bigint NOT NULL,
    student_id bigint NOT NULL,
    enrolled_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    dropped_at timestamp without time zone,
    status character varying(20) DEFAULT 'ENROLLED'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: class_enrollments_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE class_enrollments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: class_enrollments_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE class_enrollments_id_seq OWNED BY class_enrollments.id;


--
-- Name: classes id; Type: DEFAULT
--

ALTER TABLE ONLY classes ALTER COLUMN id SET DEFAULT nextval('classes_id_seq'::regclass);


--
-- Name: class_enrollments id; Type: DEFAULT
--

ALTER TABLE ONLY class_enrollments ALTER COLUMN id SET DEFAULT nextval('class_enrollments_id_seq'::regclass);


--
-- Name: classes classes_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY classes
    ADD CONSTRAINT classes_pkey PRIMARY KEY (id);


--
-- Name: class_enrollments class_enrollments_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_pkey PRIMARY KEY (id);


--
-- Name: class_enrollments class_enrollments_class_id_student_id_key; Type: CONSTRAINT
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_class_id_student_id_key UNIQUE (class_id, student_id);


--
-- Name: idx_classes_academy_id; Type: INDEX
--

CREATE INDEX idx_classes_academy_id ON classes USING btree (academy_id);


--
-- Name: idx_classes_teacher_id; Type: INDEX
--

CREATE INDEX idx_classes_teacher_id ON classes USING btree (teacher_id);


--
-- Name: idx_classes_status; Type: INDEX
--

CREATE INDEX idx_classes_status ON classes USING btree (status);


--
-- Name: idx_class_enrollments_class_id; Type: INDEX
--

CREATE INDEX idx_class_enrollments_class_id ON class_enrollments USING btree (class_id);


--
-- Name: idx_class_enrollments_student_id; Type: INDEX
--

CREATE INDEX idx_class_enrollments_student_id ON class_enrollments USING btree (student_id);


--
-- Name: classes classes_academy_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY classes
    ADD CONSTRAINT classes_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: classes classes_teacher_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY classes
    ADD CONSTRAINT classes_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES admins(id) ON DELETE SET NULL;


--
-- Name: class_enrollments class_enrollments_class_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_class_id_fkey FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE;


--
-- Name: class_enrollments class_enrollments_student_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;
