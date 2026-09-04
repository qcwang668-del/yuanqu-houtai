-- 力企企业库 · 融资信息子表（数据来源：外部投融资数据表）
-- 关联关系：liqi_enterprise_financing.enterprise_id -> liqi_enterprise.id
--          entity_id 冗余保留，用于数据导入匹配与溯源
-- 幂等：可重复执行

CREATE TABLE IF NOT EXISTS `liqi_enterprise_financing` (
  `id`            bigint        NOT NULL AUTO_INCREMENT COMMENT '编号',
  `enterprise_id` bigint        NOT NULL COMMENT '企业编号（关联 liqi_enterprise.id）',
  `entity_id`     varchar(64)   NULL COMMENT '外部 entity_id（冗余，用于溯源）',
  `source_id`     bigint        NULL COMMENT '外部融资记录 id（幂等去重用）',
  `rz_time`       datetime      NULL COMMENT '融资时间',
  `rz_amt`        varchar(128)  NULL COMMENT '融资金额（原始文本，如「数千万人民币」「未披露」）',
  `rz_amt_num`    decimal(20,2) NULL COMMENT '融资金额（数值·元，可解析时填入，用于排序/统计）',
  `rz_round`      varchar(64)   NULL COMMENT '融资轮次',
  `investor_info` text          NULL COMMENT '投资方信息 JSON 数组 [{"name":"...","id":"..."}]',
  `creator`     varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     bit(1)      DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   bigint      DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_ent` (`enterprise_id`),
  KEY `idx_entity` (`entity_id`),
  KEY `idx_source` (`source_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='力企企业库-融资信息';
