CREATE TABLE IF NOT EXISTS questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    source_scope VARCHAR(64) NULL,
    type VARCHAR(32) NOT NULL,
    difficulty VARCHAR(32) NOT NULL,
    stem TEXT NOT NULL,
    options JSON NULL,
    standard_answer JSON NOT NULL,
    explanation TEXT NULL,
    knowledge_points JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_questions_plan_difficulty_type (plan_id, difficulty, type),
    CONSTRAINT fk_questions_plan FOREIGN KEY (plan_id) REFERENCES learning_plans(id)
);

CREATE TABLE IF NOT EXISTS answer_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_answer JSON NOT NULL,
    is_correct BOOLEAN NOT NULL,
    answered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_answer_records_question_user (question_id, user_id),
    CONSTRAINT fk_answer_records_question FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_answer_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);
