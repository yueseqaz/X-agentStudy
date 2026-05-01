ALTER TABLE users
    ADD COLUMN resource_manager BIT NOT NULL DEFAULT b'0';

CREATE TABLE IF NOT EXISTS collaborator_applications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    expertise VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL,
    review_note TEXT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_collaborator_applications_user (user_id),
    INDEX idx_collaborator_applications_status_created (status, created_at),
    CONSTRAINT fk_collaborator_applications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_collaborator_applications_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS resource_manager_grants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    granted_by BIGINT NOT NULL,
    note TEXT NULL,
    active BIT NOT NULL DEFAULT b'1',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_resource_manager_grants_user (user_id),
    CONSTRAINT fk_resource_manager_grants_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_resource_manager_grants_granter FOREIGN KEY (granted_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ingestion_sources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(180) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    enabled BIT NOT NULL DEFAULT b'1',
    source_category VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ingestion_sources_owner_updated (owner_user_id, updated_at),
    CONSTRAINT fk_ingestion_sources_owner FOREIGN KEY (owner_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS ingestion_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    target_url VARCHAR(512) NOT NULL,
    title_hint VARCHAR(255) NULL,
    summary_hint TEXT NULL,
    tags VARCHAR(512) NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_ingestion_tasks_owner_updated (owner_user_id, updated_at),
    INDEX idx_ingestion_tasks_source (source_id),
    CONSTRAINT fk_ingestion_tasks_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_ingestion_tasks_source FOREIGN KEY (source_id) REFERENCES ingestion_sources(id)
);

CREATE TABLE IF NOT EXISTS candidate_resources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    source_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary TEXT NULL,
    resource_url VARCHAR(512) NOT NULL,
    author_name VARCHAR(255) NULL,
    cover_image_url VARCHAR(512) NULL,
    duration_seconds INT NULL,
    tags VARCHAR(512) NULL,
    review_status VARCHAR(32) NOT NULL,
    content_capture_mode VARCHAR(32) NOT NULL,
    raw_content LONGTEXT NULL,
    review_note TEXT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    published_resource_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_candidate_resources_owner_updated (owner_user_id, updated_at),
    INDEX idx_candidate_resources_review_status (review_status, updated_at),
    INDEX idx_candidate_resources_published (published_resource_id),
    CONSTRAINT fk_candidate_resources_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    CONSTRAINT fk_candidate_resources_source FOREIGN KEY (source_id) REFERENCES ingestion_sources(id),
    CONSTRAINT fk_candidate_resources_task FOREIGN KEY (task_id) REFERENCES ingestion_tasks(id),
    CONSTRAINT fk_candidate_resources_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id),
    CONSTRAINT fk_candidate_resources_published FOREIGN KEY (published_resource_id) REFERENCES learning_resources(id)
);
