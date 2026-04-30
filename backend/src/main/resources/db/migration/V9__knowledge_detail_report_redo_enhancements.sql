ALTER TABLE documents
    ADD COLUMN sections_json json NULL AFTER summary,
    ADD COLUMN key_points_json json NULL AFTER sections_json,
    ADD COLUMN knowledge_tree_json json NULL AFTER key_points_json,
    ADD COLUMN content_length int NULL AFTER knowledge_tree_json;

CREATE INDEX idx_answer_records_redo_user
    ON answer_records (redo_of_question_id, user_id);
