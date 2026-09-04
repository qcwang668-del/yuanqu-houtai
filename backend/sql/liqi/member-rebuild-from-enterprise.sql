-- 园区客户（会员）数据重建：按真实企业库 liqi_enterprise 生成
--
-- 背景：线上 liqi_member 原为 5000 条模拟数据（创建于 2026-08-26，手机号连号），
--       与 daas-import 导入的真实企业库（7222 条）企业名称零交集，
--       导致「是否融资」筛选恒为 0 条。
--
-- 口径：liqi_member.enterprise_name 与 liqi_enterprise.enterprise_name 一一对应，
--       使「是否融资」「所属园区」等按企业名关联的筛选正常生效。
--
-- 注意：企业库无电话字段、liqi_enterprise_contact 为空表，
--       故 phone / responsible_person_phone / contact_person_phone 置空串
--       （NOT NULL 约束），不伪造假号码。
--
-- 执行前务必备份：
--   mysqldump -uroot -p --single-transaction ruoyi-vue-pro liqi_member liqi_member_clue > backup.sql

START TRANSACTION;

DELETE FROM liqi_member;
ALTER TABLE liqi_member AUTO_INCREMENT = 1;

INSERT INTO liqi_member
  (enterprise_name, legal_person, register_address, phone,
   responsible_person_name, responsible_person_phone,
   contact_person_name, contact_person_phone,
   is_pushed_customer, creator_name, creator, updater, deleted, tenant_id)
SELECT
  e.enterprise_name,
  IFNULL(e.legal_person, ''),
  IFNULL(e.register_address, ''),
  '',
  IFNULL(e.legal_person, ''),
  '',
  IFNULL(e.legal_person, ''),
  '',
  0,
  '系统导入',
  'daas-rebuild',
  'daas-rebuild',
  b'0',
  e.tenant_id
FROM liqi_enterprise e
WHERE e.deleted = 0
ORDER BY e.id;

COMMIT;

-- 园区地址关键词修正：真实企业地址多为「深圳湾科技生态园」（7095 家），
-- 原关键词「深圳湾生态园」仅命中 17 家，放宽为「深圳湾」可覆盖 7115 家。
UPDATE liqi_park SET address_keyword = '深圳湾' WHERE park_name = '深圳湾生态园';

-- ===== 校验 =====
SELECT '会员总数' AS 项, COUNT(*) AS 值 FROM liqi_member WHERE deleted = 0
UNION ALL SELECT '能匹配企业表', COUNT(*) FROM liqi_member m
  JOIN liqi_enterprise e ON e.enterprise_name = m.enterprise_name AND e.deleted = 0 WHERE m.deleted = 0
UNION ALL SELECT '有融资', COUNT(*) FROM liqi_member m WHERE m.deleted = 0 AND EXISTS (
  SELECT 1 FROM liqi_enterprise e JOIN liqi_enterprise_financing f ON f.enterprise_id = e.id AND f.deleted = 0
  WHERE e.deleted = 0 AND e.enterprise_name = m.enterprise_name)
UNION ALL SELECT '无融资', COUNT(*) FROM liqi_member m WHERE m.deleted = 0 AND NOT EXISTS (
  SELECT 1 FROM liqi_enterprise e JOIN liqi_enterprise_financing f ON f.enterprise_id = e.id AND f.deleted = 0
  WHERE e.deleted = 0 AND e.enterprise_name = m.enterprise_name);