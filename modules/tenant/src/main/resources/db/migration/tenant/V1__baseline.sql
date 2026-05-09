-- =====================================================================
-- Baseline tenant schema snapshot from prod (tenant_inst_c704d223)
-- Generated: 2026-04-05
--
-- Consolidates historical V1..V21 tenant migrations into a single baseline.
-- Runs only in fresh environments; existing environments have this version
-- marked as BASELINE in flyway_schema_history and will skip execution.
--
-- Unqualified object names rely on Flyway setting search_path via schemas().
-- =====================================================================

--
-- PostgreSQL database dump
--


-- Dumped from database version 18.1 (Debian 18.1-1.pgdg11+2)
-- Dumped by pg_dump version 18.1 (Debian 18.1-1.pgdg11+2)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: academies; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE academies (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    address character varying(500),
    phone character varying(20),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: academies_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE academies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: academies_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE academies_id_seq OWNED BY academies.id;


--
-- Name: admin_academies; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE admin_academies (
    admin_id bigint NOT NULL,
    academy_id bigint NOT NULL
);


--
-- Name: admin_permissions; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE admin_permissions (
    admin_id bigint NOT NULL,
    permission_code character varying(100) NOT NULL,
    access_level character varying(10) DEFAULT 'NONE'::character varying NOT NULL
);


--
-- Name: admins; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE admins (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    admin_memo text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_login_id character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(20),
    is_active boolean DEFAULT true NOT NULL,
    position_id bigint,
    email character varying(255),
    employment_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL
);


--
-- Name: admins_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE admins_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: admins_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE admins_id_seq OWNED BY admins.id;


--
-- Name: ai_chat_messages; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE ai_chat_messages (
    id bigint NOT NULL,
    conversation_id bigint NOT NULL,
    role character varying(20) NOT NULL,
    content text,
    tool_call_id character varying(100),
    tool_calls jsonb,
    pending_action jsonb,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: ai_chat_messages_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE ai_chat_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_chat_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE ai_chat_messages_id_seq OWNED BY ai_chat_messages.id;


--
-- Name: ai_conversations; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE ai_conversations (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    title character varying(200),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: ai_conversations_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE ai_conversations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_conversations_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE ai_conversations_id_seq OWNED BY ai_conversations.id;


--
-- Name: ai_scheduled_tasks; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE ai_scheduled_tasks (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    tenant_slug character varying(100) NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    tool_name character varying(100) NOT NULL,
    tool_arguments jsonb,
    frequency character varying(20) NOT NULL,
    cron_expression character varying(100),
    next_execution_at timestamp without time zone,
    last_executed_at timestamp without time zone,
    last_result text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: ai_scheduled_tasks_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE ai_scheduled_tasks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_scheduled_tasks_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE ai_scheduled_tasks_id_seq OWNED BY ai_scheduled_tasks.id;


--
-- Name: announcement_academies; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE announcement_academies (
    announcement_id bigint NOT NULL,
    academy_id bigint NOT NULL
);


--
-- Name: announcements; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE announcements (
    id bigint NOT NULL,
    author_id bigint NOT NULL,
    announcement_title character varying(200) NOT NULL,
    announcement_content text NOT NULL,
    is_it_asset_announcement boolean DEFAULT false NOT NULL,
    is_it_important_announcement boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: announcements_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE announcements_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: announcements_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE announcements_id_seq OWNED BY announcements.id;


--
-- Name: attendance_records; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE attendance_records (
    id bigint NOT NULL,
    session_id bigint NOT NULL,
    student_id bigint NOT NULL,
    status character varying(20) DEFAULT 'ABSENT'::character varying NOT NULL,
    check_method character varying(20) DEFAULT 'MANUAL'::character varying NOT NULL,
    checked_at timestamp without time zone,
    checked_by bigint,
    memo text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: attendance_records_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE attendance_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attendance_records_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE attendance_records_id_seq OWNED BY attendance_records.id;


--
-- Name: attendance_sessions; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE attendance_sessions (
    id bigint NOT NULL,
    academy_id bigint NOT NULL,
    name character varying(100) NOT NULL,
    session_date date NOT NULL,
    subject character varying(100),
    attendance_code character varying(6) NOT NULL,
    code_expires_at timestamp without time zone,
    late_threshold_minutes integer DEFAULT 10 NOT NULL,
    status character varying(20) DEFAULT 'OPEN'::character varying NOT NULL,
    created_by bigint,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: attendance_sessions_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE attendance_sessions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: attendance_sessions_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE attendance_sessions_id_seq OWNED BY attendance_sessions.id;


--
-- Name: counseling_reservations; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE counseling_reservations (
    id bigint NOT NULL,
    schedule_id bigint NOT NULL,
    student_id bigint NOT NULL,
    admin_id bigint NOT NULL,
    consultation_content text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: counseling_reservations_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE counseling_reservations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: counseling_reservations_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE counseling_reservations_id_seq OWNED BY counseling_reservations.id;


--
-- Name: counseling_schedules; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE counseling_schedules (
    id bigint NOT NULL,
    admin_id bigint NOT NULL,
    date date NOT NULL,
    time_slot_id integer NOT NULL,
    is_available boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: counseling_schedules_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE counseling_schedules_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: counseling_schedules_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE counseling_schedules_id_seq OWNED BY counseling_schedules.id;


--
-- Name: exam_results; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE exam_results (
    id bigint NOT NULL,
    exam_id bigint NOT NULL,
    student_id bigint,
    total_score integer NOT NULL,
    grade integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    omr_image_key character varying(500),
    phone_number character varying(20)
);


--
-- Name: exam_results_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE exam_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exam_results_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE exam_results_id_seq OWNED BY exam_results.id;


--
-- Name: exam_templates; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE exam_templates (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    category character varying(20) DEFAULT 'GRADED'::character varying NOT NULL,
    total_questions integer NOT NULL,
    question_scores jsonb NOT NULL,
    question_types jsonb,
    pass_score integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: exam_templates_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE exam_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exam_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE exam_templates_id_seq OWNED BY exam_templates.id;


--
-- Name: exams; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE exams (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    category character varying(20) DEFAULT 'PASS_FAIL'::character varying NOT NULL,
    total_questions integer NOT NULL,
    correct_answers jsonb NOT NULL,
    question_scores jsonb NOT NULL,
    question_types jsonb,
    pass_score integer,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    grading_type character varying(20) DEFAULT 'ABSOLUTE'::character varying NOT NULL,
    grade_scale character varying(20) DEFAULT 'NINE_GRADE'::character varying
);


--
-- Name: exams_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE exams_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exams_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE exams_id_seq OWNED BY exams.id;


--
-- Name: file_metadata; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE file_metadata (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id bigint,
    original_filename character varying(255) NOT NULL,
    stored_filename character varying(255) NOT NULL,
    content_type character varying(100) NOT NULL,
    file_size bigint NOT NULL,
    object_key character varying(500) NOT NULL,
    upload_completed boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(255),
    updated_at timestamp without time zone,
    updated_by character varying(255)
);


--
-- Name: omr_grading_jobs; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE omr_grading_jobs (
    id bigint NOT NULL,
    exam_id bigint NOT NULL,
    tenant_slug character varying(50) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    total_images integer NOT NULL,
    processed_images integer DEFAULT 0 NOT NULL,
    success_count integer DEFAULT 0 NOT NULL,
    fail_count integer DEFAULT 0 NOT NULL,
    saved_count integer DEFAULT 0 NOT NULL,
    image_keys jsonb,
    results jsonb,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: omr_grading_jobs_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE omr_grading_jobs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: omr_grading_jobs_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE omr_grading_jobs_id_seq OWNED BY omr_grading_jobs.id;


--
-- Name: permissions; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE permissions (
    id bigint NOT NULL,
    code character varying(100) NOT NULL,
    name character varying(100) NOT NULL,
    category character varying(50) NOT NULL,
    description character varying(255),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: permissions_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE permissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: permissions_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE permissions_id_seq OWNED BY permissions.id;


--
-- Name: positions; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE positions (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(200),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: positions_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE positions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: positions_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE positions_id_seq OWNED BY positions.id;


--
-- Name: qna_boards; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE qna_boards (
    id bigint NOT NULL,
    qna_user_id bigint NOT NULL,
    qna_title character varying(200) NOT NULL,
    qna_content text NOT NULL,
    is_it_answered boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: qna_boards_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE qna_boards_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: qna_boards_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE qna_boards_id_seq OWNED BY qna_boards.id;


--
-- Name: qna_comments; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE qna_comments (
    id bigint NOT NULL,
    qna_id bigint NOT NULL,
    student_id bigint,
    admin_id bigint,
    comment_content text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: qna_comments_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE qna_comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: qna_comments_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE qna_comments_id_seq OWNED BY qna_comments.id;


--
-- Name: question_results; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE question_results (
    id bigint NOT NULL,
    exam_result_id bigint NOT NULL,
    question_number integer NOT NULL,
    selected_choice character varying(10),
    is_correct boolean NOT NULL,
    score integer NOT NULL
);


--
-- Name: question_results_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE question_results_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: question_results_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE question_results_id_seq OWNED BY question_results.id;


--
-- Name: reviews; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE reviews (
    id bigint NOT NULL,
    reviewer_name character varying(255) NOT NULL,
    review_title character varying(255) DEFAULT ''::character varying NOT NULL,
    review_content text NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: reviews_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE reviews_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: reviews_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE reviews_id_seq OWNED BY reviews.id;


--
-- Name: students; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE students (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    academy_id bigint NOT NULL,
    student_highschool character varying(100),
    student_birth_year integer,
    student_memo text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_login_id character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(20),
    parent_phone character varying(20)
);


--
-- Name: students_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE students_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: students_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE students_id_seq OWNED BY students.id;


--
-- Name: textbooks; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE textbooks (
    id bigint NOT NULL,
    academy_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    author character varying(100),
    publisher character varying(100),
    isbn character varying(20),
    subject character varying(100),
    grade_level character varying(50),
    price numeric(10,2),
    cover_image_path character varying(500),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: textbooks_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE textbooks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: textbooks_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE textbooks_id_seq OWNED BY textbooks.id;


--
-- Name: toggles; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE toggles (
    id bigint DEFAULT 1 NOT NULL,
    is_review_popup_on boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: academies id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY academies ALTER COLUMN id SET DEFAULT nextval('academies_id_seq'::regclass);


--
-- Name: admins id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admins ALTER COLUMN id SET DEFAULT nextval('admins_id_seq'::regclass);


--
-- Name: ai_chat_messages id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_chat_messages ALTER COLUMN id SET DEFAULT nextval('ai_chat_messages_id_seq'::regclass);


--
-- Name: ai_conversations id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_conversations ALTER COLUMN id SET DEFAULT nextval('ai_conversations_id_seq'::regclass);


--
-- Name: ai_scheduled_tasks id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_scheduled_tasks ALTER COLUMN id SET DEFAULT nextval('ai_scheduled_tasks_id_seq'::regclass);


--
-- Name: announcements id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY announcements ALTER COLUMN id SET DEFAULT nextval('announcements_id_seq'::regclass);


--
-- Name: attendance_records id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records ALTER COLUMN id SET DEFAULT nextval('attendance_records_id_seq'::regclass);


--
-- Name: attendance_sessions id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_sessions ALTER COLUMN id SET DEFAULT nextval('attendance_sessions_id_seq'::regclass);


--
-- Name: counseling_reservations id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_reservations ALTER COLUMN id SET DEFAULT nextval('counseling_reservations_id_seq'::regclass);


--
-- Name: counseling_schedules id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_schedules ALTER COLUMN id SET DEFAULT nextval('counseling_schedules_id_seq'::regclass);


--
-- Name: exam_results id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exam_results ALTER COLUMN id SET DEFAULT nextval('exam_results_id_seq'::regclass);


--
-- Name: exam_templates id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exam_templates ALTER COLUMN id SET DEFAULT nextval('exam_templates_id_seq'::regclass);


--
-- Name: exams id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exams ALTER COLUMN id SET DEFAULT nextval('exams_id_seq'::regclass);


--
-- Name: omr_grading_jobs id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY omr_grading_jobs ALTER COLUMN id SET DEFAULT nextval('omr_grading_jobs_id_seq'::regclass);


--
-- Name: permissions id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY permissions ALTER COLUMN id SET DEFAULT nextval('permissions_id_seq'::regclass);


--
-- Name: positions id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY positions ALTER COLUMN id SET DEFAULT nextval('positions_id_seq'::regclass);


--
-- Name: qna_boards id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY qna_boards ALTER COLUMN id SET DEFAULT nextval('qna_boards_id_seq'::regclass);


--
-- Name: qna_comments id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY qna_comments ALTER COLUMN id SET DEFAULT nextval('qna_comments_id_seq'::regclass);


--
-- Name: question_results id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY question_results ALTER COLUMN id SET DEFAULT nextval('question_results_id_seq'::regclass);


--
-- Name: reviews id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY reviews ALTER COLUMN id SET DEFAULT nextval('reviews_id_seq'::regclass);


--
-- Name: students id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students ALTER COLUMN id SET DEFAULT nextval('students_id_seq'::regclass);


--
-- Name: textbooks id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY textbooks ALTER COLUMN id SET DEFAULT nextval('textbooks_id_seq'::regclass);


--
-- Name: academies academies_name_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY academies
    ADD CONSTRAINT academies_name_key UNIQUE (name);


--
-- Name: academies academies_phone_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY academies
    ADD CONSTRAINT academies_phone_key UNIQUE (phone);


--
-- Name: academies academies_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY academies
    ADD CONSTRAINT academies_pkey PRIMARY KEY (id);


--
-- Name: admin_academies admin_academies_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admin_academies
    ADD CONSTRAINT admin_academies_pkey PRIMARY KEY (admin_id, academy_id);


--
-- Name: admin_permissions admin_permissions_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admin_permissions
    ADD CONSTRAINT admin_permissions_pkey PRIMARY KEY (admin_id, permission_code);


--
-- Name: admins admins_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admins
    ADD CONSTRAINT admins_pkey PRIMARY KEY (id);


--
-- Name: admins admins_user_id_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admins
    ADD CONSTRAINT admins_user_id_key UNIQUE (user_id);


--
-- Name: ai_chat_messages ai_chat_messages_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_pkey PRIMARY KEY (id);


--
-- Name: ai_conversations ai_conversations_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_conversations
    ADD CONSTRAINT ai_conversations_pkey PRIMARY KEY (id);


--
-- Name: ai_scheduled_tasks ai_scheduled_tasks_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_scheduled_tasks
    ADD CONSTRAINT ai_scheduled_tasks_pkey PRIMARY KEY (id);


--
-- Name: announcement_academies announcement_academies_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY announcement_academies
    ADD CONSTRAINT announcement_academies_pkey PRIMARY KEY (announcement_id, academy_id);


--
-- Name: announcements announcements_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY announcements
    ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);


--
-- Name: attendance_records attendance_records_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records
    ADD CONSTRAINT attendance_records_pkey PRIMARY KEY (id);


--
-- Name: attendance_sessions attendance_sessions_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_sessions
    ADD CONSTRAINT attendance_sessions_pkey PRIMARY KEY (id);


--
-- Name: counseling_reservations counseling_reservations_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_reservations
    ADD CONSTRAINT counseling_reservations_pkey PRIMARY KEY (id);


--
-- Name: counseling_schedules counseling_schedules_admin_id_date_time_slot_id_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_schedules
    ADD CONSTRAINT counseling_schedules_admin_id_date_time_slot_id_key UNIQUE (admin_id, date, time_slot_id);


--
-- Name: counseling_schedules counseling_schedules_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_schedules
    ADD CONSTRAINT counseling_schedules_pkey PRIMARY KEY (id);


--
-- Name: exam_results exam_results_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exam_results
    ADD CONSTRAINT exam_results_pkey PRIMARY KEY (id);


--
-- Name: exam_templates exam_templates_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exam_templates
    ADD CONSTRAINT exam_templates_pkey PRIMARY KEY (id);


--
-- Name: exams exams_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exams
    ADD CONSTRAINT exams_pkey PRIMARY KEY (id);


--
-- Name: file_metadata file_metadata_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY file_metadata
    ADD CONSTRAINT file_metadata_pkey PRIMARY KEY (id);


--
-- Name: omr_grading_jobs omr_grading_jobs_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY omr_grading_jobs
    ADD CONSTRAINT omr_grading_jobs_pkey PRIMARY KEY (id);


--
-- Name: permissions permissions_code_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY permissions
    ADD CONSTRAINT permissions_code_key UNIQUE (code);


--
-- Name: permissions permissions_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY permissions
    ADD CONSTRAINT permissions_pkey PRIMARY KEY (id);


--
-- Name: positions positions_name_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY positions
    ADD CONSTRAINT positions_name_key UNIQUE (name);


--
-- Name: positions positions_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY positions
    ADD CONSTRAINT positions_pkey PRIMARY KEY (id);


--
-- Name: qna_boards qna_boards_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY qna_boards
    ADD CONSTRAINT qna_boards_pkey PRIMARY KEY (id);


--
-- Name: qna_comments qna_comments_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY qna_comments
    ADD CONSTRAINT qna_comments_pkey PRIMARY KEY (id);


--
-- Name: question_results question_results_exam_result_id_question_number_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY question_results
    ADD CONSTRAINT question_results_exam_result_id_question_number_key UNIQUE (exam_result_id, question_number);


--
-- Name: question_results question_results_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY question_results
    ADD CONSTRAINT question_results_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: students students_user_id_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students
    ADD CONSTRAINT students_user_id_key UNIQUE (user_id);


--
-- Name: textbooks textbooks_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY textbooks
    ADD CONSTRAINT textbooks_pkey PRIMARY KEY (id);


--
-- Name: toggles toggles_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY toggles
    ADD CONSTRAINT toggles_pkey PRIMARY KEY (id);


--
-- Name: attendance_records uq_session_student; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records
    ADD CONSTRAINT uq_session_student UNIQUE (session_id, student_id);


--
-- Name: idx_admin_permissions_admin_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_admin_permissions_admin_id ON admin_permissions USING btree (admin_id);


--
-- Name: idx_admins_email; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_admins_email ON admins USING btree (email);


--
-- Name: idx_admins_employment_status; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_admins_employment_status ON admins USING btree (employment_status);


--
-- Name: idx_admins_phone; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX idx_admins_phone ON admins USING btree (phone) WHERE (phone IS NOT NULL);


--
-- Name: idx_admins_user_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_admins_user_id ON admins USING btree (user_id);


--
-- Name: idx_ai_chat_messages_conversation_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_ai_chat_messages_conversation_id ON ai_chat_messages USING btree (conversation_id);


--
-- Name: idx_ai_conversations_active; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_ai_conversations_active ON ai_conversations USING btree (user_id, is_active);


--
-- Name: idx_ai_conversations_user_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_ai_conversations_user_id ON ai_conversations USING btree (user_id);


--
-- Name: idx_ai_scheduled_tasks_status; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_ai_scheduled_tasks_status ON ai_scheduled_tasks USING btree (status, next_execution_at);


--
-- Name: idx_ai_scheduled_tasks_user_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_ai_scheduled_tasks_user_id ON ai_scheduled_tasks USING btree (user_id);


--
-- Name: idx_announcements_author_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_announcements_author_id ON announcements USING btree (author_id);


--
-- Name: idx_att_records_session_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_records_session_id ON attendance_records USING btree (session_id);


--
-- Name: idx_att_records_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_records_student_id ON attendance_records USING btree (student_id);


--
-- Name: idx_att_sessions_academy_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_sessions_academy_id ON attendance_sessions USING btree (academy_id);


--
-- Name: idx_att_sessions_code; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_sessions_code ON attendance_sessions USING btree (attendance_code);


--
-- Name: idx_att_sessions_date; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_sessions_date ON attendance_sessions USING btree (session_date);


--
-- Name: idx_exam_results_exam_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exam_results_exam_id ON exam_results USING btree (exam_id);


--
-- Name: idx_exam_results_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exam_results_student_id ON exam_results USING btree (student_id);


--
-- Name: idx_exams_category; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exams_category ON exams USING btree (category);


--
-- Name: idx_exams_grade_scale; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exams_grade_scale ON exams USING btree (grade_scale);


--
-- Name: idx_exams_grading_type; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exams_grading_type ON exams USING btree (grading_type);


--
-- Name: idx_file_metadata_entity_type_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_file_metadata_entity_type_id ON file_metadata USING btree (entity_type, entity_id);


--
-- Name: idx_file_metadata_upload_completed; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_file_metadata_upload_completed ON file_metadata USING btree (upload_completed);


--
-- Name: idx_permissions_category; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_permissions_category ON permissions USING btree (category);


--
-- Name: idx_qna_boards_qna_user_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_qna_boards_qna_user_id ON qna_boards USING btree (qna_user_id);


--
-- Name: idx_qna_comments_qna_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_qna_comments_qna_id ON qna_comments USING btree (qna_id);


--
-- Name: idx_question_results_exam_result_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_question_results_exam_result_id ON question_results USING btree (exam_result_id);


--
-- Name: idx_question_results_is_correct; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_question_results_is_correct ON question_results USING btree (is_correct);


--
-- Name: idx_question_results_question_number; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_question_results_question_number ON question_results USING btree (question_number);


--
-- Name: idx_reservations_schedule_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_reservations_schedule_id ON counseling_reservations USING btree (schedule_id);


--
-- Name: idx_reservations_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_reservations_student_id ON counseling_reservations USING btree (student_id);


--
-- Name: idx_schedules_admin_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_schedules_admin_id ON counseling_schedules USING btree (admin_id);


--
-- Name: idx_schedules_date; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_schedules_date ON counseling_schedules USING btree (date);


--
-- Name: idx_students_academy_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_students_academy_id ON students USING btree (academy_id);


--
-- Name: idx_students_parent_phone; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX idx_students_parent_phone ON students USING btree (parent_phone) WHERE (parent_phone IS NOT NULL);


--
-- Name: idx_students_phone; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX idx_students_phone ON students USING btree (phone) WHERE (phone IS NOT NULL);


--
-- Name: idx_students_user_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_students_user_id ON students USING btree (user_id);


--
-- Name: idx_textbooks_academy_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_textbooks_academy_id ON textbooks USING btree (academy_id);


--
-- Name: uq_exam_result_unregistered; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX uq_exam_result_unregistered ON exam_results USING btree (exam_id, phone_number) WHERE ((student_id IS NULL) AND (phone_number IS NOT NULL));


--
-- Name: admin_academies admin_academies_academy_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admin_academies
    ADD CONSTRAINT admin_academies_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: admin_academies admin_academies_admin_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admin_academies
    ADD CONSTRAINT admin_academies_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE;


--
-- Name: admin_permissions admin_permissions_admin_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admin_permissions
    ADD CONSTRAINT admin_permissions_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE;


--
-- Name: admins admins_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admins
    ADD CONSTRAINT admins_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: ai_chat_messages ai_chat_messages_conversation_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE;


--
-- Name: announcement_academies announcement_academies_announcement_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY announcement_academies
    ADD CONSTRAINT announcement_academies_announcement_id_fkey FOREIGN KEY (announcement_id) REFERENCES announcements(id) ON DELETE CASCADE;


--
-- Name: attendance_records attendance_records_session_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records
    ADD CONSTRAINT attendance_records_session_id_fkey FOREIGN KEY (session_id) REFERENCES attendance_sessions(id) ON DELETE CASCADE;


--
-- Name: attendance_records attendance_records_student_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records
    ADD CONSTRAINT attendance_records_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;


--
-- Name: attendance_sessions attendance_sessions_academy_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_sessions
    ADD CONSTRAINT attendance_sessions_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: counseling_reservations counseling_reservations_schedule_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_reservations
    ADD CONSTRAINT counseling_reservations_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES counseling_schedules(id) ON DELETE CASCADE;


--
-- Name: exam_results exam_results_exam_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exam_results
    ADD CONSTRAINT exam_results_exam_id_fkey FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE;


--
-- Name: exam_results exam_results_student_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY exam_results
    ADD CONSTRAINT exam_results_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;


--
-- Name: admins fk_admins_position; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admins
    ADD CONSTRAINT fk_admins_position FOREIGN KEY (position_id) REFERENCES positions(id);


--
-- Name: omr_grading_jobs omr_grading_jobs_exam_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY omr_grading_jobs
    ADD CONSTRAINT omr_grading_jobs_exam_id_fkey FOREIGN KEY (exam_id) REFERENCES exams(id);


--
-- Name: qna_comments qna_comments_qna_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY qna_comments
    ADD CONSTRAINT qna_comments_qna_id_fkey FOREIGN KEY (qna_id) REFERENCES qna_boards(id) ON DELETE CASCADE;


--
-- Name: question_results question_results_exam_result_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY question_results
    ADD CONSTRAINT question_results_exam_result_id_fkey FOREIGN KEY (exam_result_id) REFERENCES exam_results(id) ON DELETE CASCADE;


--
-- Name: students students_academy_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students
    ADD CONSTRAINT students_academy_id_fkey FOREIGN KEY (academy_id) REFERENCES academies(id) ON DELETE CASCADE;


--
-- Name: students students_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students
    ADD CONSTRAINT students_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--
