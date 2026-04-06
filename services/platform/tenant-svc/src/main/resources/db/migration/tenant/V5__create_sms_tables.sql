--
-- Name: sms_templates; Type: TABLE
--

CREATE TABLE sms_templates (
    id bigint NOT NULL,
    academy_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    content text NOT NULL,
    category character varying(30) DEFAULT 'GENERAL'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sms_templates_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE sms_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sms_templates_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE sms_templates_id_seq OWNED BY sms_templates.id;


--
-- Name: sms_messages; Type: TABLE
--

CREATE TABLE sms_messages (
    id bigint NOT NULL,
    academy_id bigint NOT NULL,
    sender_id bigint NOT NULL,
    recipient_type character varying(20) NOT NULL,
    recipients jsonb NOT NULL,
    title character varying(200),
    content text NOT NULL,
    template_id bigint,
    sent_at timestamp without time zone,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    success_count integer DEFAULT 0 NOT NULL,
    fail_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: sms_messages_id_seq; Type: SEQUENCE
--

CREATE SEQUENCE sms_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sms_messages_id_seq; Type: SEQUENCE OWNED BY
--

ALTER SEQUENCE sms_messages_id_seq OWNED BY sms_messages.id;


--
-- Name: sms_templates id; Type: DEFAULT
--

ALTER TABLE ONLY sms_templates ALTER COLUMN id SET DEFAULT nextval('sms_templates_id_seq'::regclass);


--
-- Name: sms_messages id; Type: DEFAULT
--

ALTER TABLE ONLY sms_messages ALTER COLUMN id SET DEFAULT nextval('sms_messages_id_seq'::regclass);


--
-- Name: sms_templates sms_templates_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY sms_templates
    ADD CONSTRAINT sms_templates_pkey PRIMARY KEY (id);


--
-- Name: sms_messages sms_messages_pkey; Type: CONSTRAINT
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_pkey PRIMARY KEY (id);


--
-- Name: idx_sms_templates_academy_id; Type: INDEX
--

CREATE INDEX idx_sms_templates_academy_id ON sms_templates USING btree (academy_id);


--
-- Name: idx_sms_templates_category; Type: INDEX
--

CREATE INDEX idx_sms_templates_category ON sms_templates USING btree (category);


--
-- Name: idx_sms_messages_academy_id; Type: INDEX
--

CREATE INDEX idx_sms_messages_academy_id ON sms_messages USING btree (academy_id);


--
-- Name: idx_sms_messages_sender_id; Type: INDEX
--

CREATE INDEX idx_sms_messages_sender_id ON sms_messages USING btree (sender_id);


--
-- Name: idx_sms_messages_status; Type: INDEX
--

CREATE INDEX idx_sms_messages_status ON sms_messages USING btree (status);


--
-- Name: idx_sms_messages_template_id; Type: INDEX
--

CREATE INDEX idx_sms_messages_template_id ON sms_messages USING btree (template_id);


--
-- Name: sms_templates sms_templates_academy_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY sms_templates
    ADD CONSTRAINT sms_templates_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: sms_messages sms_messages_academy_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: sms_messages sms_messages_sender_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES admins(id) ON DELETE CASCADE;


--
-- Name: sms_messages sms_messages_template_id_fkey; Type: FK CONSTRAINT
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_template_id_fkey FOREIGN KEY (template_id) REFERENCES sms_templates(id) ON DELETE SET NULL;
