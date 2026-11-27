-- Step 2: daily_care_logs 테이블 생성 (당일 산책·식사·간식 공유 기록)

CREATE TABLE daily_care_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    pet_id       BIGINT       NOT NULL,
    member_id    BIGINT       NOT NULL,
    care_date    DATE         NOT NULL,
    care_type    VARCHAR(20)  NOT NULL,
    performed_at DATETIME(6)  NOT NULL,
    memo         VARCHAR(200) NULL,
    created_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_care_logs_pet FOREIGN KEY (pet_id) REFERENCES pets (id),
    CONSTRAINT fk_care_logs_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_care_pet_date ON daily_care_logs (pet_id, care_date, performed_at);
