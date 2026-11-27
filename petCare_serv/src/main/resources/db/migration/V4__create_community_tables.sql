-- Step 4: 반려동물 정보 공유 커뮤니티 게시판 (posts, comments) 테이블 생성

CREATE TABLE posts (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    author_id  BIGINT       NOT NULL,
    category   VARCHAR(30)  NOT NULL,
    title      VARCHAR(150) NOT NULL,
    content    TEXT         NOT NULL,
    view_count BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_posts_cat_created ON posts (deleted_at, category, created_at DESC);
CREATE INDEX idx_posts_created ON posts (deleted_at, created_at DESC);

CREATE TABLE comments (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    post_id    BIGINT      NOT NULL,
    author_id  BIGINT      NOT NULL,
    content    TEXT        NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES members (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comments_post_created ON comments (post_id, created_at);
