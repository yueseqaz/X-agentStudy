CREATE TABLE IF NOT EXISTS summary_cards (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    document_id BIGINT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    source_document_name VARCHAR(255) NULL,
    image_data LONGTEXT NULL,
    content_json JSON NOT NULL,
    template_type VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_summary_cards_plan_created (plan_id, created_at),
    INDEX idx_summary_cards_document (document_id),
    INDEX idx_summary_cards_user (user_id)
);
