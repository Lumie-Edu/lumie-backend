-- Invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id),
    tenant_slug VARCHAR(30) NOT NULL,
    amount NUMERIC(15, 0) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(500),
    billing_period_start TIMESTAMP,
    billing_period_end TIMESTAMP,
    due_date TIMESTAMP,
    paid_at TIMESTAMP,
    payment_key VARCHAR(200),
    order_id VARCHAR(100),
    failure_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for invoices
CREATE INDEX IF NOT EXISTS idx_invoices_subscription_id ON invoices(subscription_id);
CREATE INDEX IF NOT EXISTS idx_invoices_tenant_slug ON invoices(tenant_slug);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);
CREATE INDEX IF NOT EXISTS idx_invoices_order_id ON invoices(order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_due_date ON invoices(due_date);
