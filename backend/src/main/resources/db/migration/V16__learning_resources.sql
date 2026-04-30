CREATE TABLE IF NOT EXISTS learning_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uploader_user_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT NULL,
    resource_type VARCHAR(32) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NULL,
    storage_key VARCHAR(512) NOT NULL,
    subject_name VARCHAR(120) NOT NULL,
    subject_scope VARCHAR(160) NOT NULL,
    tags VARCHAR(512) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    INDEX idx_learning_resources_created_at (created_at),
    INDEX idx_learning_resources_subject (subject_name, subject_scope)
);
