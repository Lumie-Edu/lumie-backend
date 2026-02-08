-- V10: Seed default permissions
-- Permission categories: STUDENT, ADMIN, EXAM, SCHEDULE, SALARY, ATTENDANCE, ACADEMY, ANNOUNCEMENT

INSERT INTO permissions (code, name, category, description) VALUES
-- STUDENT
('STUDENT_VIEW', '학생 조회', 'STUDENT', '학생 목록 및 상세 정보 조회'),
('STUDENT_MANAGE', '학생 관리', 'STUDENT', '학생 등록, 수정, 삭제'),
('STUDENT_BULK_IMPORT', '학생 일괄 등록', 'STUDENT', '엑셀 파일을 통한 학생 일괄 등록'),

-- ADMIN
('ADMIN_VIEW', '직원 조회', 'ADMIN', '직원 목록 및 상세 정보 조회'),
('ADMIN_MANAGE', '직원 관리', 'ADMIN', '직원 등록, 수정, 비활성화'),
('ADMIN_PERMISSION', '권한 관리', 'ADMIN', '직책별 권한 설정'),

-- EXAM
('EXAM_VIEW', '시험 조회', 'EXAM', '시험 목록 및 결과 조회'),
('EXAM_MANAGE', '시험 관리', 'EXAM', '시험 등록, 수정, 삭제'),
('EXAM_GRADE', '채점 관리', 'EXAM', '시험 채점 및 성적 입력'),

-- SCHEDULE
('SCHEDULE_VIEW', '스케줄 조회', 'SCHEDULE', '근무 스케줄 조회'),
('SCHEDULE_MANAGE', '스케줄 관리', 'SCHEDULE', '근무 스케줄 설정 및 수정'),

-- SALARY
('SALARY_VIEW', '급여 조회', 'SALARY', '급여 정보 및 이력 조회'),
('SALARY_MANAGE', '급여 관리', 'SALARY', '급여 설정 및 급여명세서 관리'),

-- ATTENDANCE
('ATTENDANCE_VIEW', '출퇴근 조회', 'ATTENDANCE', '출퇴근 기록 조회'),
('ATTENDANCE_MANAGE', '출퇴근 관리', 'ATTENDANCE', '출퇴근 기록 수정 및 관리'),
('ATTENDANCE_CLOCK', '출퇴근 기록', 'ATTENDANCE', '본인 출퇴근 기록'),

-- ACADEMY
('ACADEMY_VIEW', '학원 조회', 'ACADEMY', '학원 정보 조회'),
('ACADEMY_MANAGE', '학원 관리', 'ACADEMY', '학원 정보 수정'),

-- ANNOUNCEMENT
('ANNOUNCEMENT_VIEW', '공지 조회', 'ANNOUNCEMENT', '공지사항 조회'),
('ANNOUNCEMENT_MANAGE', '공지 관리', 'ANNOUNCEMENT', '공지사항 등록, 수정, 삭제');
