ALTER TABLE users
    ADD COLUMN avatar_url longtext NULL AFTER role,
    ADD COLUMN disabled_until datetime(6) NULL AFTER disabled;

CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NULL,
    action VARCHAR(64) NOT NULL,
    detail TEXT NULL,
    created_at datetime(6) NOT NULL
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);
