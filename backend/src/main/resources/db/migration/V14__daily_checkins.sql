CREATE TABLE IF NOT EXISTS daily_checkins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    checkin_date DATE NOT NULL,
    summary TEXT NULL,
    mood VARCHAR(32) NULL,
    study_minutes INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_daily_checkins_user_date (user_id, checkin_date),
    INDEX idx_daily_checkins_user_date (user_id, checkin_date),
    CONSTRAINT fk_daily_checkins_user FOREIGN KEY (user_id) REFERENCES users(id)
);
