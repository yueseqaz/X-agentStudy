ALTER TABLE community_questions
    ADD COLUMN plan_id BIGINT NULL AFTER user_id;

CREATE INDEX idx_community_questions_plan_id ON community_questions(plan_id);

ALTER TABLE community_questions
    ADD CONSTRAINT fk_community_questions_plan
        FOREIGN KEY (plan_id)
        REFERENCES learning_plans(id)
        ON DELETE SET NULL;
