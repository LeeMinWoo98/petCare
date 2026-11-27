-- Step 3: 동물용 의약품 카탈로그, 반려동물 등록 약, 당일 투약 기록 테이블 생성

CREATE TABLE medicine_catalog (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    external_code VARCHAR(100) NOT NULL,
    name          VARCHAR(150) NOT NULL,
    manufacturer  VARCHAR(100) NULL,
    efficacy      TEXT         NULL,
    dosage_guide  TEXT         NULL,
    precautions   TEXT         NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_med_catalog_external_code UNIQUE (external_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_med_catalog_name ON medicine_catalog (name);

CREATE TABLE pet_medicines (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    pet_id           BIGINT       NOT NULL,
    catalog_id       BIGINT       NULL,
    created_by       BIGINT       NOT NULL,
    source_type      VARCHAR(20)  NOT NULL,
    name             VARCHAR(150) NOT NULL,
    dosage_method    TEXT         NOT NULL,
    daily_dose_count INT          NOT NULL DEFAULT 1,
    total_doses      INT          NOT NULL,
    remaining_doses  INT          NOT NULL,
    start_date       DATE         NOT NULL,
    active           TINYINT(1)   NOT NULL DEFAULT 1,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    deleted_at       DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pet_med_pet FOREIGN KEY (pet_id) REFERENCES pets (id),
    CONSTRAINT fk_pet_med_catalog FOREIGN KEY (catalog_id) REFERENCES medicine_catalog (id),
    CONSTRAINT fk_pet_med_creator FOREIGN KEY (created_by) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pet_med_pet_active ON pet_medicines (pet_id, active, deleted_at);

CREATE TABLE medication_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    pet_medicine_id BIGINT       NOT NULL,
    member_id       BIGINT       NOT NULL,
    medicated_date  DATE         NOT NULL,
    medicated_at    DATETIME(6)  NOT NULL,
    memo            VARCHAR(200) NULL,
    created_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_med_logs_medicine FOREIGN KEY (pet_medicine_id) REFERENCES pet_medicines (id),
    CONSTRAINT fk_med_logs_member FOREIGN KEY (member_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_med_logs_med_date ON medication_logs (pet_medicine_id, medicated_date);
