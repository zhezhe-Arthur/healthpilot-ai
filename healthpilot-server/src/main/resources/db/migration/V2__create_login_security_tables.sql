
-- 登录限流计数器
CREATE TABLE auth_login_limits (
                                   limit_key VARCHAR(72)
                                                 CHARACTER SET ascii
                                       COLLATE ascii_bin NOT NULL,

                                   window_started_at DATETIME(6) NOT NULL,

                                   attempts INT NOT NULL DEFAULT 0,

                                   blocked_until DATETIME(6) NULL,

                                   PRIMARY KEY (limit_key)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;

-- 登录安全审计日志
CREATE TABLE auth_login_audit (
                                  id BIGINT NOT NULL AUTO_INCREMENT,

                                  user_id BIGINT NULL,

                                  email_hash CHAR(64)
                                         CHARACTER SET ascii
                                      COLLATE ascii_bin NOT NULL,

                                  ip_hash CHAR(64)
                                         CHARACTER SET ascii
                                      COLLATE ascii_bin NOT NULL,

                                  event_type VARCHAR(32) NOT NULL,

                                  occurred_at DATETIME(6) NOT NULL,

                                  PRIMARY KEY (id),

                                  INDEX idx_login_audit_time (occurred_at),

                                  INDEX idx_login_audit_user (user_id, occurred_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4;
