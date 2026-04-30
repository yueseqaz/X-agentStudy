CREATE TABLE IF NOT EXISTS qa_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    citations JSON NULL,
    related_points JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_qa_records_plan_created (plan_id, created_at),
    CONSTRAINT fk_qa_records_plan FOREIGN KEY (plan_id) REFERENCES learning_plans(id),
    CONSTRAINT fk_qa_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);
