-- =============================================================
-- Personal OS · V1 登录闭环
-- 仅创建 sys_user 一张表，字段严格对齐《personal-os-design.md》5.1 节
-- 种子数据由 DataInitializer 在应用启动时按需写入，不入库脚本
-- =============================================================

CREATE TABLE sys_user (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  nickname      VARCHAR(50),
  avatar        VARCHAR(255),
  city          VARCHAR(50),
  daily_target_minutes INT DEFAULT 480 COMMENT '每日目标投入分钟',
  last_login_at DATETIME,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
  COMMENT = '用户';
