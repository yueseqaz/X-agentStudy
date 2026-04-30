ALTER TABLE questions
    ADD COLUMN quiz_batch_id VARCHAR(64) NULL AFTER source_scope;

CREATE INDEX idx_questions_plan_source_batch
    ON questions (plan_id, source_scope, quiz_batch_id);

