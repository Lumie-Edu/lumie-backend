-- Plans table with seed data
CREATE TABLE IF NOT EXISTS plans (
    id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    monthly_price NUMERIC(15, 0) NOT NULL DEFAULT 0,
    yearly_price NUMERIC(15, 0) NOT NULL DEFAULT 0,
    max_students INTEGER NOT NULL DEFAULT 0,
    max_academies INTEGER NOT NULL DEFAULT 0,
    max_admins INTEGER NOT NULL DEFAULT 0,
    omr_monthly_quota INTEGER NOT NULL DEFAULT 0,
    custom_domains BOOLEAN NOT NULL DEFAULT FALSE,
    advanced_analytics BOOLEAN NOT NULL DEFAULT FALSE,
    priority_support BOOLEAN NOT NULL DEFAULT FALSE,
    api_access BOOLEAN NOT NULL DEFAULT FALSE,
    white_labeling BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed data for plans
INSERT INTO plans (id, name, description, monthly_price, yearly_price, max_students, max_academies, max_admins, omr_monthly_quota, custom_domains, advanced_analytics, priority_support, api_access, white_labeling, display_order, active)
VALUES
    ('FREE', 'Free', '무료 체험 플랜', 0, 0, 50, 1, 2, 100, FALSE, FALSE, FALSE, FALSE, FALSE, 1, TRUE),
    ('BASIC', 'Basic', '소규모 학원을 위한 기본 플랜', 29000, 290000, 200, 3, 5, 500, FALSE, FALSE, FALSE, TRUE, FALSE, 2, TRUE),
    ('PRO', 'Pro', '성장하는 학원을 위한 프로 플랜', 79000, 790000, 1000, 10, 20, 5000, TRUE, TRUE, TRUE, TRUE, FALSE, 3, TRUE),
    ('ENTERPRISE', 'Enterprise', '대형 학원 및 프랜차이즈를 위한 엔터프라이즈 플랜', 199000, 1990000, 2147483647, 2147483647, 2147483647, 2147483647, TRUE, TRUE, TRUE, TRUE, TRUE, 4, TRUE)
ON CONFLICT (id) DO NOTHING;
