-- =============================================================
-- Personal OS · V16 SOP 版本历史 + 使用记录（M2 核心差异化模块）
-- work_sop_version + work_sop_log
-- DDL 严格对齐《docs/modules/m2-sop-library.md》§3
-- =============================================================

CREATE TABLE work_sop_version (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id      BIGINT NOT NULL,
  version     VARCHAR(20) NOT NULL,
  content_json JSON COMMENT '该版本的完整快照（含步骤）',
  change_note VARCHAR(500) COMMENT '本次优化了什么',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sop (sop_id)
) COMMENT 'SOP 版本历史';

CREATE TABLE work_sop_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id      BIGINT NOT NULL,
  started_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME,
  cost_min    INT,
  stuck_step  INT COMMENT '卡在第几步',
  deviation   VARCHAR(500) COMMENT '执行中发现的问题/需要改进的点',
  KEY idx_sop (sop_id)
) COMMENT 'SOP 使用记录';
