
-- V6：创建每周健康计划及计划任务表

-- 1. 每周健康计划
CREATE TABLE health_plans (
                              id BIGINT NOT NULL AUTO_INCREMENT,

                              user_id BIGINT NOT NULL,
                              week_start_date DATE NOT NULL,

    -- DRAFT：AI生成待确认；CONFIRMED：用户已确认
                              status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    -- AI对本周计划的整体说明
                              ai_summary TEXT NULL,

    -- 乐观锁版本号
                              version BIGINT NOT NULL DEFAULT 0,

                              confirmed_at DATETIME(6) NULL,
                              created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                              updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                              PRIMARY KEY (id),

    -- 每个用户每周只有一份计划
                              CONSTRAINT uk_health_plan_user_week
                                  UNIQUE (user_id, week_start_date),

                              CONSTRAINT fk_health_plan_user
                                  FOREIGN KEY (user_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE,

                              CONSTRAINT chk_health_plan_status
                                  CHECK (status IN ('DRAFT', 'CONFIRMED')),

                              INDEX idx_health_plan_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- 2. 健康计划任务
CREATE TABLE health_plan_items (
                                   id BIGINT NOT NULL AUTO_INCREMENT,

                                   plan_id BIGINT NOT NULL,

    -- BODY、MENTAL、DIET、SLEEP、GOAL
                                   category VARCHAR(20) NOT NULL,

                                   title VARCHAR(150) NOT NULL,
                                   description VARCHAR(500) NULL,

    -- ISO星期：1=周一，7=周日，例如 1,3,5
                                   scheduled_weekdays VARCHAR(20) NOT NULL,

    -- 本周计划完成的数量及其单位
                                   target_quantity DECIMAL(8,2) NOT NULL,
                                   target_unit VARCHAR(20) NOT NULL,

                                   sort_order INT NOT NULL DEFAULT 0,

                                   created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

                                   PRIMARY KEY (id),

                                   CONSTRAINT fk_health_plan_item_plan
                                       FOREIGN KEY (plan_id)
                                           REFERENCES health_plans(id)
                                           ON DELETE CASCADE,

                                   CONSTRAINT chk_health_plan_item_category
                                       CHECK (
                                           category IN (
                                                        'BODY', 'MENTAL', 'DIET', 'SLEEP', 'GOAL'
                                               )
                                           ),

                                   CONSTRAINT chk_health_plan_item_quantity
                                       CHECK (target_quantity > 0),

                                   INDEX idx_health_plan_item_plan (plan_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
