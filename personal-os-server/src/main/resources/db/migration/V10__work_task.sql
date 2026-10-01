-- =============================================================
-- Personal OS · V10 工作域任务表（M1 工作域）
-- work_task 单表；status 三态 + abandoned；P1 不实现 project/parent UI
-- DDL 严格对齐《docs/modules/m1-work-domain.md》§3
-- =============================================================

CREATE TABLE work_task (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  project_id    BIGINT,
  parent_id     BIGINT DEFAULT 0 COMMENT '父任务，0=顶层；P1 不实现 UI',
  title         VARCHAR(200) NOT NULL,
  description   TEXT,
  status        VARCHAR(20) DEFAULT 'todo' COMMENT 'todo/doing/done/abandoned',
  priority      TINYINT DEFAULT 2 COMMENT '1高 2中 3低',
  plan_start    DATETIME,
  due_at        DATETIME,
  estimate_min  INT COMMENT '预估工时',
  actual_min    INT DEFAULT 0 COMMENT '实际工时',
  done_at       DATETIME,
  sort_no       INT DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0,
  KEY idx_user_status (user_id, status),
  KEY idx_due (due_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '任务';
