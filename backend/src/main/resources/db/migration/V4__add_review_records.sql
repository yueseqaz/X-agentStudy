CREATE TABLE IF NOT EXISTS review_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    knowledge_point VARCHAR(128) NOT NULL,
    reason TEXT NULL,
    priority_level INT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    recommended_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at DATETIME(6) NULL,
    INDEX idx_review_records_plan_user_completed (plan_id, user_id, completed),
    CONSTRAINT fk_review_records_plan FOREIGN KEY (plan_id) REFERENCES learning_plans(id),
    CONSTRAINT fk_review_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);
