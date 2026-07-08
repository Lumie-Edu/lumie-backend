-- Dev seed data. Repeatable migration — re-runs when this file changes.
-- Idempotent; the demo row converges its plan_id on re-run so a stale
-- value can't wedge backend startup. Do NOT run on prod (excluded from
-- prod Flyway locations).
--
-- All people/data below are SYNTHETIC. No production PII. Every demo
-- account shares password: demo1234!  (bcrypt hash embedded below).

-- Demo tenant. plan_id must be a valid TenantPlan enum constant
-- (FREE | BASIC | PRO | MAX) — it is read back as the entity enum.
INSERT INTO tenants (slug, custom_id, name, display_name, status, plan_id, institute_name)
VALUES ('demo', 'demo', 'Demo Academy', '데모 학원', 'ACTIVE', 'FREE', '데모학원')
ON CONFLICT (slug) DO UPDATE SET
  custom_id = EXCLUDED.custom_id,
  display_name = EXCLUDED.display_name,
  plan_id = EXCLUDED.plan_id,
  institute_name = EXCLUDED.institute_name;

-- Domain demo data, scoped to the demo tenant. Converging seed: every
-- re-run wipes and re-inserts the demo tenant's domain rows so file
-- edits always reflect (no skip-guard, no duplicate rows).
-- Enum string values verified against backend VO enums:
--   Role(OWNER|MANAGER|INSTRUCTOR|STUDENT) · UserStatus(ACTIVE|INACTIVE)
--   EmploymentStatus(ACTIVE|...) · ExamCategory(GRADED|PASS_FAIL)
--   GradingType(RELATIVE|ABSOLUTE) · GradeScale(NINE_GRADE|FIVE_GRADE)
DO $seed$
DECLARE
  t_id   bigint;
  pw     text := '$2y$10$v.IkS0w0PAb/xPMHk/rVzONyW7roCm49CxQOUZ5vqNjQEqN0VfDSW';
BEGIN
  SELECT id INTO t_id FROM tenants WHERE slug = 'demo';
  IF t_id IS NULL THEN
    RAISE EXCEPTION 'demo tenant not found — tenants upsert must run first';
  END IF;

  -- Converge: wipe demo-tenant domain rows (FK-safe order) then
  -- re-insert from this file. Dev-only, demo-tenant-scoped.
  DELETE FROM exam_results           WHERE tenant_id = t_id;
  DELETE FROM omr_grading_jobs       WHERE tenant_id = t_id;
  DELETE FROM report_generation_jobs WHERE tenant_id = t_id;
  DELETE FROM classes                WHERE tenant_id = t_id;
  DELETE FROM exams                  WHERE tenant_id = t_id;
  DELETE FROM reviews                WHERE tenant_id = t_id;
  DELETE FROM announcements          WHERE tenant_id = t_id;
  DELETE FROM sms_messages           WHERE tenant_id = t_id;
  DELETE FROM sms_recipient_groups   WHERE tenant_id = t_id;
  DELETE FROM students               WHERE tenant_id = t_id;
  DELETE FROM staff                  WHERE tenant_id = t_id;
  DELETE FROM users                  WHERE tenant_id = t_id;

  -- Users: 1 owner + 1 manager + 1 instructor + 8 students.
  INSERT INTO users (user_login_id, password_hash, name, phone, email, role, status, tenant_id) VALUES
    ('demo_owner', pw, '김민준',  '010-0000-0001', 'owner@demo.lumie-edu.com', 'OWNER',      'ACTIVE', t_id),
    ('demo_mgr',   pw, '박서연',  '010-0000-0002', NULL,                        'MANAGER',    'ACTIVE', t_id),
    ('demo_inst',  pw, '이도윤',  '010-0000-0003', NULL,                        'INSTRUCTOR', 'ACTIVE', t_id),
    ('demo_stu_01', pw, '강하준', '010-0000-1001', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_02', pw, '윤지우', '010-0000-1002', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_03', pw, '임서진', '010-0000-1003', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_04', pw, '한예은', '010-0000-1004', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_05', pw, '오시윤', '010-0000-1005', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_06', pw, '배준호', '010-0000-1006', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_07', pw, '신가람', '010-0000-1007', NULL, 'STUDENT', 'ACTIVE', t_id),
    ('demo_stu_08', pw, '류하은', '010-0000-1008', NULL, 'STUDENT', 'ACTIVE', t_id);

  -- Students (structure/score profile mirrors a real cohort; identities synthetic).
  INSERT INTO students (user_id, student_highschool, student_birth_year, student_memo, is_active,
                         user_login_id, name, phone, parent_phone, tenant_id)
  SELECT u.id, v.hs, v.by, NULL, true, u.user_login_id, u.name,
         u.phone, '010-0000-20'||right(u.user_login_id, 2), t_id
  FROM users u
  JOIN (VALUES
    ('demo_stu_01','상암고등학교',2007),
    ('demo_stu_02','한가람고등학교',2007),
    ('demo_stu_03','대일고등학교',2007),
    ('demo_stu_04','김포고등학교',2005),
    ('demo_stu_05','경인고등학교',2007),
    ('demo_stu_06','여의도고등학교',2007),
    ('demo_stu_07','관악고등학교',2007),
    ('demo_stu_08','광영여자고등학교',2007)
  ) AS v(login, hs, by) ON v.login = u.user_login_id
  WHERE u.tenant_id = t_id;

  -- Staff (staff table).
  INSERT INTO staff (user_id, staff_memo, user_login_id, name, phone, is_active, email,
                      employment_status, tenant_id)
  SELECT u.id,
         CASE u.role WHEN 'OWNER' THEN '원장' WHEN 'MANAGER' THEN '총괄 매니저' ELSE '영어 강사' END,
         u.user_login_id, u.name, u.phone, true, u.email, 'ACTIVE', t_id
  FROM users u
  WHERE u.tenant_id = t_id AND u.role IN ('OWNER','MANAGER','INSTRUCTOR');

  -- Classes taught by the instructor / manager.
  INSERT INTO classes (name, teacher_id, schedule_pattern, description, tenant_id)
  SELECT '수능영어 정규반', a.id,
         '{"days":["MON","WED","FRI"],"startTime":"19:00","endTime":"21:00"}'::jsonb,
         '수능 영어 정규 커리큘럼반', t_id
  FROM staff a JOIN users u ON u.id = a.user_id
  WHERE u.user_login_id = 'demo_inst' AND a.tenant_id = t_id;

  INSERT INTO classes (name, teacher_id, schedule_pattern, description, tenant_id)
  SELECT '수능 파이널 실전반', a.id,
         '{"days":["TUE","THU"],"startTime":"18:00","endTime":"22:00"}'::jsonb,
         '수능 직전 실전 모의고사반', t_id
  FROM staff a JOIN users u ON u.id = a.user_id
  WHERE u.user_login_id = 'demo_mgr' AND a.tenant_id = t_id;

  -- Exams: 3 mock exams (graded / relative 9-grade). Answer keys are not PII.
  INSERT INTO exams (name, category, total_questions, correct_answers, question_scores,
                     question_types, pass_score, grading_type, grade_scale, tenant_id) VALUES
    ('모의고사 1회', 'GRADED', 45,
     '{"1":"1","2":"2","3":"3","4":"5","5":"4","6":"4","7":"3","8":"2","9":"4","10":"1","11":"3","12":"5","13":"2","14":"3","15":"1","16":"5","17":"1","18":"2","19":"2","20":"1","21":"3","22":"2","23":"3","24":"3","25":"5","26":"4","27":"3","28":"5","29":"4","30":"4","31":"2","32":"1","33":"2","34":"1","35":"4","36":"5","37":"3","38":"4","39":"5","40":"1","41":"1","42":"5","43":"5","44":"4","45":"2"}'::jsonb,
     '{"1":2,"2":2,"3":2,"4":2,"5":2,"6":2,"7":2,"8":2,"9":2,"10":2,"11":2,"12":3,"13":2,"14":3,"15":3,"16":2,"17":2,"18":2,"19":2,"20":2,"21":3,"22":2,"23":2,"24":2,"25":2,"26":2,"27":2,"28":2,"29":2,"30":3,"31":2,"32":2,"33":3,"34":3,"35":2,"36":2,"37":3,"38":2,"39":3,"40":2,"41":2,"42":3,"43":2,"44":2,"45":2}'::jsonb,
     '{"1":"듣기","2":"듣기","3":"듣기","4":"듣기","5":"듣기","6":"듣기","7":"듣기","8":"듣기","9":"듣기","10":"듣기","11":"듣기","12":"듣기","13":"듣기","14":"듣기","15":"듣기","16":"듣기","17":"듣기","18":"기초유형","19":"기초유형","20":"내용파악","21":"내용파악","22":"내용파악","23":"내용파악","24":"내용파악","25":"기초유형","26":"기초유형","27":"기초유형","28":"기초유형","29":"어법","30":"어휘","31":"빈칸","32":"빈칸","33":"빈칸","34":"빈칸","35":"내용파악","36":"순서","37":"순서","38":"삽입","39":"삽입","40":"내용파악","41":"내용파악","42":"내용파악","43":"내용파악","44":"내용파악","45":"내용파악"}'::jsonb,
     NULL, 'RELATIVE', 'NINE_GRADE', t_id),
    ('모의고사 2회', 'GRADED', 45,
     '{"1":"4","2":"3","3":"1","4":"4","5":"1","6":"3","7":"1","8":"5","9":"4","10":"5","11":"2","12":"2","13":"5","14":"2","15":"1","16":"2","17":"3","18":"5","19":"1","20":"5","21":"1","22":"5","23":"4","24":"1","25":"3","26":"5","27":"4","28":"2","29":"5","30":"4","31":"2","32":"1","33":"2","34":"1","35":"3","36":"2","37":"4","38":"4","39":"3","40":"1","41":"5","42":"4","43":"2","44":"4","45":"3"}'::jsonb,
     '{"1":2,"2":2,"3":2,"4":2,"5":2,"6":2,"7":2,"8":2,"9":2,"10":2,"11":2,"12":3,"13":2,"14":3,"15":3,"16":2,"17":2,"18":2,"19":2,"20":2,"21":3,"22":2,"23":2,"24":2,"25":2,"26":2,"27":2,"28":2,"29":2,"30":3,"31":2,"32":2,"33":3,"34":3,"35":2,"36":2,"37":3,"38":2,"39":3,"40":2,"41":2,"42":3,"43":2,"44":2,"45":2}'::jsonb,
     '{"1":"듣기","2":"듣기","3":"듣기","4":"듣기","5":"듣기","6":"듣기","7":"듣기","8":"듣기","9":"듣기","10":"듣기","11":"듣기","12":"듣기","13":"듣기","14":"듣기","15":"듣기","16":"듣기","17":"듣기","18":"기초유형","19":"기초유형","20":"내용파악","21":"내용파악","22":"내용파악","23":"내용파악","24":"내용파악","25":"기초유형","26":"기초유형","27":"기초유형","28":"기초유형","29":"어법","30":"어휘","31":"빈칸","32":"빈칸","33":"빈칸","34":"빈칸","35":"내용파악","36":"순서","37":"순서","38":"삽입","39":"삽입","40":"내용파악","41":"내용파악","42":"내용파악","43":"내용파악","44":"내용파악","45":"내용파악"}'::jsonb,
     NULL, 'RELATIVE', 'NINE_GRADE', t_id),
    ('모의고사 3회', 'GRADED', 45,
     '{"1":"5","2":"1","3":"1","4":"5","5":"4","6":"4","7":"2","8":"4","9":"4","10":"3","11":"3","12":"1","13":"5","14":"2","15":"2","16":"4","17":"3","18":"5","19":"2","20":"1","21":"5","22":"5","23":"3","24":"5","25":"3","26":"4","27":"2","28":"3","29":"2","30":"3","31":"1","32":"1","33":"1","34":"4","35":"2","36":"1","37":"2","38":"5","39":"3","40":"1","41":"2","42":"2","43":"4","44":"3","45":"5"}'::jsonb,
     '{"1":2,"2":2,"3":2,"4":2,"5":2,"6":2,"7":2,"8":2,"9":2,"10":2,"11":2,"12":3,"13":2,"14":3,"15":3,"16":2,"17":2,"18":2,"19":2,"20":2,"21":3,"22":2,"23":2,"24":2,"25":2,"26":2,"27":2,"28":2,"29":2,"30":3,"31":2,"32":2,"33":3,"34":3,"35":2,"36":2,"37":3,"38":2,"39":3,"40":2,"41":2,"42":3,"43":2,"44":2,"45":2}'::jsonb,
     '{"1":"듣기","2":"듣기","3":"듣기","4":"듣기","5":"듣기","6":"듣기","7":"듣기","8":"듣기","9":"듣기","10":"듣기","11":"듣기","12":"듣기","13":"듣기","14":"듣기","15":"듣기","16":"듣기","17":"듣기","18":"기초유형","19":"기초유형","20":"내용파악","21":"내용파악","22":"내용파악","23":"내용파악","24":"내용파악","25":"기초유형","26":"기초유형","27":"기초유형","28":"기초유형","29":"어법","30":"어휘","31":"빈칸","32":"빈칸","33":"빈칸","34":"빈칸","35":"내용파악","36":"순서","37":"순서","38":"삽입","39":"삽입","40":"내용파악","41":"내용파악","42":"내용파악","43":"내용파악","44":"내용파악","45":"내용파악"}'::jsonb,
     NULL, 'RELATIVE', 'NINE_GRADE', t_id);

  -- Exam results (synthetic score profile across exams 1 & 2).
  INSERT INTO exam_results (exam_id, student_id, total_score, grade, phone_number, tenant_id)
  SELECT e.id, s.id, r.score, r.grade, NULL, t_id
  FROM (VALUES
    ('모의고사 1회','demo_stu_02',77,3),
    ('모의고사 1회','demo_stu_03',83,2),
    ('모의고사 1회','demo_stu_05',87,2),
    ('모의고사 1회','demo_stu_01',74,3),
    ('모의고사 1회','demo_stu_07',97,1),
    ('모의고사 1회','demo_stu_08',89,2),
    ('모의고사 2회','demo_stu_05',79,3),
    ('모의고사 2회','demo_stu_04',55,5),
    ('모의고사 2회','demo_stu_06',57,5),
    ('모의고사 2회','demo_stu_01',77,3),
    ('모의고사 2회','demo_stu_07',88,2),
    ('모의고사 2회','demo_stu_02',70,3)
  ) AS r(exam_name, stu_login, score, grade)
  JOIN exams e   ON e.name = r.exam_name AND e.tenant_id = t_id
  JOIN students s ON s.user_login_id = r.stu_login AND s.tenant_id = t_id;

  -- Reviews (synthetic testimonials).
  INSERT INTO reviews (reviewer_name, review_title, review_content, tenant_id) VALUES
    ('학생 A', '믿고 따라간 한 학기',
     '4~5등급에서 시작했는데 정규반 커리큘럼을 끝까지 따라가니 수능에서 2등급을 받았습니다. 논리 전개 중심 풀이가 큰 도움이 됐어요.', t_id),
    ('학생 B', '멘탈 관리까지 챙겨주는 수업',
     '고1·2 때 6~7등급이었는데, 수업과 멘탈 케어 덕분에 공부 습관이 잡혔고 성적도 안정적으로 올랐습니다.', t_id),
    ('학생 C', '실전 감각이 확 늘었어요',
     '파이널 실전반에서 모의고사를 반복하니 시간 운영과 순서·삽입 유형 정답률이 크게 좋아졌습니다.', t_id);

  -- Announcements (authored by the demo owner).
  INSERT INTO announcements (author_id, announcement_title, announcement_content,
                             is_it_asset_announcement, is_it_important_announcement, tenant_id)
  SELECT u.id, 'Q&A 이용 안내',
         'Q&A 게시판 이용 규칙을 반드시 숙지한 뒤 질문을 등록해 주세요. 늘 응원합니다!',
         false, true, t_id
  FROM users u WHERE u.user_login_id = 'demo_owner' AND u.tenant_id = t_id;

  INSERT INTO announcements (author_id, announcement_title, announcement_content,
                             is_it_asset_announcement, is_it_important_announcement, tenant_id)
  SELECT u.id, '수능특강 영어 FINAL 자료 안내',
         '내신·수능 대비 FINAL 자료는 학원 내부 자료실에서 배포됩니다. 자세한 사항은 담당 강사에게 문의해 주세요.',
         true, true, t_id
  FROM users u WHERE u.user_login_id = 'demo_owner' AND u.tenant_id = t_id;

  RAISE NOTICE 'demo domain seed reapplied for tenant %', t_id;
END
$seed$;
