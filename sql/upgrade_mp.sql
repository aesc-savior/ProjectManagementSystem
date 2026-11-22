-- =====================================================================
-- 微信小程序端 · 地基增量脚本
-- 适用：已执行 sql/pm.sql（以及 sql/upgrade_approval_fine.sql）的库
-- 可重复执行；不修改任何既有表，只新增两张小程序专用表
--
--   pm_mp_user   小程序 openid 与平台账号的绑定关系（PM-112）
--   pm_mp_token  小程序登录态（PM-112 / PM-123 退出、切换账号）
-- =====================================================================

-- ---------------------------------------------------------------------
-- openid 绑定
-- 约定：
--   1) 一个微信（openid）只能绑一个平台账号 → uk_openid
--   2) 一个平台账号只能绑一个微信 → uk_user
--   3) 换绑必须先解绑（解绑是物理删除，审计信息写 pm_log）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pm_mp_user (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT      NOT NULL COMMENT '平台账号 pm_user.id',
  openid      VARCHAR(64) NOT NULL COMMENT '微信 openid',
  unionid     VARCHAR(64) DEFAULT NULL COMMENT '微信 unionid（有则存）',
  nick_name   VARCHAR(64) DEFAULT NULL COMMENT '微信昵称（仅展示用）',
  avatar_url  VARCHAR(255) DEFAULT NULL COMMENT '微信头像',
  bind_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_openid (openid),
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序 openid 绑定';

-- ---------------------------------------------------------------------
-- 登录态
-- 不用 JWT：明文 token 落库，可随时吊销（退出登录、解绑、管理员踢人）
-- 过期判断一律用数据库 now()，避免 JVM 与 MySQL 时区不一致
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pm_mp_token (
  token        CHAR(64) NOT NULL COMMENT '登录令牌（32 字节随机数的 hex）',
  user_id      BIGINT   NOT NULL,
  openid       VARCHAR(64) NOT NULL,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  expire_time  DATETIME NOT NULL,
  last_active  DATETIME DEFAULT NULL,
  PRIMARY KEY (token),
  KEY idx_user (user_id),
  KEY idx_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序登录态';

-- 自检
SELECT 'pm_mp_user' AS item, COUNT(*) AS cnt FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pm_mp_user'
UNION ALL
SELECT 'pm_mp_token', COUNT(*) FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pm_mp_token';

-- 期望：两项 cnt 都是 1
