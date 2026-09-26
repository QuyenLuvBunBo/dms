CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(128) NOT NULL,
    email         VARCHAR(128) NULL,
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE settings (
    setting_key   VARCHAR(64)  NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE audit_logs (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    subject_type VARCHAR(40) NOT NULL,
    subject_id   BIGINT      NOT NULL,
    from_status  VARCHAR(30) NULL,
    to_status    VARCHAR(30) NOT NULL,
    user_id      BIGINT      NULL,
    created_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_audit_logs_subject (subject_type, subject_id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
