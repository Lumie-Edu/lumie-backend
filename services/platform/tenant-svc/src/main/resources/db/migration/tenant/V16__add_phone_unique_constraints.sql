-- Student: phone, parent_phone 각 컬럼에 UNIQUE (NULL 허용, 같은 컬럼 내 중복 방지)
CREATE UNIQUE INDEX idx_students_phone ON students(phone) WHERE phone IS NOT NULL;
CREATE UNIQUE INDEX idx_students_parent_phone ON students(parent_phone) WHERE parent_phone IS NOT NULL;

-- Admin: phone에 UNIQUE
CREATE UNIQUE INDEX idx_admins_phone ON admins(phone) WHERE phone IS NOT NULL;
