-- =====================================================================
-- 增量升级脚本 v3：周报导出改 Excel（PM-035）+ 催交微信通道（PM-038）
-- 适用：已按 v2 部署过、库中数据需保留的环境；新建库直接用 pm.sql 即可
-- 执行：mysql -u root -p --default-character-set=utf8mb4 < pm_v3_weekly_xlsx_wechat.sql
-- =====================================================================
USE pm;

-- PM-038：记录微信通道推送结果，未配置 Webhook 时标 SKIPPED 而非静默
ALTER TABLE pm_remind
  ADD COLUMN wechat_status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'SENT/SKIPPED/FAILED',
  ADD COLUMN wechat_detail VARCHAR(255) DEFAULT NULL;

-- 历史记录补一个明确状态，避免界面显示为空白
UPDATE pm_remind SET wechat_status = 'SKIPPED',
       wechat_detail = '升级前历史数据，未走微信通道'
WHERE wechat_status = 'PENDING';
