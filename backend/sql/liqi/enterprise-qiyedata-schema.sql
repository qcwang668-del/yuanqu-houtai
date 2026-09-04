-- 力企企业库 · 对接「企业数据平台 - 企业基本信息」接口（/app-api/business/qiyedata/qiye-base-info）
-- 按接口返回字段（QiYeInfoNewVO）适配主表结构；已有语义相同的列复用，不新增重复列：
--   legalName→legal_person、entStatus→reg_status、regAddress→register_address、regDate→establish_date、
--   licenseNumber→reg_number、zcbComType→company_org_type、regOrg→reg_institute、opScope→business_scope、
--   organizationNumber→org_number、socialStaffNum→insured_count、companyFormerName→former_name
-- 幂等：列已存在则跳过（可重复执行）

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

-- ========== 主表：企业基本信息接口字段 ==========
CALL liqi_add_column('liqi_enterprise', 'entity_id',            "varchar(64) NULL COMMENT '企业数据平台 entityId（统一社会信用代码/唯一id）'");
CALL liqi_add_column('liqi_enterprise', 'english_name',         "varchar(255) NULL COMMENT '企业英文名称 entityEnglishName'");
CALL liqi_add_column('liqi_enterprise', 'website',              "varchar(255) NULL COMMENT '网址 zcbWeb'");
CALL liqi_add_column('liqi_enterprise', 'email',                "varchar(128) NULL COMMENT '邮箱 zcbEmail'");
CALL liqi_add_column('liqi_enterprise', 'reg_capital_amount',   "decimal(20,4) NULL COMMENT '注册资本金额 regCapital'");
CALL liqi_add_column('liqi_enterprise', 'reg_capital_type',     "varchar(32) NULL COMMENT '注册资本币种 regCapitalType'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv1',         "int NULL COMMENT '行业一级分类编码'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv1_name',    "varchar(128) NULL COMMENT '行业一级分类名'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv2',         "int NULL COMMENT '行业二级分类编码'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv2_name',    "varchar(128) NULL COMMENT '行业二级分类名'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv3',         "int NULL COMMENT '行业三级分类编码'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv3_name',    "varchar(128) NULL COMMENT '行业三级分类名'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv4',         "int NULL COMMENT '行业四级分类编码'");
CALL liqi_add_column('liqi_enterprise', 'industry_lv4_name',    "varchar(128) NULL COMMENT '行业四级分类名'");
CALL liqi_add_column('liqi_enterprise', 'op_from',              "varchar(32) NULL COMMENT '经营期限自 opFrom'");
CALL liqi_add_column('liqi_enterprise', 'op_to',                "varchar(32) NULL COMMENT '经营期限至 opTo'");
CALL liqi_add_column('liqi_enterprise', 'check_date',           "varchar(32) NULL COMMENT '核准日期 checkDate'");
CALL liqi_add_column('liqi_enterprise', 'reg_provinces_code',   "int NULL COMMENT '省码 regProvincesCode'");
CALL liqi_add_column('liqi_enterprise', 'reg_city_code',        "int NULL COMMENT '市码 regCityCode'");
CALL liqi_add_column('liqi_enterprise', 'reg_district_code',    "int NULL COMMENT '区码 regDistrictCode'");
CALL liqi_add_column('liqi_enterprise', 'subsidy_total_money',  "decimal(20,4) NULL COMMENT '补贴总金额 subsidyTotalMoney'");
CALL liqi_add_column('liqi_enterprise', 'company_scale',        "tinyint NULL COMMENT '企业规模：1微型 2小型 3中型 4大型'");
CALL liqi_add_column('liqi_enterprise', 'final_show_info',      "text NULL COMMENT '最终展示信息（荣誉/资质等，; 分隔）'");
CALL liqi_add_column('liqi_enterprise', 'enrich_source',        "varchar(16) NULL COMMENT '最近回填数据源：qiyedata/tyc'");

-- 放宽既有列长度：平台返回的法定代表人（合伙企业可能是长机构名）、经营状态、企业类型等会超出原长度（幂等）
ALTER TABLE `liqi_enterprise`
  MODIFY COLUMN `enterprise_name`    varchar(255) NOT NULL COMMENT '企业名称',
  MODIFY COLUMN `short_name`         varchar(32)  NULL COMMENT '企业简称',
  MODIFY COLUMN `legal_person`       varchar(255) NULL COMMENT '法定代表人 legalName',
  MODIFY COLUMN `reg_status`         varchar(64)  NULL COMMENT '经营状态 entStatus',
  MODIFY COLUMN `company_org_type`   varchar(128) NULL COMMENT '企业类型 zcbComType',
  MODIFY COLUMN `reg_capital_type`   varchar(64)  NULL COMMENT '注册资本币种 regCapitalType',
  MODIFY COLUMN `registered_capital` varchar(64)  NULL COMMENT '注册资本（展示文本）',
  MODIFY COLUMN `register_address`   varchar(512) NULL COMMENT '注册地址 regAddress',
  MODIFY COLUMN `reg_institute`      varchar(255) NULL COMMENT '登记机关 regOrg';

-- entityId 唯一定位企业，建索引便于按信用代码回查（幂等）
DROP PROCEDURE IF EXISTS `liqi_add_index`;
DELIMITER $$
CREATE PROCEDURE `liqi_add_index`(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_cols VARCHAR(256))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index) THEN
    SET @s = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_cols, ')');
    PREPARE stmt FROM @s;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL liqi_add_index('liqi_enterprise', 'idx_entity_id', '`entity_id`');

DROP PROCEDURE IF EXISTS `liqi_add_index`;
DROP PROCEDURE IF EXISTS `liqi_add_column`;
