
-- ============================================
-- V4：统一健康画像数据库
-- 1. 基础健康画像
-- 2. 健康目标
-- 3. 每日健康记录
-- ============================================


-- 1. 基础健康画像：一位用户只能有一份
CREATE TABLE health_profiles (
                                 id BIGINT NOT NULL AUTO_INCREMENT,

                                 user_id BIGINT NOT NULL,

    -- 基本资料（允许用户只填写部分信息）
                                 birth_year SMALLINT UNSIGNED NULL,
                                 height_cm DECIMAL(5, 2) NULL,

    -- 日常活动水平
                                 activity_level VARCHAR(20) NULL,

    -- 生活习惯和主动填写的健康关注点
                                 sleep_schedule VARCHAR(100) NULL,
                                 health_notes VARCHAR(1000) NULL,

    -- 用于确定每日记录对应的当地日期
                                 timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',

    -- 为以后使用JPA @Version乐观锁预留
                                 version BIGINT NOT NULL DEFAULT 0,

                                 created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                 updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                 PRIMARY KEY (id),

    -- 保证同一用户只有一份基础画像
                                 UNIQUE KEY uk_health_profiles_user (user_id),

                                 CONSTRAINT fk_health_profiles_user
                                     FOREIGN KEY (user_id)
                                         REFERENCES users(id)
                                         ON DELETE CASCADE,

                                 CONSTRAINT chk_profile_birth_year
                                     CHECK (birth_year BETWEEN 1900 AND 2100),

                                 CONSTRAINT chk_profile_height
                                     CHECK (height_cm BETWEEN 30 AND 300),

                                 CONSTRAINT chk_profile_activity
                                     CHECK (
                                         activity_level IN (
                                                            'SEDENTARY',
                                                            'LIGHT',
                                                            'MODERATE',
                                                            'ACTIVE',
                                                            'VERY_ACTIVE'
                                             )
                                         )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


-- 2. 健康目标：一个用户可以建立多个目标
CREATE TABLE health_goals (
                              id BIGINT NOT NULL AUTO_INCREMENT,

                              user_id BIGINT NOT NULL,

    -- 目标类别、名称和说明
                              category VARCHAR(20) NOT NULL,
                              title VARCHAR(120) NOT NULL,
                              description VARCHAR(1000) NULL,

    -- 数值型目标可选，例如70 kg、每周150 min
                              target_value DECIMAL(10, 2) NULL,
                              target_unit VARCHAR(20) NULL,

                              target_date DATE NULL,

    -- ACTIVE / PAUSED / COMPLETED / CANCELLED
                              status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',

                              version BIGINT NOT NULL DEFAULT 0,

                              created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                              updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                              PRIMARY KEY (id),

    -- 按用户、状态及创建时间查询目标
                              KEY idx_health_goals_user_status
                                  (user_id, status, created_at),

                              CONSTRAINT fk_health_goals_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT chk_goal_category
                                  CHECK (
                                      category IN (
                                                   'BODY',
                                                   'MENTAL',
                                                   'SLEEP',
                                                   'DIET',
                                                   'EXERCISE',
                                                   'OTHER'
                                          )
                                      ),

                              CONSTRAINT chk_goal_status
                                  CHECK (
                                      status IN (
                                                 'ACTIVE',
                                                 'PAUSED',
                                                 'COMPLETED',
                                                 'CANCELLED'
                                          )
                                      ),

                              CONSTRAINT chk_goal_target_value
                                  CHECK (target_value >= 0),

    -- 数值和单位必须同时提供或同时为空
                              CONSTRAINT chk_goal_target_pair
                                  CHECK (
                                      (target_value IS NULL AND target_unit IS NULL)
                                          OR
                                      (target_value IS NOT NULL AND target_unit IS NOT NULL)
                                      )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;


-- 3. 每日健康记录：一个用户每天最多一条
CREATE TABLE health_daily_checkins (
                                       id BIGINT NOT NULL AUTO_INCREMENT,

                                       user_id BIGINT NOT NULL,

    -- 由用户当地时区确定的记录日期
                                       record_date DATE NOT NULL,

    -- 心理与情绪（1～5分）
                                       mood_score TINYINT UNSIGNED NULL,
                                       stress_score TINYINT UNSIGNED NULL,

    -- 睡眠数据
                                       sleep_minutes SMALLINT UNSIGNED NULL,
                                       sleep_quality TINYINT UNSIGNED NULL,

    -- 身体与运动
                                       weight_kg DECIMAL(6, 2) NULL,
                                       steps INT UNSIGNED NULL,
                                       activity_minutes SMALLINT UNSIGNED NULL,

    -- 饮食与饮水
                                       water_ml INT UNSIGNED NULL,
                                       diet_quality TINYINT UNSIGNED NULL,

    -- 备注和数据来源
                                       notes VARCHAR(2000) NULL,
                                       source VARCHAR(16) NOT NULL DEFAULT 'MANUAL',

                                       version BIGINT NOT NULL DEFAULT 0,

                                       created_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6),

                                       updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),

                                       PRIMARY KEY (id),

    -- 同一用户同一天不能重复创建记录
    -- 同时支持按用户、日期范围查询
                                       UNIQUE KEY uk_checkins_user_date
                                           (user_id, record_date),

                                       CONSTRAINT fk_health_checkins_user
                                           FOREIGN KEY (user_id)
                                               REFERENCES users(id)
                                               ON DELETE CASCADE,

                                       CONSTRAINT chk_checkin_mood
                                           CHECK (mood_score BETWEEN 1 AND 5),

                                       CONSTRAINT chk_checkin_stress
                                           CHECK (stress_score BETWEEN 1 AND 5),

                                       CONSTRAINT chk_checkin_sleep_minutes
                                           CHECK (sleep_minutes <= 1440),

                                       CONSTRAINT chk_checkin_sleep_quality
                                           CHECK (sleep_quality BETWEEN 1 AND 5),

                                       CONSTRAINT chk_checkin_weight
                                           CHECK (weight_kg BETWEEN 1 AND 700),

                                       CONSTRAINT chk_checkin_steps
                                           CHECK (steps <= 200000),

                                       CONSTRAINT chk_checkin_activity
                                           CHECK (activity_minutes <= 1440),

                                       CONSTRAINT chk_checkin_water
                                           CHECK (water_ml <= 30000),

                                       CONSTRAINT chk_checkin_diet_quality
                                           CHECK (diet_quality BETWEEN 1 AND 5),

                                       CONSTRAINT chk_checkin_source
                                           CHECK (
                                               source IN (
                                                          'MANUAL',
                                                          'DEVICE',
                                                          'IMPORT',
                                                          'MIXED'
                                                   )
                                               )

) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci;
