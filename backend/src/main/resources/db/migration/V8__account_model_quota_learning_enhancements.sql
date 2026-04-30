ALTER TABLE users
    ADD COLUMN disabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_count INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME(6) NULL,
    ADD COLUMN last_login_at DATETIME(6) NULL;

ALTER TABLE model_configs
    ADD COLUMN api_key_cipher TEXT NULL;

ALTER TABLE answer_records
    ADD COLUMN score INT NULL,
    ADD COLUMN feedback TEXT NULL,
    ADD COLUMN redo_of_question_id BIGINT NULL;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_refresh_tokens_user_revoked (user_id, revoked),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS account_action_tokens (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    action_type VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_account_action_tokens_user_type (user_id, action_type),
    CONSTRAINT fk_account_action_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
