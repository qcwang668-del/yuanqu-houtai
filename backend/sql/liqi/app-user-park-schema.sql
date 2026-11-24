-- 园区席位模块：会员/席位关联字段（幂等执行）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'liqi_app_user' AND COLUMN_NAME = 'park_id');
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `liqi_app_user` ADD COLUMN `park_id` bigint DEFAULT NULL COMMENT ''所属园区编号''',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;