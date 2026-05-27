-- Seed the platform Plan catalog so /admin/settings/billing has something to render.
-- Prior PoC tenants were created before the billing module existed, so the plans
-- table was empty and the plan-selector page raised an error.
--
-- NULL limit columns = unlimited on that dimension (matches the BE Plan.isUnlimited
-- check). Yearly price = monthly × 12 × ~0.83 (≈ 2 months free) for the paid tiers;
-- adjust before live launch when 사업자 KYC closes 2027/1.
--
-- Re-runnable: ON CONFLICT (id) DO NOTHING so the dev DB can absorb a manual seed
-- before this migration runs without breaking Flyway on the next boot.

INSERT INTO plans (
    id, name, description, monthly_price, yearly_price,
    max_students, max_academies, max_admins, omr_monthly_quota,
    custom_domains, advanced_analytics, priority_support, api_access, white_labeling,
    display_order, active
) VALUES
    (
        'FREE', 'Free',
        '서비스를 처음 둘러보거나 소규모 학원에 적합한 기본 플랜입니다.',
        0, 0,
        30, 1, 2, 50,
        false, false, false, false, false,
        0, true
    ),
    (
        'BASIC', 'Basic',
        '단일 학원 운영에 필요한 핵심 기능을 모두 포함합니다.',
        39000, 388800,
        100, 1, 5, 300,
        false, false, false, false, false,
        1, true
    ),
    (
        'PRO', 'Pro',
        '여러 학원/캠퍼스를 운영하거나 학생 수가 많은 학원을 위한 플랜입니다.',
        99000, 986400,
        500, 3, 15, 1000,
        true, true, true, false, false,
        2, true
    ),
    (
        'ENTERPRISE', 'Enterprise',
        '대형 프랜차이즈/지점망을 위한 무제한 플랜입니다. 별도 계약으로 도입합니다.',
        299000, 2978400,
        NULL, NULL, NULL, NULL,
        true, true, true, true, true,
        3, true
    )
ON CONFLICT (id) DO NOTHING;
