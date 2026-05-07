ALTER TABLE learning_plans
    ADD COLUMN share_code VARCHAR(64);

CREATE UNIQUE INDEX idx_learning_plans_share_code ON learning_plans(share_code);
