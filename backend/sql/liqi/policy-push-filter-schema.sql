-- 力企 · 园区发布项目「客户对象」企业画像圈选
-- 背景：园区发布政策时，需要按企业画像标签（参保人数/是否融资/成立日期/所属行业/实缴资本）
--       圈选推送客户；圈选范围 = 园区客户管理（liqi_member）中的企业。
-- 幂等：可重复执行

DROP PROCEDURE IF EXISTS `liqi_add_column`;
DELIMITER $$
CREATE PROCEDURE `liqi_add_column`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(512))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
    SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_ddl);
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `liqi_add_index`;
DELIMITER $$
CREATE PROCEDURE `liqi_add_index`(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_cols VARCHAR(512))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index) THEN
    SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

-- ========== 政策表：客户对象配置 ==========
-- push_target_mode 为 NULL 或 all 时，行为与改造前一致（本园区全部客户企业）
CALL liqi_add_column('liqi_policy', 'push_target_mode',
  "varchar(20) NULL DEFAULT 'all' COMMENT '客户对象模式：all 本园区全部客户 / filter 按企业画像筛选'");
CALL liqi_add_column('liqi_policy', 'push_filter_conditions',
  "varchar(1000) NULL COMMENT '客户对象筛选条件快照 JSON（见 ParkPushFilterVO）'");

-- ========== 企业库：画像圈选相关索引 ==========
CALL liqi_add_index('liqi_enterprise', 'idx_insured_count',   '`insured_count`');
CALL liqi_add_index('liqi_enterprise', 'idx_establish_date',  '`establish_date`');
CALL liqi_add_index('liqi_enterprise', 'idx_industry_lv12',   '`industry_lv1_name`, `industry_lv2_name`');
CALL liqi_add_index('liqi_enterprise', 'idx_ent_name',        '`enterprise_name`');

-- ========== 园区客户管理：按企业名关联企业库/绑定表的索引 ==========
CALL liqi_add_index('liqi_member', 'idx_member_ent_name', '`enterprise_name`');

DROP PROCEDURE IF EXISTS `liqi_add_column`;
DROP PROCEDURE IF EXISTS `liqi_add_index`;

-- ========== 校验 ==========
SELECT '园区客户总数' AS 项, COUNT(*) AS 值 FROM liqi_member WHERE deleted = 0
UNION ALL SELECT '可推送客户（已绑定小程序）', COUNT(DISTINCT m.id) FROM liqi_member m
  JOIN liqi_user_enterprise_bind b ON b.enterprise_name = m.enterprise_name AND b.deleted = 0
  WHERE m.deleted = 0
UNION ALL SELECT '能匹配企业库（画像可用）', COUNT(*) FROM liqi_member m
  JOIN liqi_enterprise e ON e.enterprise_name = m.enterprise_name AND e.deleted = 0
  WHERE m.deleted = 0;
