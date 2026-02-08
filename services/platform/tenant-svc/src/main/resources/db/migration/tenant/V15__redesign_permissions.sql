-- 1. Drop existing position_permissions join table
DROP TABLE IF EXISTS position_permissions;

-- 2. Replace permissions data with 19 new sidebar-aligned permissions
TRUNCATE TABLE permissions CASCADE;
INSERT INTO permissions (code, name, category, description) VALUES
-- USER
('STUDENT_MANAGEMENT', '수강생 관리', 'USER', '수강생 목록 조회, 등록, 수정, 삭제'),
('STAFF_MANAGEMENT', '직원 관리', 'USER', '직원 목록 조회, 등록, 수정, 비활성화'),
('PERMISSION_MANAGEMENT', '권한 관리', 'USER', '직책별 권한 설정'),
('ATTENDANCE_MANAGEMENT', '출석 관리', 'USER', '출석 기록 조회 및 관리'),
-- LEARNING
('ASSIGNMENT_MANAGEMENT', '과제 관리', 'LEARNING', '과제 출제, 조회, 채점'),
('GRADE_MANAGEMENT', '성적 관리', 'LEARNING', '성적 조회 및 입력'),
('EXAM_GRADING', '시험 채점', 'LEARNING', '시험 채점 및 OMR 처리'),
('LEARNING_REPORT', '학습 리포트', 'LEARNING', '학습 분석 리포트 조회 및 생성'),
('LISTENING_CREATION', '리스닝 생성', 'LEARNING', '리스닝 문제 생성'),
-- COMMUNICATION
('ANNOUNCEMENT', '학원 공지', 'COMMUNICATION', '공지사항 조회, 등록, 수정'),
('SMS_SENDING', '문자 전송', 'COMMUNICATION', '문자 메시지 발송'),
('RESOURCE_MANAGEMENT', '학원 자료실', 'COMMUNICATION', '자료 업로드 및 관리'),
('QNA_MANAGEMENT', 'QnA 관리', 'COMMUNICATION', '질문/답변 조회 및 관리'),
('COUNSELING_MANAGEMENT', '상담 관리', 'COMMUNICATION', '상담 일정 및 기록 관리'),
-- OPERATION
('BILLING', '비용 수납', 'OPERATION', '수납 조회 및 관리'),
('TEXTBOOK_MANAGEMENT', '교재 관리', 'OPERATION', '교재 등록, 재고 관리'),
('SPREADSHEET', '스프레드시트', 'OPERATION', '스프레드시트 조회 및 편집'),
('ACADEMY_MANAGEMENT', '단과 관리', 'OPERATION', '단과 설정 및 관리'),
('REVIEW_MANAGEMENT', '리뷰 관리', 'OPERATION', '리뷰 조회 및 관리');

-- 3. Recreate position_permissions with access_level
CREATE TABLE position_permissions (
    position_id BIGINT NOT NULL REFERENCES positions(id) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL,
    access_level VARCHAR(10) NOT NULL DEFAULT 'NONE',
    PRIMARY KEY (position_id, permission_code)
);

CREATE INDEX idx_position_permissions_code ON position_permissions(permission_code);
