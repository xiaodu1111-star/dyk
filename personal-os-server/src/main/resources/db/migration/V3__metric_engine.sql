-- =============================================================
-- Personal OS · V3 指标引擎（M0 基础设施）
-- sys_metric_def 指标定义 + sys_metric_record 指标记录
-- DDL 严格对齐《personal-os-design.md》5.1 节
-- =============================================================

CREATE TABLE sys_metric_def (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  code          VARCHAR(50) NOT NULL UNIQUE COMMENT 'weight / water_cup / mood',
  name          VARCHAR(50) NOT NULL,
  dimension     VARCHAR(20) NOT NULL COMMENT 'work/learn/fit/finance/life',
  value_type    VARCHAR(20) NOT NULL DEFAULT 'number' COMMENT 'number/duration/enum/bool',
  unit          VARCHAR(20) COMMENT 'kg / 分钟 / 杯',
  options_json  VARCHAR(500) COMMENT 'enum 类型的可选值',
  agg_type      VARCHAR(10) DEFAULT 'last' COMMENT 'sum/avg/max/min/last',
  target_value  DECIMAL(12,2) COMMENT '目标值，用于进度条',
  show_on_home  TINYINT DEFAULT 1,
  sort_no       INT DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '指标定义 - 新增追踪项只需插一条';

CREATE TABLE sys_metric_record (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  metric_id   BIGINT NOT NULL,
  record_date DATE   NOT NULL,
  record_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  value_num   DECIMAL(12,2),
  value_text  VARCHAR(200),
  ref_type    VARCHAR(30) COMMENT '可选：来源业务实体',
  ref_id      BIGINT,
  remark      VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  KEY idx_metric_date (metric_id, record_date),
  KEY idx_date (record_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '指标记录';
