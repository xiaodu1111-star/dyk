-- =============================================================
-- Personal OS · V4 活动流引擎（M0 基础设施）
-- act_activity_log 统一活动流 - 全系统时间轴
-- DDL 严格对齐《personal-os-design.md》5.1 节
-- =============================================================

CREATE TABLE act_activity_log (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  dimension    VARCHAR(20) NOT NULL COMMENT 'work/learn/fit/finance/life',
  biz_type     VARCHAR(30) NOT NULL COMMENT 'task_done/study/workout/expense/checkin',
  title        VARCHAR(200) NOT NULL,
  content      VARCHAR(1000),
  duration_min INT DEFAULT 0,
  value_num    DECIMAL(12,2),
  ref_type     VARCHAR(30),
  ref_id       BIGINT,
  occurred_at  DATETIME NOT NULL COMMENT '事件发生时间（非创建时间）',
  activity_date DATE NOT NULL COMMENT '冗余，用于分组统计',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_date (user_id, activity_date),
  KEY idx_dim_date (dimension, activity_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '统一活动流 - 全系统时间轴';
