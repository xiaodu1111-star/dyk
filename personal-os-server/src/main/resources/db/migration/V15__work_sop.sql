-- =============================================================
-- Personal OS · V15 SOP 库（M2 核心差异化模块）
-- work_sop + work_sop_step
-- DDL 严格对齐《docs/modules/m2-sop-library.md》§3
-- =============================================================

CREATE TABLE work_sop (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  title          VARCHAR(200) NOT NULL,
  category       VARCHAR(50) COMMENT '分类，如 需求评审/故障处理/周报',
  trigger_scene  VARCHAR(500) COMMENT '什么场景下用这个 SOP',
  goal           VARCHAR(500) COMMENT '产出什么结果',
  version        VARCHAR(20) DEFAULT 'v1.0',
  use_count      INT DEFAULT 0 COMMENT '被复用次数',
  avg_minutes    INT DEFAULT 0 COMMENT '平均耗时，自动统计',
  last_used_at   DATETIME,
  status         VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/active/deprecated',
  source_task_id BIGINT COMMENT '由哪个任务沉淀而来',
  pinned         TINYINT DEFAULT 0 COMMENT '置顶',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT DEFAULT 0
) COMMENT 'SOP / 工作技能库';

CREATE TABLE work_sop_step (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id       BIGINT NOT NULL,
  step_no      INT NOT NULL,
  title        VARCHAR(200) NOT NULL,
  detail       TEXT COMMENT '操作说明/Markdown',
  tip          VARCHAR(500) COMMENT '避坑提示',
  estimate_min INT,
  KEY idx_sop (sop_id, step_no)
) COMMENT 'SOP 步骤';
