-- =====================================================================
-- RLS baseline: drop schema-per-tenant infrastructure, recreate every
-- tenant-scoped table in `public` with a `tenant_id` column + Postgres
-- Row-Level Security policy.
--
-- Generated from pg_dump of `tenant_inst_c704d223` (V33 state) plus the
-- V34 (timestamptz) and V35 (version column) transformations that the
-- old schema-per-tenant flow never applied due to the ownership bug.
--
-- Tenant test data is discarded — see NEXT_SESSION.md #22 follow-up and
-- the user-confirmed cutover plan at
-- /Users/bluemayne/.claude/plans/encapsulated-twirling-frost.md.
-- =====================================================================

-- 1. Drop every leftover tenant_* schema (test data only)
DO $$ DECLARE r RECORD; BEGIN
  FOR r IN SELECT schema_name FROM information_schema.schemata
           WHERE schema_name LIKE 'tenant_%' LOOP
    EXECUTE 'DROP SCHEMA ' || quote_ident(r.schema_name) || ' CASCADE';
  END LOOP;
END $$;

-- 2. Drop the obsolete schema_name column on public.tenants
ALTER TABLE public.tenants DROP COLUMN IF EXISTS schema_name;

-- 3. Drop the slim OWNER-directory public.users — replaced below with
--    the full tenant-scoped users table that now lives in public.
DROP TABLE IF EXISTS public.users CASCADE;

--
-- Name: activity_logs; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE activity_logs (
id bigint NOT NULL,
    actor_id bigint NOT NULL,
    actor_name character varying(100) NOT NULL,
    actor_role character varying(20) NOT NULL,
    action character varying(30) NOT NULL,
    entity_type character varying(50) NOT NULL,
    entity_id bigint,
    entity_name character varying(200),
    description character varying(500),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL
);


--
-- Name: activity_logs_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE activity_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: activity_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE activity_logs_id_seq OWNED BY activity_logs.id;


--
-- Name: admin_permissions; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE admin_permissions (
admin_id bigint NOT NULL,
    permission_code character varying(100) NOT NULL,
    access_level character varying(10) DEFAULT 'NONE'::character varying NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: admins; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE admins (
id bigint NOT NULL,
    user_id bigint NOT NULL,
    admin_memo text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_login_id character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(20),
    is_active boolean DEFAULT true NOT NULL,
    email character varying(255),
    employment_status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    next_execution_at timestamptz,
    last_executed_at timestamptz,
    last_result text,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
-- Name: announcements; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE announcements (
id bigint NOT NULL,
    author_id bigint,
    announcement_title character varying(200) NOT NULL,
    announcement_content text NOT NULL,
    is_it_asset_announcement boolean DEFAULT false NOT NULL,
    is_it_important_announcement boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
-- Name: assignment_submissions; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE assignment_submissions (
id bigint NOT NULL,
    assignment_id bigint NOT NULL,
    student_id bigint NOT NULL,
    content text,
    attachments jsonb,
    score integer,
    feedback text,
    submitted_at timestamptz,
    graded_at timestamptz,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: assignment_submissions_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE assignment_submissions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: assignment_submissions_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE assignment_submissions_id_seq OWNED BY assignment_submissions.id;


--
-- Name: assignments; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE assignments (
id bigint NOT NULL,
    class_id bigint NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    type character varying(30) DEFAULT 'HOMEWORK'::character varying NOT NULL,
    due_date timestamptz,
    max_score integer,
    attachments jsonb,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: assignments_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE assignments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: assignments_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE assignments_id_seq OWNED BY assignments.id;


--
-- Name: attendance_records; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE attendance_records (
id bigint NOT NULL,
    session_id bigint NOT NULL,
    student_id bigint NOT NULL,
    status character varying(20) DEFAULT 'ABSENT'::character varying NOT NULL,
    check_method character varying(20) DEFAULT 'MANUAL'::character varying NOT NULL,
    checked_at timestamptz,
    checked_by bigint,
    memo text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL
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
    name character varying(100) NOT NULL,
    session_date date NOT NULL,
    attendance_code character varying(6) NOT NULL,
    code_expires_at timestamptz,
    late_threshold_minutes integer DEFAULT 10 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    class_id bigint,
    class_name character varying(100),
    tenant_id bigint NOT NULL
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
-- Name: class_enrollments; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE class_enrollments (
id bigint NOT NULL,
    class_id bigint NOT NULL,
    student_id bigint NOT NULL,
    enrolled_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    dropped_at timestamptz,
    status character varying(20) DEFAULT 'ENROLLED'::character varying NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: class_enrollments_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE class_enrollments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: class_enrollments_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE class_enrollments_id_seq OWNED BY class_enrollments.id;


--
-- Name: classes; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE classes (
id bigint NOT NULL,
    name character varying(100) NOT NULL,
    teacher_id bigint,
    schedule_pattern jsonb,
    description text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: classes_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE classes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: classes_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE classes_id_seq OWNED BY classes.id;


--
-- Name: counseling_reservations; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE counseling_reservations (
id bigint NOT NULL,
    schedule_id bigint NOT NULL,
    student_id bigint NOT NULL,
    user_id bigint CONSTRAINT counseling_reservations_admin_id_not_null NOT NULL,
    consultation_content text,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    user_id bigint CONSTRAINT counseling_schedules_admin_id_not_null NOT NULL,
    date date NOT NULL,
    is_available boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    start_time time without time zone NOT NULL,
    end_time time without time zone NOT NULL,
    description text,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    omr_image_key character varying(500),
    phone_number character varying(20),
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    grading_type character varying(20) DEFAULT 'ABSOLUTE'::character varying NOT NULL,
    grade_scale character varying(20) DEFAULT 'NINE_GRADE'::character varying,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(255),
    updated_at timestamptz,
    updated_by character varying(255),
    folder_id uuid,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: homepage_config; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE homepage_config (
id bigint NOT NULL,
    template_id character varying(64) NOT NULL,
    template_version integer NOT NULL,
    published boolean DEFAULT false NOT NULL,
    sections jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: homepage_config_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE homepage_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: homepage_config_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE homepage_config_id_seq OWNED BY homepage_config.id;


--
-- Name: lectures; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE lectures (
id bigint NOT NULL,
    class_id bigint,
    teacher_id bigint,
    title character varying(300) NOT NULL,
    description text,
    visibility character varying(20) DEFAULT 'TEACHER_ONLY'::character varying NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    lecture_date date,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    youtube_url character varying(500),
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: lectures_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE lectures_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lectures_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE lectures_id_seq OWNED BY lectures.id;


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
    created_at timestamptz DEFAULT now() NOT NULL,
    updated_at timestamptz DEFAULT now() NOT NULL,
    tenant_id bigint NOT NULL
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
-- Name: qna_boards; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE qna_boards (
id bigint NOT NULL,
    qna_user_id bigint NOT NULL,
    qna_title character varying(200) NOT NULL,
    qna_content text NOT NULL,
    is_it_answered boolean DEFAULT false NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    user_id bigint,
    comment_content text NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    score integer NOT NULL,
    tenant_id bigint NOT NULL
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
-- Name: report_generation_jobs; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE report_generation_jobs (
id bigint NOT NULL,
    exam_id bigint NOT NULL,
    tenant_slug character varying(50) NOT NULL,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    total_reports integer DEFAULT 0 NOT NULL,
    processed_reports integer DEFAULT 0 NOT NULL,
    success_count integer DEFAULT 0 NOT NULL,
    fail_count integer DEFAULT 0 NOT NULL,
    student_ids jsonb,
    zip_file_key character varying(500),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    tenant_id bigint NOT NULL
);


--
-- Name: report_generation_jobs_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE report_generation_jobs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: report_generation_jobs_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE report_generation_jobs_id_seq OWNED BY report_generation_jobs.id;


--
-- Name: reviews; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE reviews (
id bigint NOT NULL,
    reviewer_name character varying(255) NOT NULL,
    review_title character varying(255) DEFAULT ''::character varying NOT NULL,
    review_content text NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
-- Name: sms_messages; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE sms_messages (
id bigint NOT NULL,
    sender_id bigint NOT NULL,
    recipient_type character varying(20) NOT NULL,
    recipients jsonb NOT NULL,
    title character varying(200),
    content text NOT NULL,
    template_id bigint,
    sent_at timestamptz,
    status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    success_count integer DEFAULT 0 NOT NULL,
    fail_count integer DEFAULT 0 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL
);


--
-- Name: sms_messages_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE sms_messages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sms_messages_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE sms_messages_id_seq OWNED BY sms_messages.id;


--
-- Name: sms_templates; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE sms_templates (
id bigint NOT NULL,
    name character varying(100) NOT NULL,
    content text NOT NULL,
    category character varying(30) DEFAULT 'GENERAL'::character varying NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: sms_templates_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE sms_templates_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sms_templates_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE sms_templates_id_seq OWNED BY sms_templates.id;


--
-- Name: students; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE students (
id bigint NOT NULL,
    user_id bigint NOT NULL,
    student_highschool character varying(100),
    student_birth_year integer,
    student_memo text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    user_login_id character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(20),
    parent_phone character varying(20),
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
-- Name: textbook_folders; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE textbook_folders (
id uuid DEFAULT gen_random_uuid() NOT NULL,
    name character varying(100) NOT NULL,
    parent_id uuid,
    depth integer DEFAULT 1 NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(255),
    updated_at timestamptz,
    updated_by character varying(255),
    CONSTRAINT chk_textbook_folders_depth CHECK (((depth >= 1) AND (depth <= 5))),
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: textbooks; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE textbooks (
id bigint NOT NULL,
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
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
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: users; Type: TABLE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE TABLE users (
id bigint NOT NULL,
    user_login_id character varying(50) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(100) NOT NULL,
    phone character varying(20),
    email character varying(255),
    role character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    avatar_seed character varying(50),
    oauth_provider character varying(50),
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamptz DEFAULT CURRENT_TIMESTAMP NOT NULL,
    tenant_id bigint NOT NULL,
    version bigint NOT NULL DEFAULT 0
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: tenant_inst_c704d223; Owner: -
--

CREATE SEQUENCE users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: tenant_inst_c704d223; Owner: -
--

ALTER SEQUENCE users_id_seq OWNED BY users.id;


--
-- Name: activity_logs id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY activity_logs ALTER COLUMN id SET DEFAULT nextval('activity_logs_id_seq'::regclass);


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
-- Name: assignment_submissions id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignment_submissions ALTER COLUMN id SET DEFAULT nextval('assignment_submissions_id_seq'::regclass);


--
-- Name: assignments id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignments ALTER COLUMN id SET DEFAULT nextval('assignments_id_seq'::regclass);


--
-- Name: attendance_records id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records ALTER COLUMN id SET DEFAULT nextval('attendance_records_id_seq'::regclass);


--
-- Name: attendance_sessions id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_sessions ALTER COLUMN id SET DEFAULT nextval('attendance_sessions_id_seq'::regclass);


--
-- Name: class_enrollments id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY class_enrollments ALTER COLUMN id SET DEFAULT nextval('class_enrollments_id_seq'::regclass);


--
-- Name: classes id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY classes ALTER COLUMN id SET DEFAULT nextval('classes_id_seq'::regclass);


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
-- Name: homepage_config id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY homepage_config ALTER COLUMN id SET DEFAULT nextval('homepage_config_id_seq'::regclass);


--
-- Name: lectures id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY lectures ALTER COLUMN id SET DEFAULT nextval('lectures_id_seq'::regclass);


--
-- Name: omr_grading_jobs id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY omr_grading_jobs ALTER COLUMN id SET DEFAULT nextval('omr_grading_jobs_id_seq'::regclass);


--
-- Name: permissions id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY permissions ALTER COLUMN id SET DEFAULT nextval('permissions_id_seq'::regclass);


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
-- Name: report_generation_jobs id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY report_generation_jobs ALTER COLUMN id SET DEFAULT nextval('report_generation_jobs_id_seq'::regclass);


--
-- Name: reviews id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY reviews ALTER COLUMN id SET DEFAULT nextval('reviews_id_seq'::regclass);


--
-- Name: sms_messages id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY sms_messages ALTER COLUMN id SET DEFAULT nextval('sms_messages_id_seq'::regclass);


--
-- Name: sms_templates id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY sms_templates ALTER COLUMN id SET DEFAULT nextval('sms_templates_id_seq'::regclass);


--
-- Name: students id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students ALTER COLUMN id SET DEFAULT nextval('students_id_seq'::regclass);


--
-- Name: textbooks id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY textbooks ALTER COLUMN id SET DEFAULT nextval('textbooks_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY users ALTER COLUMN id SET DEFAULT nextval('users_id_seq'::regclass);


--
-- Name: activity_logs activity_logs_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY activity_logs
    ADD CONSTRAINT activity_logs_pkey PRIMARY KEY (id);


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
-- Name: announcements announcements_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY announcements
    ADD CONSTRAINT announcements_pkey PRIMARY KEY (id);


--
-- Name: assignment_submissions assignment_submissions_assignment_id_student_id_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_assignment_id_student_id_key UNIQUE (assignment_id, student_id);


--
-- Name: assignment_submissions assignment_submissions_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_pkey PRIMARY KEY (id);


--
-- Name: assignments assignments_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignments_pkey PRIMARY KEY (id);


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
-- Name: class_enrollments class_enrollments_class_id_student_id_key; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_class_id_student_id_key UNIQUE (class_id, student_id);


--
-- Name: class_enrollments class_enrollments_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_pkey PRIMARY KEY (id);


--
-- Name: classes classes_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY classes
    ADD CONSTRAINT classes_pkey PRIMARY KEY (id);


--
-- Name: counseling_reservations counseling_reservations_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_reservations
    ADD CONSTRAINT counseling_reservations_pkey PRIMARY KEY (id);


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

--
-- Name: homepage_config homepage_config_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY homepage_config
    ADD CONSTRAINT homepage_config_pkey PRIMARY KEY (id);


--
-- Name: lectures lectures_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY lectures
    ADD CONSTRAINT lectures_pkey PRIMARY KEY (id);


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
-- Name: report_generation_jobs report_generation_jobs_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY report_generation_jobs
    ADD CONSTRAINT report_generation_jobs_pkey PRIMARY KEY (id);


--
-- Name: reviews reviews_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY reviews
    ADD CONSTRAINT reviews_pkey PRIMARY KEY (id);


--
-- Name: sms_messages sms_messages_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_pkey PRIMARY KEY (id);


--
-- Name: sms_templates sms_templates_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY sms_templates
    ADD CONSTRAINT sms_templates_pkey PRIMARY KEY (id);


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
-- Name: textbook_folders textbook_folders_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY textbook_folders
    ADD CONSTRAINT textbook_folders_pkey PRIMARY KEY (id);


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
-- Name: counseling_schedules uq_schedules_user_date_start; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_schedules
    ADD CONSTRAINT uq_schedules_user_date_start UNIQUE (user_id, date, start_time);


--
-- Name: attendance_records uq_session_student; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY attendance_records
    ADD CONSTRAINT uq_session_student UNIQUE (session_id, student_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_user_login_id_unique; Type: CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY users
    ADD CONSTRAINT users_user_login_id_unique UNIQUE (user_login_id);


--
-- Name: homepage_config_singleton; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX homepage_config_singleton ON homepage_config USING btree ((true));


--
-- Name: idx_activity_logs_actor_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_activity_logs_actor_id ON activity_logs USING btree (actor_id);


--
-- Name: idx_activity_logs_actor_role; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_activity_logs_actor_role ON activity_logs USING btree (actor_role);


--
-- Name: idx_activity_logs_created_at; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_activity_logs_created_at ON activity_logs USING btree (created_at DESC);


--
-- Name: idx_activity_logs_entity_type; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_activity_logs_entity_type ON activity_logs USING btree (entity_type);


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
-- Name: idx_assignment_submissions_assignment_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_assignment_submissions_assignment_id ON assignment_submissions USING btree (assignment_id);


--
-- Name: idx_assignment_submissions_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_assignment_submissions_student_id ON assignment_submissions USING btree (student_id);


--
-- Name: idx_assignments_class_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_assignments_class_id ON assignments USING btree (class_id);


--
-- Name: idx_assignments_status; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_assignments_status ON assignments USING btree (status);


--
-- Name: idx_att_records_session_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_records_session_id ON attendance_records USING btree (session_id);


--
-- Name: idx_att_records_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_records_student_id ON attendance_records USING btree (student_id);


--
-- Name: idx_att_sessions_code; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_sessions_code ON attendance_sessions USING btree (attendance_code);


--
-- Name: idx_att_sessions_date; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_att_sessions_date ON attendance_sessions USING btree (session_date);


--
-- Name: idx_attendance_sessions_class_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_attendance_sessions_class_id ON attendance_sessions USING btree (class_id);


--
-- Name: idx_class_enrollments_class_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_class_enrollments_class_id ON class_enrollments USING btree (class_id);


--
-- Name: idx_class_enrollments_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_class_enrollments_student_id ON class_enrollments USING btree (student_id);


--
-- Name: idx_classes_teacher_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_classes_teacher_id ON classes USING btree (teacher_id);


--
-- Name: idx_exam_results_exam_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exam_results_exam_id ON exam_results USING btree (exam_id);


--
-- Name: idx_exam_results_phone; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exam_results_phone ON exam_results USING btree (exam_id, phone_number) WHERE ((student_id IS NULL) AND (phone_number IS NOT NULL));


--
-- Name: idx_exam_results_student; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_exam_results_student ON exam_results USING btree (exam_id, student_id) WHERE (student_id IS NOT NULL);


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
-- Name: idx_file_metadata_folder_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_file_metadata_folder_id ON file_metadata USING btree (folder_id);


--
-- Name: idx_file_metadata_upload_completed; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_file_metadata_upload_completed ON file_metadata USING btree (upload_completed);


--
-- Name: idx_lectures_class_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_lectures_class_id ON lectures USING btree (class_id);


--
-- Name: idx_lectures_lecture_date; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_lectures_lecture_date ON lectures USING btree (lecture_date);


--
-- Name: idx_lectures_status; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_lectures_status ON lectures USING btree (status);


--
-- Name: idx_lectures_teacher_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_lectures_teacher_id ON lectures USING btree (teacher_id);


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
-- Name: idx_report_jobs_exam_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_report_jobs_exam_id ON report_generation_jobs USING btree (exam_id);


--
-- Name: idx_report_jobs_status; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_report_jobs_status ON report_generation_jobs USING btree (status);


--
-- Name: idx_reservations_schedule_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_reservations_schedule_id ON counseling_reservations USING btree (schedule_id);


--
-- Name: idx_reservations_student_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_reservations_student_id ON counseling_reservations USING btree (student_id);


--
-- Name: idx_schedules_date; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_schedules_date ON counseling_schedules USING btree (date);


--
-- Name: idx_schedules_start_time; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_schedules_start_time ON counseling_schedules USING btree (start_time);


--
-- Name: idx_schedules_user_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_schedules_user_id ON counseling_schedules USING btree (user_id);


--
-- Name: idx_sms_messages_sender_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_sms_messages_sender_id ON sms_messages USING btree (sender_id);


--
-- Name: idx_sms_messages_status; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_sms_messages_status ON sms_messages USING btree (status);


--
-- Name: idx_sms_messages_template_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_sms_messages_template_id ON sms_messages USING btree (template_id);


--
-- Name: idx_sms_templates_category; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_sms_templates_category ON sms_templates USING btree (category);


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
-- Name: idx_textbook_folders_parent_id; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_textbook_folders_parent_id ON textbook_folders USING btree (parent_id);


--
-- Name: idx_users_role; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE INDEX idx_users_role ON users USING btree (role);


--
-- Name: uq_exam_result_student; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX uq_exam_result_student ON exam_results USING btree (exam_id, student_id) WHERE (student_id IS NOT NULL);


--
-- Name: uq_textbook_folders_parent_name; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX uq_textbook_folders_parent_name ON textbook_folders USING btree (parent_id, name) WHERE (parent_id IS NOT NULL);


--
-- Name: uq_textbook_folders_root_name; Type: INDEX; Schema: tenant_inst_c704d223; Owner: -
--

CREATE UNIQUE INDEX uq_textbook_folders_root_name ON textbook_folders USING btree (name) WHERE (parent_id IS NULL);


--
-- Name: admin_permissions admin_permissions_admin_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admin_permissions
    ADD CONSTRAINT admin_permissions_admin_id_fkey FOREIGN KEY (admin_id) REFERENCES admins(id) ON DELETE CASCADE;


--
-- Name: admins admins_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY admins
    ADD CONSTRAINT admins_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: ai_chat_messages ai_chat_messages_conversation_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY ai_chat_messages
    ADD CONSTRAINT ai_chat_messages_conversation_id_fkey FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE;


--
-- Name: announcements announcements_author_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY announcements
    ADD CONSTRAINT announcements_author_id_fkey FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE SET NULL;


--
-- Name: assignment_submissions assignment_submissions_assignment_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_assignment_id_fkey FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE;


--
-- Name: assignment_submissions assignment_submissions_student_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignment_submissions
    ADD CONSTRAINT assignment_submissions_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;


--
-- Name: assignments assignments_class_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY assignments
    ADD CONSTRAINT assignments_class_id_fkey FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE;


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
-- Name: class_enrollments class_enrollments_class_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_class_id_fkey FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE;


--
-- Name: class_enrollments class_enrollments_student_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY class_enrollments
    ADD CONSTRAINT class_enrollments_student_id_fkey FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE;


--
-- Name: classes classes_teacher_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY classes
    ADD CONSTRAINT classes_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES admins(id) ON DELETE SET NULL;


--
-- Name: counseling_reservations counseling_reservations_schedule_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_reservations
    ADD CONSTRAINT counseling_reservations_schedule_id_fkey FOREIGN KEY (schedule_id) REFERENCES counseling_schedules(id) ON DELETE CASCADE;


--
-- Name: counseling_reservations counseling_reservations_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_reservations
    ADD CONSTRAINT counseling_reservations_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


--
-- Name: counseling_schedules counseling_schedules_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY counseling_schedules
    ADD CONSTRAINT counseling_schedules_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


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
-- Name: file_metadata fk_file_metadata_folder; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY file_metadata
    ADD CONSTRAINT fk_file_metadata_folder FOREIGN KEY (folder_id) REFERENCES textbook_folders(id) ON DELETE RESTRICT;


--
-- Name: lectures fk_lectures_teacher; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY lectures
    ADD CONSTRAINT fk_lectures_teacher FOREIGN KEY (teacher_id) REFERENCES admins(id) ON DELETE SET NULL;


--
-- Name: textbook_folders fk_textbook_folders_parent; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY textbook_folders
    ADD CONSTRAINT fk_textbook_folders_parent FOREIGN KEY (parent_id) REFERENCES textbook_folders(id) ON DELETE RESTRICT;


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
-- Name: qna_comments qna_comments_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY qna_comments
    ADD CONSTRAINT qna_comments_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;


--
-- Name: question_results question_results_exam_result_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY question_results
    ADD CONSTRAINT question_results_exam_result_id_fkey FOREIGN KEY (exam_result_id) REFERENCES exam_results(id) ON DELETE CASCADE;


--
-- Name: report_generation_jobs report_generation_jobs_exam_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY report_generation_jobs
    ADD CONSTRAINT report_generation_jobs_exam_id_fkey FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE;


--
-- Name: sms_messages sms_messages_sender_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_sender_id_fkey FOREIGN KEY (sender_id) REFERENCES admins(id) ON DELETE CASCADE;


--
-- Name: sms_messages sms_messages_template_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY sms_messages
    ADD CONSTRAINT sms_messages_template_id_fkey FOREIGN KEY (template_id) REFERENCES sms_templates(id) ON DELETE SET NULL;


--
-- Name: students students_user_id_fkey; Type: FK CONSTRAINT; Schema: tenant_inst_c704d223; Owner: -
--

ALTER TABLE ONLY students
    ADD CONSTRAINT students_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;


-- ============================================================
-- Enable RLS + tenant-isolation policy on every tenant-scoped table
-- ============================================================

ALTER TABLE activity_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE activity_logs FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON activity_logs
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE admin_permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE admin_permissions FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON admin_permissions
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE admins ENABLE ROW LEVEL SECURITY;
ALTER TABLE admins FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON admins
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE ai_chat_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_chat_messages FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ai_chat_messages
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE ai_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_conversations FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ai_conversations
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE ai_scheduled_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_scheduled_tasks FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ai_scheduled_tasks
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE announcements ENABLE ROW LEVEL SECURITY;
ALTER TABLE announcements FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON announcements
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE assignment_submissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE assignment_submissions FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON assignment_submissions
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE assignments ENABLE ROW LEVEL SECURITY;
ALTER TABLE assignments FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON assignments
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE attendance_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_records FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON attendance_records
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE attendance_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE attendance_sessions FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON attendance_sessions
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE class_enrollments ENABLE ROW LEVEL SECURITY;
ALTER TABLE class_enrollments FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON class_enrollments
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE classes ENABLE ROW LEVEL SECURITY;
ALTER TABLE classes FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON classes
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE counseling_reservations ENABLE ROW LEVEL SECURITY;
ALTER TABLE counseling_reservations FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON counseling_reservations
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE counseling_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE counseling_schedules FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON counseling_schedules
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE exam_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_results FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_results
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE exam_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE exam_templates FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exam_templates
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
ALTER TABLE exams FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON exams
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE file_metadata ENABLE ROW LEVEL SECURITY;
ALTER TABLE file_metadata FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON file_metadata
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE homepage_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE homepage_config FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON homepage_config
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE lectures ENABLE ROW LEVEL SECURITY;
ALTER TABLE lectures FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON lectures
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE omr_grading_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE omr_grading_jobs FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON omr_grading_jobs
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE permissions ENABLE ROW LEVEL SECURITY;
ALTER TABLE permissions FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON permissions
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE qna_boards ENABLE ROW LEVEL SECURITY;
ALTER TABLE qna_boards FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON qna_boards
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE qna_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE qna_comments FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON qna_comments
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE question_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE question_results FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON question_results
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE report_generation_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE report_generation_jobs FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON report_generation_jobs
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE reviews FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON reviews
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE sms_messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE sms_messages FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sms_messages
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE sms_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE sms_templates FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON sms_templates
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE students ENABLE ROW LEVEL SECURITY;
ALTER TABLE students FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON students
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE textbook_folders ENABLE ROW LEVEL SECURITY;
ALTER TABLE textbook_folders FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON textbook_folders
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE textbooks ENABLE ROW LEVEL SECURITY;
ALTER TABLE textbooks FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON textbooks
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE toggles ENABLE ROW LEVEL SECURITY;
ALTER TABLE toggles FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON toggles
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE  ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON users
  USING (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint)
  WITH CHECK (tenant_id = NULLIF(current_setting('app.tenant_id', true), '')::bigint);

