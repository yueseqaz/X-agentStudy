CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nickname VARCHAR(64) NOT NULL,
    account VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS learning_directions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NULL,
    description TEXT NULL,
    last_active_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,
    INDEX idx_learning_directions_user_deleted (user_id, deleted_at),
    CONSTRAINT fk_learning_directions_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS learning_profiles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    direction_id BIGINT NOT NULL,
    goal TEXT NULL,
    current_level VARCHAR(64) NULL,
    time_budget VARCHAR(128) NULL,
    preference TEXT NULL,
    risks JSON NULL,
    strategy TEXT NULL,
    raw_conversation JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_learning_profiles_direction FOREIGN KEY (direction_id) REFERENCES learning_directions(id)
);

CREATE TABLE IF NOT EXISTS learning_plans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    direction_id BIGINT NOT NULL,
    profile_id BIGINT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    goal TEXT NULL,
    stages JSON NULL,
    current_stage_index INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_learning_plans_direction_status (direction_id, status),
    CONSTRAINT fk_learning_plans_direction FOREIGN KEY (direction_id) REFERENCES learning_directions(id),
    CONSTRAINT fk_learning_plans_profile FOREIGN KEY (profile_id) REFERENCES learning_profiles(id)
);

CREATE TABLE IF NOT EXISTS documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    parse_status VARCHAR(32) NOT NULL,
    summary TEXT NULL,
    parse_error TEXT NULL,
    uploaded_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_documents_plan_status (plan_id, parse_status),
    CONSTRAINT fk_documents_plan FOREIGN KEY (plan_id) REFERENCES learning_plans(id)
);

CREATE TABLE IF NOT EXISTS knowledge_chunks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    source_location VARCHAR(255) NULL,
    vector_id VARCHAR(128) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_knowledge_chunks_plan_document (plan_id, document_id),
    CONSTRAINT fk_knowledge_chunks_plan FOREIGN KEY (plan_id) REFERENCES learning_plans(id),
    CONSTRAINT fk_knowledge_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id)
);

CREATE TABLE IF NOT EXISTS agent_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NULL,
    task_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    input_payload JSON NULL,
    output_payload JSON NULL,
    error_message TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_agent_tasks_plan_type_status (plan_id, task_type, status)
);

INSERT INTO users (id, nickname, account, password_hash)
VALUES (1, 'Demo User', 'demo@xagentstudy.local', NULL)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);
