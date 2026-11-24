-- =====================================================================
-- 项目管理系统 · 建库建表脚本（MySQL 8.0）
-- 用法：
--   mysql -uroot -p < sql/pm.sql
-- 说明：
--   1) 库名 pm，表前缀 pm_，字符集 utf8mb4；
--   2) 业务表统一含 id / create_time / creator / del_flag 公共字段；
--   3) 演示账号密码均为 123456，存储为 md5(盐 + 密码)，与 pm.util.PasswordUtil 一致。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pm DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE pm;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS pm_remind;
DROP TABLE IF EXISTS pm_log;
DROP TABLE IF EXISTS pm_fine;
DROP TABLE IF EXISTS pm_approval;
DROP TABLE IF EXISTS pm_notice_read;
DROP TABLE IF EXISTS pm_notice;
DROP TABLE IF EXISTS pm_doc;
DROP TABLE IF EXISTS pm_weekly;
DROP TABLE IF EXISTS pm_task;
DROP TABLE IF EXISTS pm_project_member;
DROP TABLE IF EXISTS pm_project;
DROP TABLE IF EXISTS pm_user;
DROP TABLE IF EXISTS pm_meeting_attendee;
DROP TABLE IF EXISTS pm_meeting;

-- ---------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------
CREATE TABLE pm_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  login_id     VARCHAR(64)  NOT NULL COMMENT '登录账号',
  user_name    VARCHAR(64)  NOT NULL COMMENT '姓名',
  password     VARCHAR(64)  NOT NULL COMMENT 'md5(盐+密码)',
  salt         VARCHAR(32)  NOT NULL,
  role_code    VARCHAR(16)  NOT NULL DEFAULT 'MEMBER' COMMENT 'ADMIN/PM/MEMBER/VIEWER',
  dept_name    VARCHAR(64)  DEFAULT NULL,
  phone        VARCHAR(32)  DEFAULT NULL,
  email        VARCHAR(128) DEFAULT NULL,
  status       VARCHAR(16)  NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/DISABLED',
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator      VARCHAR(64)  DEFAULT NULL,
  del_flag     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_login_id (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

-- ---------------------------------------------------------------------
-- 项目
-- ---------------------------------------------------------------------
CREATE TABLE pm_project (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  project_no   VARCHAR(64)   NOT NULL COMMENT '项目编号 PM-年份-4位流水',
  name         VARCHAR(255)  NOT NULL,
  project_type VARCHAR(16)   NOT NULL DEFAULT 'RND' COMMENT 'RND/BUILD/SERVICE/OTHER',
  priority     VARCHAR(8)    NOT NULL DEFAULT 'MID' COMMENT 'HIGH/MID/LOW',
  status       VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/PAUSED/DONE/CANCELED/ARCHIVED',
  start_date   DATE          DEFAULT NULL,
  end_date     DATE          DEFAULT NULL,
  owner_id     BIGINT        DEFAULT NULL COMMENT '项目经理',
  budget       DECIMAL(12,2) NOT NULL DEFAULT 0,
  dept_name    VARCHAR(64)   DEFAULT NULL,
  description  VARCHAR(1000) DEFAULT NULL,
  create_time  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator      VARCHAR(64)   DEFAULT NULL,
  del_flag     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_no (project_no),
  KEY idx_status (status),
  KEY idx_owner (owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

CREATE TABLE pm_project_member (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  project_id  BIGINT      NOT NULL,
  user_id     BIGINT      NOT NULL,
  role_code   VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT 'MANAGER/MEMBER/VIEWER',
  join_date   DATE        DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_user (project_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员';

-- ---------------------------------------------------------------------
-- 任务（parent_id=0 为顶层）
-- ---------------------------------------------------------------------
CREATE TABLE pm_task (
  id             BIGINT        NOT NULL AUTO_INCREMENT,
  project_id     BIGINT        NOT NULL,
  parent_id      BIGINT        NOT NULL DEFAULT 0,
  title          VARCHAR(255)  NOT NULL,
  description    VARCHAR(1000) DEFAULT NULL,
  assignee_id    BIGINT        DEFAULT NULL,
  priority       VARCHAR(8)    NOT NULL DEFAULT 'MID',
  status         VARCHAR(16)   NOT NULL DEFAULT 'TODO' COMMENT 'TODO/DOING/PAUSED/DONE/CANCELED',
  progress       INT           NOT NULL DEFAULT 0,
  start_date     DATE          DEFAULT NULL,
  due_date       DATE          DEFAULT NULL,
  estimate_hours DECIMAL(8,1)  NOT NULL DEFAULT 0,
  actual_hours   DECIMAL(8,1)  NOT NULL DEFAULT 0,
  create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  creator        VARCHAR(64)   DEFAULT NULL,
  del_flag       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id),
  KEY idx_parent (parent_id),
  KEY idx_assignee (assignee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务';

-- ---------------------------------------------------------------------
-- 周报
-- ---------------------------------------------------------------------
CREATE TABLE pm_weekly (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  project_id  BIGINT       NOT NULL,
  user_id     BIGINT       NOT NULL,
  week_no     VARCHAR(16)  NOT NULL COMMENT '如 2026-W46',
  done_text   VARCHAR(2000) DEFAULT NULL,
  plan_text   VARCHAR(2000) DEFAULT NULL,
  issue_text  VARCHAR(1000) DEFAULT NULL,
  hours       DECIMAL(6,1) NOT NULL DEFAULT 0,
  status      VARCHAR(16)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SUBMITTED/REVIEWED/REJECTED',
  review      VARCHAR(500) DEFAULT NULL COMMENT '审核评语（退回时必填理由）',
  score       INT          DEFAULT NULL,
  reviewer    VARCHAR(64)  DEFAULT NULL COMMENT '审核人',
  review_time DATETIME     DEFAULT NULL,
  attach_name VARCHAR(255) DEFAULT NULL COMMENT '附件原始文件名',
  attach_path VARCHAR(500) DEFAULT NULL COMMENT '附件存储相对路径',
  attach_size BIGINT       NOT NULL DEFAULT 0,
  submit_time DATETIME     DEFAULT NULL,
  withdraw_time DATETIME   DEFAULT NULL COMMENT '最近一次撤回时间',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_week (project_id, user_id, week_no),
  KEY idx_week (week_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目周报';

-- ---------------------------------------------------------------------
-- 文档
-- ---------------------------------------------------------------------
CREATE TABLE pm_doc (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  project_id  BIGINT       NOT NULL,
  task_id     BIGINT       NOT NULL DEFAULT 0,
  name        VARCHAR(255) NOT NULL,
  ext         VARCHAR(16)  DEFAULT NULL,
  size        BIGINT       NOT NULL DEFAULT 0,
  path        VARCHAR(500) NOT NULL,
  version     INT          NOT NULL DEFAULT 1,
  visibility  VARCHAR(16)  NOT NULL DEFAULT 'PROJECT' COMMENT 'PUBLIC/PROJECT/PRIVATE',
  uploader_id BIGINT       DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag    TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目文档';

-- ---------------------------------------------------------------------
-- 公告与签收
-- ---------------------------------------------------------------------
CREATE TABLE pm_notice (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  title        VARCHAR(255) NOT NULL,
  content      VARCHAR(4000) DEFAULT NULL,
  scope        VARCHAR(16)  NOT NULL DEFAULT 'ALL' COMMENT 'ALL/PROJECT/USERS',
  project_id   BIGINT       NOT NULL DEFAULT 0,
  expire_time  DATETIME     DEFAULT NULL,
  publisher_id BIGINT       DEFAULT NULL,
  publisher    VARCHAR(64)  DEFAULT NULL,
  create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  del_flag     TINYINT      NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告';

CREATE TABLE pm_notice_read (
  id         BIGINT   NOT NULL AUTO_INCREMENT,
  notice_id  BIGINT   NOT NULL,
  user_id    BIGINT   NOT NULL,
  read_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  device     VARCHAR(32) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notice_user (notice_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告签收';

-- ---------------------------------------------------------------------
-- 审批（延期/报销等，本期先做任务延期）
-- ---------------------------------------------------------------------
CREATE TABLE pm_approval (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  biz_type      VARCHAR(32)  NOT NULL COMMENT 'TASK_DELAY/PROJECT_DELAY/EXPENSE',
  biz_id        BIGINT       NOT NULL,
  title         VARCHAR(255) NOT NULL,
  applicant_id  BIGINT       NOT NULL,
  applicant     VARCHAR(64)  DEFAULT NULL,
  approver_id   BIGINT       DEFAULT NULL,
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  detail        VARCHAR(1000) DEFAULT NULL COMMENT '延期原因/新日期等',
  new_date      DATE         DEFAULT NULL COMMENT '延期后新截止日',
  opinion       VARCHAR(500) DEFAULT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  approve_time  DATETIME     DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批';

-- ---------------------------------------------------------------------
-- 罚款单
-- ---------------------------------------------------------------------
CREATE TABLE pm_fine (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  project_id  BIGINT        NOT NULL,
  user_id     BIGINT        NOT NULL COMMENT '被罚人（项目经理）',
  user_name   VARCHAR(64)   DEFAULT NULL,
  amount      DECIMAL(10,2) NOT NULL DEFAULT 0,
  reason      VARCHAR(500)  DEFAULT NULL,
  status      VARCHAR(16)   NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PAID',
  trade_no    VARCHAR(64)   DEFAULT NULL,
  pay_time    DATETIME      DEFAULT NULL,
  create_time DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='罚款单';

-- ---------------------------------------------------------------------
-- 操作日志
-- ---------------------------------------------------------------------
CREATE TABLE pm_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     VARCHAR(64)  DEFAULT NULL,
  user_name   VARCHAR(64)  DEFAULT NULL,
  action      VARCHAR(16)  NOT NULL COMMENT 'LOGIN/ADD/UPDATE/DELETE/ARCHIVE/SUBMIT/APPROVE/UPLOAD/DOWNLOAD/PAY',
  target      VARCHAR(128) DEFAULT NULL,
  detail      VARCHAR(500) DEFAULT NULL,
  ip          VARCHAR(64)  DEFAULT NULL,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_time (create_time),
  KEY idx_action (action)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- ---------------------------------------------------------------------
-- 站内提醒（周报催交）
-- ---------------------------------------------------------------------
CREATE TABLE pm_remind (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  user_id     BIGINT       NOT NULL,
  user_name   VARCHAR(64)  DEFAULT NULL,
  type        VARCHAR(16)  NOT NULL COMMENT 'WEEKLY/MEETING',
  title       VARCHAR(128) NOT NULL,
  content     VARCHAR(500) DEFAULT NULL,
  target_url  VARCHAR(255) DEFAULT NULL,
  read_flag   TINYINT      NOT NULL DEFAULT 0,
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  wechat_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'SENT/SKIPPED/FAILED',
  wechat_detail VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_remind_user (user_id, read_flag),
  KEY idx_remind_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内提醒';


-- 会议
CREATE TABLE pm_meeting (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    topic         VARCHAR(255) NOT NULL COMMENT '会议主题',
    project_id    BIGINT       NOT NULL DEFAULT 0 COMMENT '关联项目，0=不关联',
    start_time    DATETIME     NOT NULL COMMENT '开始时间',
    end_time      DATETIME     NOT NULL COMMENT '结束时间',
    location      VARCHAR(255) DEFAULT NULL COMMENT '地点',
    agenda        TEXT         COMMENT '议程',
    organizer_id  BIGINT       DEFAULT NULL COMMENT '组织人ID',
    organizer     VARCHAR(64)  DEFAULT NULL COMMENT '组织人姓名快照',
    status        VARCHAR(16)  NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED/CANCELED',
    cancel_reason VARCHAR(500) DEFAULT NULL COMMENT '取消理由',
    cancel_time   DATETIME     DEFAULT NULL COMMENT '取消时间',
    attach_name   VARCHAR(255) DEFAULT NULL COMMENT '附件原名',
    attach_path   VARCHAR(500) DEFAULT NULL COMMENT '相对上传目录的路径',
    attach_size   BIGINT       NOT NULL DEFAULT 0 COMMENT '附件字节数',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     DEFAULT NULL COMMENT '最后修改时间',
    creator       VARCHAR(64)  DEFAULT NULL COMMENT '创建人姓名快照',
    del_flag      TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常/1删除',
    PRIMARY KEY (id),
    KEY idx_meeting_time (start_time),
    KEY idx_meeting_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议';

-- 会议参会人与回执
CREATE TABLE pm_meeting_attendee (
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    meeting_id   BIGINT      NOT NULL COMMENT '会议ID',
    user_id      BIGINT      NOT NULL COMMENT '参会人ID',
    user_name    VARCHAR(64) DEFAULT NULL COMMENT '姓名快照',
    reply_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/ACCEPTED/DECLINED',
    reply_reason VARCHAR(500) DEFAULT NULL COMMENT '请假理由',
    reply_time   DATETIME    DEFAULT NULL COMMENT '回执时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_meeting_user (meeting_id, user_id),
    KEY idx_attendee_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会议参会人与回执';

-- =====================================================================
-- 演示数据（密码均为 123456）
-- =====================================================================
-- =====================================================================
-- 演示数据（密码均为 123456）
-- =====================================================================
INSERT INTO pm_user (login_id, user_name, password, salt, role_code, dept_name, phone, email, status, creator) VALUES
('admin',  '系统管理员', '59d313268530c2f4b8e2bc1c98cfe74e', '9f2c7a1b4e6d8035', 'ADMIN',  '信息中心', '13800000001', 'admin@example.com',  'NORMAL', 'system'),
('pm',     '张项目经理', 'e87dc75aa7987df6b08b2b53381cb584', '3b7e1d9c5a2f4068', 'PM',     '项目管理部', '13800000002', 'pm@example.com',     'NORMAL', 'system'),
('member', '李四',       '599c96f0abd6e9d8c1737cb086decf5d', '6c1f8e3a9b5d2074', 'MEMBER', '研发部',   '13800000003', 'member@example.com', 'NORMAL', 'system'),
('viewer', '王五',       '6a08c8d70e09d5cc6300960b60b7ebae', 'a1b2c3d4e5f60718', 'VIEWER', '客户成功部', '13800000004', 'viewer@example.com', 'NORMAL', 'system');

INSERT INTO pm_project (project_no, name, project_type, priority, status, start_date, end_date, owner_id, budget, dept_name, description, creator) VALUES
('PM-2026-0001', '官网改版项目',     'RND',     'HIGH', 'RUNNING',  '2026-09-01', '2026-12-31', 2, 200000.00, '研发部',   '官网视觉与信息架构重做，含 PC 与移动端', '张项目经理'),
('PM-2026-0002', '数据中心机房建设', 'BUILD',   'MID',  'RUNNING',  '2026-07-01', '2026-11-30', 2, 800000.00, '基建部',   '二期机房扩容与配电改造', '张项目经理'),
('PM-2026-0003', '客户成功服务包',   'SERVICE', 'LOW',  'DONE',     '2026-03-01', '2026-08-31', 1, 120000.00, '客户部',   '重点客户年度服务与巡检', '系统管理员'),
('PM-2026-0004', '移动端重构',       'RND',     'HIGH', 'PAUSED',   '2026-10-01', '2027-02-28', 2, 300000.00, '研发部',   'App 与小程序统一技术栈，等待预算审批', '张项目经理'),
('PM-2026-0005', '年度审计支持',     'OTHER',   'MID',  'ARCHIVED', '2026-01-01', '2026-06-30', 1, 50000.00,  '财务部',   '配合外部审计提供资料与访谈', '系统管理员');

INSERT INTO pm_project_member (project_id, user_id, role_code, join_date) VALUES
(1, 2, 'MANAGER', '2026-09-01'),
(1, 3, 'MEMBER',  '2026-09-01'),
(1, 4, 'VIEWER',  '2026-09-10'),
(2, 2, 'MANAGER', '2026-07-01'),
(2, 3, 'MEMBER',  '2026-07-05'),
(3, 1, 'MANAGER', '2026-03-01'),
(4, 2, 'MANAGER', '2026-10-01'),
(5, 1, 'MANAGER', '2026-01-01');

INSERT INTO pm_task (project_id, parent_id, title, description, assignee_id, priority, status, progress, start_date, due_date, estimate_hours, actual_hours, creator) VALUES
(1, 0, '需求调研',     '访谈业务方，输出需求清单', 3, 'HIGH', 'DONE',   100, '2026-09-01', '2026-09-15', 40, 42, '张项目经理'),
(1, 0, '原型设计',     '低保真 + 高保真原型',     3, 'HIGH', 'DONE',   100, '2026-09-16', '2026-10-07', 60, 58, '张项目经理'),
(1, 0, '前端开发',     '首页、产品页、案例页',     3, 'HIGH', 'DOING',   60, '2026-10-08', '2026-11-30', 120, 80, '张项目经理'),
(1, 0, '后端接口',     'CMS 接口与表单服务',      2, 'MID',  'DOING',   40, '2026-10-15', '2026-12-05', 100, 45, '张项目经理'),
(1, 3, '页面开发',     '首页与产品页',             3, 'HIGH', 'DOING',   70, '2026-10-08', '2026-11-20', 60, 45, '张项目经理'),
(1, 3, '组件库',       '按钮、表单、卡片组件',     3, 'MID',  'DOING',   50, '2026-10-08', '2026-11-15', 40, 20, '张项目经理'),
(1, 0, '联调测试',     '接口联调与回归',           2, 'MID',  'TODO',     0, '2026-12-06', '2026-12-20', 50, 0, '张项目经理'),
(2, 0, '土建施工',     '机房地面与隔断',           3, 'HIGH', 'DOING',   50, '2026-07-01', '2026-10-31', 300, 160, '张项目经理'),
(2, 0, '设备采购',     'UPS、机柜与空调',          2, 'MID',  'TODO',     0, '2026-11-01', '2026-11-25', 40, 0, '张项目经理'),
(1, 0, '需求调研0',     '访谈业务方，输出需求清单0',   2, 'HIGH', 'TODO',     0, '2026-12-21', '2026-12-31', 30, 0, '张项目经理');

INSERT INTO pm_weekly (project_id, user_id, week_no, done_text, plan_text, issue_text, hours, status, submit_time) VALUES
(1, 3, '2026-W45', '完成首页视觉稿与 3 个产品页切图；组件库完成按钮与表单。', '完成案例页与组件库剩余组件。', '设计稿变更两次，进度略延后。', 38.0, 'SUBMITTED', '2026-11-06 17:30:00'),
(1, 3, '2026-W46', '完成案例页；组件库完成 80%。', '联调首页接口。', NULL, 40.0, 'DRAFT', NULL),
(1, 2, '2026-W46', '后端接口完成项目与任务模块。', '完成周报与文档接口。', '测试环境数据库不稳定。', 36.0, 'SUBMITTED', '2026-11-13 18:05:00'),
(2, 3, '2026-W45', '土建完成 50%，配电进场。', '完成机柜到货验收。', '物业施工时间受限。', 42.0, 'SUBMITTED', '2026-11-06 16:40:00');

INSERT INTO pm_notice (title, content, scope, project_id, expire_time, publisher_id, publisher) VALUES
('关于 11 月项目例会的通知', '本周五 14:00 在三楼会议室召开项目例会，请各项目经理准备进度与风险清单。', 'ALL', 0, '2026-12-31 23:59:59', 1, '系统管理员'),
('官网改版项目进入联调阶段', '官网改版项目已完成页面开发 80%，下周起进入接口联调，请相关同学预留时间。', 'PROJECT', 1, '2026-12-31 23:59:59', 2, '张项目经理');

INSERT INTO pm_notice_read (notice_id, user_id, read_time, device) VALUES
(1, 3, '2026-11-14 09:12:00', 'PC');

INSERT INTO pm_approval (biz_type, biz_id, title, applicant_id, applicant, approver_id, status, detail, new_date, create_time) VALUES
('TASK_DELAY', 3, '任务延期申请：前端开发', 3, '李四', 2, 'PENDING', '设计稿两次变更，需重做案例页。', '2026-12-10', '2026-11-13 10:20:00'),
('TASK_DELAY', 7, '任务延期申请：联调测试', 2, '张项目经理', 2, 'APPROVED', '后端接口延期。', '2026-12-24', '2026-11-05 09:00:00');

INSERT INTO pm_fine (project_id, user_id, user_name, amount, reason, status) VALUES
(5, 1, '系统管理员', 13800.00, '项目「年度审计支持」超期 138 天，按 100 元/天计。', 'UNPAID');

INSERT INTO pm_log (user_id, user_name, action, target, detail, ip) VALUES
('1', '系统管理员', 'LOGIN', 'user:1', '演示数据初始化', '127.0.0.1'),
('2', '张项目经理', 'ADD', 'project:1', '创建项目：官网改版项目', '127.0.0.1'),
('3', '李四', 'SUBMIT', 'weekly:1', '提交 2026-W45 周报', '127.0.0.1');

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO pm_meeting(topic, project_id, start_time, end_time, location, agenda,
                       organizer_id, organizer, status, create_time)
SELECT '官网改版周例会', p.id, '2026-11-27 14:00:00', '2026-11-27 15:00:00',
       '三楼会议室', '1. 本周进度\n2. 联调风险',
       u.id, u.user_name, 'PUBLISHED', '2026-11-20 10:00:00'
FROM pm_project p, pm_user u
WHERE p.project_no = 'PM-2026-0001' AND u.login_id = 'pm';

SET @meetingId = (SELECT id FROM pm_meeting
                  WHERE topic = '官网改版周例会' ORDER BY id DESC LIMIT 1);

INSERT INTO pm_meeting_attendee(meeting_id, user_id, user_name, reply_status)
SELECT @meetingId, u.id, u.user_name,
       CASE WHEN u.login_id = 'pm' THEN 'ACCEPTED' ELSE 'PENDING' END
FROM pm_user u
WHERE u.login_id IN ('pm', 'member', 'viewer');