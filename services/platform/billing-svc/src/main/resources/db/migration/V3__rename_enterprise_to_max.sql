-- Align billing plan id with TenantPlan enum (FREE, BASIC, PRO, MAX).
-- tenants.plan_id was migrated in app V9; this migration completes the rename
-- on the billing side so all surfaces use MAX.

INSERT INTO plans (
    id, name, description, monthly_price, yearly_price,
    max_students, max_academies, max_admins, omr_monthly_quota,
    custom_domains, advanced_analytics, priority_support, api_access, white_labeling,
    display_order, active,
    paddle_product_id, paddle_monthly_price_id, paddle_yearly_price_id,
    created_at, updated_at
)
SELECT
    'MAX', name, description, monthly_price, yearly_price,
    max_students, max_academies, max_admins, omr_monthly_quota,
    custom_domains, advanced_analytics, priority_support, api_access, white_labeling,
    display_order, active,
    paddle_product_id, paddle_monthly_price_id, paddle_yearly_price_id,
    created_at, updated_at
FROM plans
WHERE id = 'ENTERPRISE'
ON CONFLICT (id) DO NOTHING;

UPDATE subscriptions SET plan_id = 'MAX' WHERE plan_id = 'ENTERPRISE';

DELETE FROM plans WHERE id = 'ENTERPRISE';
