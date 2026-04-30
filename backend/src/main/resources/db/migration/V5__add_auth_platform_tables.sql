ALTER TABLE users
    ADD COLUMN role VARCHAR(32) NOT NULL DEFAULT 'USER';

UPDATE users
SET role = 'ADMIN'
WHERE id = 1;

CREATE TABLE IF NOT EXISTS model_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    base_url VARCHAR(255) NULL,
    api_key_mask VARCHAR(64) NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_model_configs_user_enabled (user_id, enabled),
    CONSTRAINT fk_model_configs_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS subscription_accounts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE,
    plan_code VARCHAR(32) NOT NULL,
    monthly_agent_quota INT NOT NULL,
    used_agent_calls INT NOT NULL DEFAULT 0,
    storage_quota_mb INT NOT NULL,
    used_storage_mb INT NOT NULL DEFAULT 0,
    period_start DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    period_end DATETIME(6) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_subscription_accounts_user FOREIGN KEY (user_id) REFERENCES users(id)
);
