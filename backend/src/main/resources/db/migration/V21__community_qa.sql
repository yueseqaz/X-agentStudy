CREATE TABLE community_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    CONSTRAINT fk_community_questions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE community_answers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT NOT NULL,
    user_id BIGINT,
    source VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6),
    CONSTRAINT fk_community_answers_question
        FOREIGN KEY (question_id)
        REFERENCES community_questions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_community_answers_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_community_questions_updated_at ON community_questions(updated_at);
CREATE INDEX idx_community_answers_question_id ON community_answers(question_id);
