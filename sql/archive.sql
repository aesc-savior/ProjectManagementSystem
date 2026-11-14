-- =====================================================================
-- 档案管理系统 · 建库脚本（MySQL 8.0）
-- 用法：
--   mysql -uroot -p -e "create database archive default charset utf8mb4 collate utf8mb4_general_ci;"
--   mysql -uroot -p archive < sql/archive.sql
-- 说明：
--   1) 表前缀统一 da_，字符集 utf8mb4；
--   2) 所有业务表含 id / create_time / creator / del_flag 公共字段（del_flag=1 为逻辑删除）；
--   3) 演示账号密码均为 123456，存储为 md5(盐 + 密码)，与 PasswordUtil 一致。
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS da_log;
DROP TABLE IF EXISTS da_borrow;
DROP TABLE IF EXISTS da_archive_file;
DROP TABLE IF EXISTS da_archive;
DROP TABLE IF EXISTS da_category;
DROP TABLE IF EXISTS da_user;

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
CREATE TABLE da_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  login_id     VARCHAR(64)  NOT NULL COMMENT '登录账号',
  user_name    VARCHAR(64)  NOT NULL COMMENT '姓名',
  password     VARCHAR(64)  NOT NULL COMMENT 'md5(盐+密码)',
  salt         VARCHAR(32)  NOT NULL COMMENT '密码盐',
  role_code    VARCHAR(16)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN/ARCHIVIST/USER',
  dept_name    VARCHAR(64)  DEFAULT NULL COMMENT '部门',
  phone        VARCHAR(32)  DEFAULT NULL,
  email        VARCHAR(128) DEFAULT NULL,
  secret_level TINYINT      NOT NULL DEFAULT 1 COMMENT '可访问密级 1~4',
  status       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator      VARCHAR(64)  DEFAULT NULL,
  del_flag     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------------------------------------------------
-- 分类（两级：parent_id=0 为一级）
-- ---------------------------------------------------------------------
CREATE TABLE da_category (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT      NOT NULL DEFAULT 0 COMMENT '0 为一级分类',
  name        VARCHAR(64) NOT NULL,
  level       TINYINT     NOT NULL DEFAULT 1,
  sort        INT         NOT NULL DEFAULT 0,
  create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag    TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档案分类';

-- ---------------------------------------------------------------------
-- 档案主表
-- ---------------------------------------------------------------------
CREATE TABLE da_archive (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  archive_no   VARCHAR(64)  NOT NULL COMMENT '档号 DA-年份-0001',
  title        VARCHAR(255) NOT NULL COMMENT '档案名称',
  category_id  BIGINT       NOT NULL DEFAULT 0,
  secret_level TINYINT      NOT NULL DEFAULT 1 COMMENT '1公开 2内部 3秘密 4机密',
  keep_years   VARCHAR(16)  NOT NULL DEFAULT 'LONG' COMMENT 'SHORT/LONG/FOREVER',
  dept_name    VARCHAR(64)  DEFAULT NULL COMMENT '责任部门',
  owner_name   VARCHAR(64)  DEFAULT NULL COMMENT '责任者',
  status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ARCHIVED/BORROWED/DESTROYED',
  archive_time DATETIME     DEFAULT NULL,
  remark       VARCHAR(500) DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator      VARCHAR(64)  DEFAULT NULL,
  del_flag     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_archive_no (archive_no),
  KEY idx_cat_status (category_id, status),
  KEY idx_time (archive_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档案主表';

-- ---------------------------------------------------------------------
-- 附件
-- ---------------------------------------------------------------------
CREATE TABLE da_archive_file (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  archive_id  BIGINT       NOT NULL,
  file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
  store_name  VARCHAR(128) NOT NULL COMMENT '落盘文件名（UUID）',
  file_path   VARCHAR(500) NOT NULL COMMENT '落盘绝对路径',
  file_size   BIGINT       NOT NULL DEFAULT 0,
  file_type   VARCHAR(32)  DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_archive (archive_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='档案附件';

-- ---------------------------------------------------------------------
-- 借阅
-- ---------------------------------------------------------------------
CREATE TABLE da_borrow (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  archive_id   BIGINT      NOT NULL,
  user_id      VARCHAR(64) NOT NULL COMMENT '申请人 id',
  user_name    VARCHAR(64) DEFAULT NULL,
  reason       VARCHAR(500) DEFAULT NULL,
  start_date   DATE        NOT NULL,
  end_date     DATE        NOT NULL,
  status       VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/RETURNED',
  approver     VARCHAR(64) DEFAULT NULL,
  approve_time DATETIME    DEFAULT NULL,
  return_time  DATETIME    DEFAULT NULL,
  create_time  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_archive (archive_id),
  KEY idx_user (user_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='借阅记录';

-- ---------------------------------------------------------------------
-- 操作日志
-- ---------------------------------------------------------------------
CREATE TABLE da_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     VARCHAR(64)  DEFAULT NULL,
  user_name   VARCHAR(64)  DEFAULT NULL,
  action      VARCHAR(16)  NOT NULL COMMENT 'LOGIN/ADD/UPDATE/DELETE/ARCHIVE/APPLY/APPROVE/UPLOAD/DOWNLOAD/VIEW',
  target      VARCHAR(128) DEFAULT NULL,
  detail      VARCHAR(500) DEFAULT NULL,
  ip          VARCHAR(64)  DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_time (create_time),
  KEY idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- =====================================================================
-- 演示数据
-- =====================================================================

-- 密码均为 123456
INSERT INTO da_user (login_id, user_name, password, salt, role_code, dept_name, phone, email, secret_level, status, creator) VALUES
('admin',     '张管理员',   '59d313268530c2f4b8e2bc1c98cfe74e', '9f2c7a1b4e6d8035', 'ADMIN',     '信息中心', '13800000001', 'admin@example.com', 4, 'NORMAL', 'system'),
('archivist', '张档案员',   'e87dc75aa7987df6b08b2b53381cb584', '3b7e1d9c5a2f4068', 'ARCHIVIST', '档案室',   '13800000002', 'arch@example.com',  3, 'NORMAL', 'system'),
('user',      '李四',       '599c96f0abd6e9d8c1737cb086decf5d', '6c1f8e3a9b5d2074', 'USER',      '财务处',   '13800000003', 'user@example.com',  2, 'NORMAL', 'system'),
('wangwu',    '王五',       '599c96f0abd6e9d8c1737cb086decf5d', '6c1f8e3a9b5d2074', 'USER',      '人事处',   '13800000004', 'wang@example.com',  1, 'NORMAL', 'system');

INSERT INTO da_category (parent_id, name, level, sort) VALUES
(0, '财务档案', 1, 1),
(0, '文书档案', 1, 2),
(0, '人事档案', 1, 3),
(0, '基建档案', 1, 4),
(1, '2023 年度决算', 2, 1),
(1, '审计底稿', 2, 2),
(2, '年度总结', 2, 3);

INSERT INTO da_archive (archive_no, title, category_id, secret_level, keep_years, dept_name, owner_name, status, archive_time, remark, creator) VALUES
('DA-2026-0001', '2023 年度财务决算报告', 5, 2, 'LONG',    '财务处', '财务处', 'ARCHIVED', '2026-03-12 10:20:00', '含附件与审计意见', '张档案员'),
('DA-2026-0002', '2023 年第四季度审计底稿', 6, 3, 'LONG',  '审计处', '审计处', 'ARCHIVED', '2026-03-15 09:05:00', '共 4 册', '张档案员'),
('DA-2026-0003', '人事任免文件（2023-12）', 3, 4, 'FOREVER', '人事处', '人事处', 'ARCHIVED', '2026-04-02 15:40:00', '机密，限人事处查阅', '张档案员'),
('DA-2026-0004', '年度工作总结汇编', 7, 1, 'SHORT',       '办公室', '办公室', 'ARCHIVED', '2026-04-08 11:00:00', NULL, '张档案员'),
('DA-2026-0005', '基建工程验收资料', 4, 2, 'LONG',        '基建处', '基建处', 'PENDING',  NULL, '待补齐监理报告', '张档案员'),
('DA-2026-0006', '设备采购合同（第 3 批）', 2, 2, 'LONG',  '采购中心', '采购中心', 'ARCHIVED', '2026-05-20 16:30:00', NULL, '张档案员');

INSERT INTO da_borrow (archive_id, user_id, user_name, reason, start_date, end_date, status, approver, approve_time) VALUES
(1, '3', '李四', '年度决算审计需要核对原始凭证', '2026-05-01', '2026-05-10', 'RETURNED', '张档案员', '2026-05-01 09:30:00'),
(2, '3', '李四', '复核审计底稿', '2026-05-12', '2026-05-20', 'APPROVED', '张档案员', '2026-05-12 10:00:00');

INSERT INTO da_log (user_id, user_name, action, target, detail, ip) VALUES
('1', '系统管理员', 'LOGIN', 'user:1', '演示数据初始化', '127.0.0.1');

SET FOREIGN_KEY_CHECKS = 1;
