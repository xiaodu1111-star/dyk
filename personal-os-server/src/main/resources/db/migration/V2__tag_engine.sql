-- =============================================================
-- Personal OS · V2 标签引擎（M0 基础设施）
-- sys_tag 统一标签 + sys_tag_rel 多态关联
-- DDL 严格对齐《personal-os-design.md》5.1 节
-- =============================================================

CREATE TABLE sys_tag (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(50) NOT NULL,
  color       VARCHAR(20) DEFAULT '#639922',
  scope       VARCHAR(20) COMMENT '所属域，common=通用',
  use_count   INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  UNIQUE KEY uk_name_scope (name, scope)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '统一标签';

CREATE TABLE sys_tag_rel (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag_id      BIGINT NOT NULL,
  biz_type    VARCHAR(30) NOT NULL COMMENT 'task/note/workout/transaction/sop...',
  biz_id      BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tag_biz (tag_id, biz_type, biz_id),
  KEY idx_biz (biz_type, biz_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '标签多态关联';
