-- 园区客户管理（liqi_member）改为直接引用企业库真实企业：
-- id 与 liqi_enterprise.id 一一对应，企业名/法人取自企业库；
-- 联系电话等字段企业库无数据源，置空由后续人工/接口补充。
TRUNCATE TABLE liqi_member;
INSERT INTO liqi_member
  (id, enterprise_name, legal_person, register_address, phone,
   responsible_person_name, responsible_person_phone,
   contact_person_name, contact_person_phone,
   is_pushed_customer, creator_name, creator, updater, deleted, tenant_id)
SELECT
  e.id,
  e.enterprise_name,
  IFNULL(e.legal_person, ''),
  IFNULL(e.register_address, ''),
  '',
  IFNULL(e.legal_person, ''),
  '',
  '',
  '',
  0,
  '企业库同步',
  '1',
  '1',
  b'0',
  1
FROM liqi_enterprise e
WHERE e.deleted = 0;
SELECT COUNT(*) AS member_rows FROM liqi_member;
SELECT COUNT(*) AS name_matched FROM liqi_member m JOIN liqi_enterprise e ON m.id = e.id WHERE m.enterprise_name = e.enterprise_name;
