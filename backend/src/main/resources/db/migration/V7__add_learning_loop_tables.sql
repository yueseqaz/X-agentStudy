CREATE TABLE IF NOT EXISTS plan_task_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    stage_index INT NOT NULL,
    task_index INT NOT NULL,
    task_text VARCHAR(512) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_plan_task_user_stage_task (plan_id, user_id, stage_index, task_index),
    INDEX idx_plan_task_records_plan_user_completed (plan_id, user_id, completed),
    CONSTRAINT fk_plan_task_records_plan FOREIGN KEY (plan_id) REFERENCES learning_plans(id),
    CONSTRAINT fk_plan_task_records_user FOREIGN KEY (user_id) REFERENCES users(id)
);

ALTER TABLE review_records
    ADD COLUMN due_at DATETIME(6) NULL,
    ADD COLUMN last_reviewed_at DATETIME(6) NULL,
    ADD COLUMN interval_days INT NOT NULL DEFAULT 1,
    ADD COLUMN ease_factor DOUBLE NOT NULL DEFAULT 2.5,
    ADD COLUMN mastery_score INT NOT NULL DEFAULT 0;

UPDATE review_records
SET due_at = COALESCE(recommended_at, CURRENT_TIMESTAMP(6))
WHERE due_at IS NULL;
