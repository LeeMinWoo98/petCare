-- Step 1: families, members, pets 초기 테이블 생성 (MariaDB InnoDB / utf8mb4)

CREATE TABLE families (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    invite_code VARCHAR(20) NOT NULL,
    name        VARCHAR(50) NULL,
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_families_invite_code UNIQUE (invite_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE members (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    family_id       BIGINT       NULL,
    email           VARCHAR(100) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    nickname        VARCHAR(30)  NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    is_family_owner TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    deleted_at      DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email),
    CONSTRAINT fk_members_family FOREIGN KEY (family_id) REFERENCES families (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_members_family_id ON members (family_id);

CREATE TABLE pets (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    family_id   BIGINT       NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    species     VARCHAR(20)  NOT NULL,
    breed       VARCHAR(50)  NULL,
    birth_date  DATE         NULL,
    gender      VARCHAR(20)  NOT NULL DEFAULT 'UNKNOWN',
    neutered    TINYINT(1)   NOT NULL DEFAULT 0,
    weight_kg   DECIMAL(5,2) NULL,
    notes       TEXT         NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    deleted_at  DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pets_family FOREIGN KEY (family_id) REFERENCES families (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_pets_family_deleted ON pets (family_id, deleted_at);
