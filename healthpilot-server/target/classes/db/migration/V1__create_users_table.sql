
-- 用户基础表
CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       email VARCHAR(190) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,

                       created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                       updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                       PRIMARY KEY (id),
                       UNIQUE KEY uk_users_email (email)

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
