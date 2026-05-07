CREATE TABLE  campaigns (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'ACTIVE', 'EXPIRED'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index for searching campaigns by status and end_at to quickly find active or expiring campaigns
CREATE INDEX idx_campaigns_status_end_at ON campaigns (status, end_at);

CREATE TABLE voucher_rules (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    conditions JSONB,
    total_quota INT NOT NULL,
    discount_type VARCHAR(20) NOT NULL, -- 'FIXED', 'PERCENTAGE'
    discount_value DECIMAL(19,4) NOT NULL,
    max_discount DECIMAL(19,4) NOT NULL,
    min_order_val DECIMAL(19,4) NOT NULL DEFAULT 0,
    CONSTRAINT fk_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE
);

CREATE TABLE user_voucher(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNUSED', -- 'UNUSED', 'USED', 'EXPIRED'
    claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP,
    CONSTRAINT fk_voucher_rule FOREIGN KEY (rule_id) REFERENCES voucher_rules(id)
);

-- Index for enforcing one voucher per user per rule
CREATE UNIQUE INDEX idx_unique_user_rule ON user_voucher(user_id, rule_id);

-- Index for searching user vouchers by user_id and status
CREATE INDEX idx_user_vouchers_user_status ON user_voucher(user_id, status);

CREATE TABLE order_vouchers(
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    user_voucher_id BIGINT NOT NULL,
    applied_discount DECIMAL(19,4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_voucher FOREIGN KEY (user_voucher_id) REFERENCES user_voucher(id)
);

-- Index for searching order vouchers by order_id
CREATE INDEX idx_order_vouchers_order_id ON order_vouchers(order_id);