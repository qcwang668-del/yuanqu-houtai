-- 各业务表中的企业名称统一改为引用企业库（liqi_enterprise）真实企业，
-- 按 id 顺序取企业库前 N 家，业务字段（申报项目、线索状态、预约内容等）保持原值。

-- 1) 企业获批动态：企业名 / 信用代码 / 法人 取自企业库
UPDATE liqi_approval_dynamic d
JOIN (SELECT @r := @r + 1 AS rn, e.enterprise_name, e.credit_code, e.legal_person
      FROM liqi_enterprise e, (SELECT @r := 0) t
      WHERE e.deleted = 0 AND e.enrich_status = 1 ORDER BY e.id LIMIT 50) s
  ON s.rn = d.id
SET d.enterprise_name = s.enterprise_name,
    d.credit_code = IFNULL(s.credit_code, ''),
    d.legal_person = IFNULL(s.legal_person, '');

-- 2) 会员线索：企业名 / 注册地址 取自企业库
UPDATE liqi_member_clue c
JOIN (SELECT @r2 := @r2 + 1 AS rn, e.enterprise_name, e.register_address
      FROM liqi_enterprise e, (SELECT @r2 := 0) t
      WHERE e.deleted = 0 AND e.enrich_status = 1 ORDER BY e.id LIMIT 50) s
  ON s.rn = c.id
SET c.enterprise_name = s.enterprise_name,
    c.register_address = IFNULL(s.register_address, '');

-- 3) 智能匹配线索
UPDATE liqi_match_clue m
JOIN (SELECT @r3 := @r3 + 1 AS rn, e.enterprise_name
      FROM liqi_enterprise e, (SELECT @r3 := 0) t
      WHERE e.deleted = 0 AND e.enrich_status = 1 ORDER BY e.id LIMIT 50) s
  ON s.rn = m.id
SET m.company = s.enterprise_name;

-- 4) 评分线索
UPDATE liqi_scoring_clue sc
JOIN (SELECT @r4 := @r4 + 1 AS rn, e.enterprise_name
      FROM liqi_enterprise e, (SELECT @r4 := 0) t
      WHERE e.deleted = 0 AND e.enrich_status = 1 ORDER BY e.id LIMIT 50) s
  ON s.rn = sc.id
SET sc.company_name = s.enterprise_name;

-- 5) 预约信息：company 取企业名，entity_id 关联企业库编号
UPDATE liqi_reserve_info r
JOIN (SELECT @r5 := @r5 + 1 AS rn, e.id AS ent_id, e.enterprise_name
      FROM liqi_enterprise e, (SELECT @r5 := 0) t
      WHERE e.deleted = 0 AND e.enrich_status = 1 ORDER BY e.id LIMIT 50) s
  ON s.rn = r.id
SET r.company = s.enterprise_name,
    r.entity_id = s.ent_id;
