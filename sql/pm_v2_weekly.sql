-- =====================================================================
-- 项目管理系统 · 增量升级脚本（V1 → V2）
-- 用途：在已有数据的 pm 库上启用「三、项目周报管理」增强，不实现资产与合同
-- 用法：mysql -uroot -p --default-character-set=utf8mb4 < sql/pm_v2_weekly.sql
-- 说明：新建库直接用 pm.sql；ALTER 提示 Duplicate column name 说明列已存在
-- =====================================================================

USE pm;
SET NAMES utf8mb4;

-- 一、周报表补齐：附件、审核人、审核时间、撤回时间
-- ---------------------------------------------------------------------
ALTER TABLE pm_weekly ADD COLUMN reviewer      VARCHAR(64)  DEFAULT NULL COMMENT '审核人';
ALTER TABLE pm_weekly ADD COLUMN review_time   DATETIME     DEFAULT NULL;
ALTER TABLE pm_weekly ADD COLUMN attach_name   VARCHAR(255) DEFAULT NULL COMMENT '附件原始文件名';
ALTER TABLE pm_weekly ADD COLUMN attach_path   VARCHAR(500) DEFAULT NULL COMMENT '附件存储相对路径';
ALTER TABLE pm_weekly ADD COLUMN attach_size   BIGINT       NOT NULL DEFAULT 0;
ALTER TABLE pm_weekly ADD COLUMN withdraw_time DATETIME     DEFAULT NULL COMMENT '最近一次撤回时间';
ALTER TABLE pm_weekly MODIFY COLUMN review VARCHAR(500) DEFAULT NULL COMMENT '审核评语（退回时必填理由）';

-- ---------------------------------------------------------------------
-- 二、站内提醒（周报催交）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pm_remind (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  user_name   VARCHAR(64)  DEFAULT NULL,
  type        VARCHAR(16)  NOT NULL COMMENT 'WEEKLY',
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(500) DEFAULT NULL,
  target_url  VARCHAR(255) DEFAULT NULL,
  read_flag   TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_remind_user (user_id, read_flag),
  KEY idx_remind_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内提醒';
