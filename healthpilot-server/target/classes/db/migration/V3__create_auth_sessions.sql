
-- 每次登录对应一个独立会话
CREATE TABLE auth_sessions (
                               id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                               user_id BIGINT NOT NULL,
                               created_at DATETIME(6) NOT NULL,
                               expires_at DATETIME(6) NOT NULL,
                               revoked_at DATETIME(6) NULL,
                               last_used_at DATETIME(6) NULL,

                               PRIMARY KEY (id),

                               KEY idx_auth_sessions_user (user_id),
                               KEY idx_auth_sessions_expiry (expires_at),

                               CONSTRAINT fk_auth_sessions_user
                                   FOREIGN KEY (user_id)
                                       REFERENCES users(id)
                                       ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 保留Refresh Token的历史摘要，用于识别旧Token重放
CREATE TABLE auth_refresh_tokens (
                                     id BIGINT NOT NULL AUTO_INCREMENT,
                                     session_id CHAR(36)
                                            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                                     token_hash CHAR(64)
                                            CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
                                     issued_at DATETIME(6) NOT NULL,
                                     used_at DATETIME(6) NULL,

                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_refresh_token_hash (token_hash),
                                     KEY idx_refresh_session (session_id),

                                     CONSTRAINT fk_refresh_session
                                         FOREIGN KEY (session_id)
                                             REFERENCES auth_sessions(id)
                                             ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
