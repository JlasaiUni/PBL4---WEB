-- ============================================================
--  V1 — Esquema inicial
-- ============================================================

CREATE TABLE IF NOT EXISTS roles (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    username            VARCHAR(50)  NOT NULL UNIQUE,
    email               VARCHAR(120) NOT NULL UNIQUE,
    password            VARCHAR(120) NOT NULL,
    full_name           VARCHAR(120),
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id   BIGINT NOT NULL,
    role_id   INT    NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS posts (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    content     TEXT         NOT NULL,
    published   BOOLEAN      NOT NULL DEFAULT FALSE,
    author_id   BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NULL,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tags (
    id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS post_tags (
    post_id  BIGINT NOT NULL,
    tag_id   BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id)  REFERENCES tags(id)  ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS comments (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    content     TEXT         NOT NULL,
    post_id     BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id)   REFERENCES posts(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_posts_published_created ON posts (published, created_at);
CREATE INDEX idx_posts_author            ON posts (author_id);
CREATE INDEX idx_comments_post           ON comments (post_id);
