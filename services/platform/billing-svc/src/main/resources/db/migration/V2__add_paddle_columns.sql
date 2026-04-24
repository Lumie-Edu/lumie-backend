-- V2: Add Paddle integration columns

-- Plans: map to Paddle Products & Prices
ALTER TABLE plans ADD COLUMN paddle_product_id VARCHAR(50);
ALTER TABLE plans ADD COLUMN paddle_monthly_price_id VARCHAR(50);
ALTER TABLE plans ADD COLUMN paddle_yearly_price_id VARCHAR(50);

-- Subscriptions: link to Paddle subscription & customer
ALTER TABLE subscriptions ADD COLUMN paddle_subscription_id VARCHAR(50);
ALTER TABLE subscriptions ADD COLUMN paddle_customer_id VARCHAR(50);
ALTER TABLE subscriptions ADD COLUMN billing_cycle VARCHAR(10);

-- Invoices: link to Paddle transaction
ALTER TABLE invoices ADD COLUMN paddle_transaction_id VARCHAR(50);
ALTER TABLE invoices ADD COLUMN currency VARCHAR(3) DEFAULT 'KRW';

-- Webhook idempotency
CREATE TABLE IF NOT EXISTS paddle_webhook_events (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_subscriptions_paddle_sub_id ON subscriptions(paddle_subscription_id);
CREATE INDEX IF NOT EXISTS idx_invoices_paddle_txn_id ON invoices(paddle_transaction_id);
