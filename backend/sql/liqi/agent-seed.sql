-- 智能体广场 种子数据（tenant_id=1，与现有 liqi_member 数据一致）
-- 幂等：先按 name 删除再插入
SET NAMES utf8mb4;

DELETE FROM `liqi_agent` WHERE `name` IN ('工商信息智能体', '企业司法风险智能体');

-- 工商信息智能体（启用 MCP 工具）
INSERT INTO `liqi_agent`
(`name`,`avatar`,`description`,`system_prompt`,`provider`,`model`,`mcp_enabled`,`mcp_url`,`sort`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
VALUES
('工商信息智能体','','查询企业工商登记、股东、投资、分支、主要人员等',
 '你是企业工商信息查询助手，必须通过工具查询真实数据后作答，查询企业前先用关键词搜索补全全称；数据来自天眼查/企业信息洞察。',
 'volcengine','ark-code-latest',b'1','http://120.79.142.141:8800/mcp/',1,0,
 'admin','2026-08-29 12:00:00','admin','2026-08-29 12:00:00',b'0',1);

-- 企业司法风险智能体（不启用 MCP）
INSERT INTO `liqi_agent`
(`name`,`avatar`,`description`,`system_prompt`,`provider`,`model`,`mcp_enabled`,`mcp_url`,`sort`,`status`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
VALUES
('企业司法风险智能体','','查询企业涉诉、被执行、失信等司法风险（数据源接入中）',
 '你是企业司法风险咨询助手。当前司法数据源尚在接入，若用户询问具体企业司法信息，请说明数据源正在接入、暂无法查询真实司法数据，可先做一般性说明。',
 'volcengine','ark-code-latest',b'0','',2,0,
 'admin','2026-08-29 12:00:00','admin','2026-08-29 12:00:00',b'0',1);
