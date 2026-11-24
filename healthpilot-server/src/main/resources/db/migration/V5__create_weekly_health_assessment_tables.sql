
-- V5: 每周健康反馈与健康画像快照

-- 1. 每周主观反馈
CREATE TABLE health_weekly_feedbacks (
                                         id BIGINT NOT NULL AUTO_INCREMENT,
                                         user_id BIGINT NOT NULL,
                                         week_start_date DATE NOT NULL,

                                         execution VARCHAR(16) NOT NULL,
                                         sleep VARCHAR(16) NOT NULL,
                                         feeling VARCHAR(16) NOT NULL,
                                         note VARCHAR(1000) NULL,

                                         version BIGINT NOT NULL DEFAULT 0,
                                         created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                         updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                         PRIMARY KEY (id),

                                         CONSTRAINT uk_weekly_feedback_user_week
                                             UNIQUE (user_id, week_start_date),

                                         CONSTRAINT fk_weekly_feedback_user
                                             FOREIGN KEY (user_id) REFERENCES users(id),

                                         CONSTRAINT chk_weekly_feedback_execution
                                             CHECK (execution IN ('all', 'most', 'few')),

                                         CONSTRAINT chk_weekly_feedback_sleep
                                             CHECK (sleep IN ('better', 'same', 'worse')),

                                         CONSTRAINT chk_weekly_feedback_feeling
                                             CHECK (feeling IN ('good', 'normal', 'bad')),

                                         CONSTRAINT chk_weekly_feedback_version
                                             CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2. 每周健康画像快照
CREATE TABLE health_profile_snapshots (
                                          id BIGINT NOT NULL AUTO_INCREMENT,
                                          user_id BIGINT NOT NULL,
                                          week_start_date DATE NOT NULL,

    -- 从当前基础画像复制，用于保留历史状态
                                          birth_year SMALLINT NULL,
                                          height_cm DECIMAL(5,2) NULL,
                                          activity_level VARCHAR(20) NULL,
                                          sleep_schedule VARCHAR(100) NULL,
                                          health_notes VARCHAR(1000) NULL,
                                          timezone VARCHAR(64) NULL,

    -- 用户每周自主更新的健康指标
                                          weight_kg DECIMAL(6,2) NULL,
                                          avg_sleep_minutes SMALLINT NULL,
                                          sleep_quality TINYINT NULL,
                                          mood_score TINYINT NULL,
                                          stress_score TINYINT NULL,
                                          diet_quality TINYINT NULL,
                                          activity_minutes INT NULL,

                                          version BIGINT NOT NULL DEFAULT 0,
                                          created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                          updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                          PRIMARY KEY (id),

                                          CONSTRAINT uk_health_snapshot_user_week
                                              UNIQUE (user_id, week_start_date),

                                          CONSTRAINT fk_health_snapshot_user
                                              FOREIGN KEY (user_id) REFERENCES users(id),

                                          CONSTRAINT chk_snapshot_birth_year
                                              CHECK (birth_year IS NULL OR birth_year BETWEEN 1900 AND 2100),

                                          CONSTRAINT chk_snapshot_height
                                              CHECK (height_cm IS NULL OR height_cm BETWEEN 50 AND 300),

                                          CONSTRAINT chk_snapshot_activity_level
                                              CHECK (
                                                  activity_level IS NULL OR
                                                  activity_level IN (
                                                                     'SEDENTARY', 'LIGHT', 'MODERATE',
                                                                     'ACTIVE', 'VERY_ACTIVE'
                                                      )
                                                  ),

                                          CONSTRAINT chk_snapshot_weight
                                              CHECK (weight_kg IS NULL OR weight_kg > 0),

                                          CONSTRAINT chk_snapshot_sleep_minutes
                                              CHECK (
                                                  avg_sleep_minutes IS NULL OR
                                                  avg_sleep_minutes BETWEEN 0 AND 1440
                                                  ),

                                          CONSTRAINT chk_snapshot_sleep_quality
                                              CHECK (sleep_quality IS NULL OR sleep_quality BETWEEN 1 AND 5),

                                          CONSTRAINT chk_snapshot_mood
                                              CHECK (mood_score IS NULL OR mood_score BETWEEN 1 AND 5),

                                          CONSTRAINT chk_snapshot_stress
                                              CHECK (stress_score IS NULL OR stress_score BETWEEN 1 AND 5),

                                          CONSTRAINT chk_snapshot_diet
                                              CHECK (diet_quality IS NULL OR diet_quality BETWEEN 1 AND 5),

                                          CONSTRAINT chk_snapshot_activity_minutes
                                              CHECK (
                                                  activity_minutes IS NULL OR
                                                  activity_minutes BETWEEN 0 AND 10080
                                                  ),

                                          CONSTRAINT chk_snapshot_version
                                              CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
