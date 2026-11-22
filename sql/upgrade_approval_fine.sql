-- =====================================================================
-- 审批与罚款模块 · 增量升级脚本
-- 适用：已经跑过 sql/pm.sql 的库（pm），可重复执行
-- 用法：mysql -uroot -p pm < upgrade_approval_fine.sql
--       （Navicat 里请整体执行；脚本使用了 DELIMITER，需支持该语法）
--
-- 本次新增：
--   pm_approval.old_date  申请时的原截止日快照（审批人对比新旧日期、审计用）
--   pm_fine.due_date      被罚那一期的项目截止日快照（同一期只能开一张单）
--   uk_project_term       (project_id, due_date) 唯一键，从数据库层堵住重复开单
--   若干查询索引
-- =====================================================================

-- ---------- 1. 加列（幂等：已存在则跳过） ----------
DROP PROCEDURE IF EXISTS pm_up_add_column;

DELIMITER $$
CREATE PROCEDURE pm_up_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_table
                     AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE st FROM @ddl;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

CALL pm_up_add_column('pm_approval', 'old_date',
     '`old_date` DATE DEFAULT NULL COMMENT ''申请时的原截止日'' AFTER `detail`');
CALL pm_up_add_column('pm_fine', 'due_date',
     '`due_date` DATE DEFAULT NULL COMMENT ''罚款对应的那一期项目截止日'' AFTER `status`');

DROP PROCEDURE IF EXISTS pm_up_add_column;

-- ---------- 2. 历史数据回填 ----------
-- 审批：老数据无法反推原截止日，保持 NULL（页面显示 —），不影响新流程
-- 罚款：按项目当前截止日回填，但每个项目只补最早的一条，避免回填后撞唯一键
UPDATE pm_fine f
    JOIN pm_project p ON p.id = f.project_id
SET f.due_date = p.end_date
WHERE f.due_date IS NULL
  AND f.id = (SELECT MIN(x.id) FROM (SELECT id, project_id FROM pm_fine WHERE due_date IS NULL) x
              WHERE x.project_id = f.project_id);

-- ---------- 3. 索引（幂等：已存在则跳过） ----------
DROP PROCEDURE IF EXISTS pm_up_add_index;

DELIMITER $$
CREATE PROCEDURE pm_up_add_index(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = p_table
                     AND INDEX_NAME = p_index) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ', p_ddl);
        PREPARE st FROM @ddl;
        EXECUTE st;
        DEALLOCATE PREPARE st;
    END IF;
END$$
DELIMITER ;

-- 同一项目、同一期（同一截止日）只允许一张罚款单：缴完也不能对同一期重复开单
CALL pm_up_add_index('pm_fine', 'uk_project_term',
     'UNIQUE KEY `uk_project_term` (`project_id`, `due_date`)');
-- 台账/未缴合计按人+状态过滤
CALL pm_up_add_index('pm_fine', 'idx_user_status',
     'KEY `idx_user_status` (`user_id`, `status`)');
-- 重复申请检查：biz_type + biz_id + status
CALL pm_up_add_index('pm_approval', 'idx_biz',
     'KEY `idx_biz` (`biz_type`, `biz_id`, `status`)');
-- "我发起的"按申请人查
CALL pm_up_add_index('pm_approval', 'idx_applicant',
     'KEY `idx_applicant` (`applicant_id`)');

DROP PROCEDURE IF EXISTS pm_up_add_index;

-- ---------- 4. 自检 ----------
SELECT 'pm_approval.old_date' AS item, COUNT(*) AS cnt
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pm_approval' AND COLUMN_NAME = 'old_date'
UNION ALL
SELECT 'pm_fine.due_date', COUNT(*)
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pm_fine' AND COLUMN_NAME = 'due_date'
UNION ALL
SELECT 'uk_project_term', COUNT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pm_fine' AND INDEX_NAME = 'uk_project_term';

-- 期望结果：三项 cnt 都为 1
