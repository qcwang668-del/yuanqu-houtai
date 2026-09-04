-- 智能体广场（方案①：自建轻量对话 + MCP，纯 JDK8，不依赖 spring-ai）
-- DO 继承 TenantBaseDO：含 tenant_id + BaseDO 公共列（creator/create_time/updater/update_time/deleted）
SET NAMES utf8mb4;

-- 1) 智能体定义
DROP TABLE IF EXISTS `liqi_agent`;
CREATE TABLE `liqi_agent` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(128) DEFAULT NULL COMMENT '智能体名称',
  `avatar` varchar(512) DEFAULT NULL COMMENT '头像 URL',
  `description` varchar(1024) DEFAULT NULL COMMENT '描述',
  `system_prompt` text COMMENT '系统提示词',
  `provider` varchar(64) DEFAULT NULL COMMENT '大模型供应商，如 volcengine',
  `model` varchar(128) DEFAULT NULL COMMENT '模型标识，如 ark-code-latest',
  `mcp_enabled` bit(1) DEFAULT b'0' COMMENT '是否启用 MCP 工具',
  `mcp_url` varchar(512) DEFAULT NULL COMMENT 'MCP 服务地址',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` int DEFAULT '0' COMMENT '状态：0=开启，1=关闭',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='力企-智能体定义';

-- 2) 会话
DROP TABLE IF EXISTS `liqi_agent_conversation`;
CREATE TABLE `liqi_agent_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_id` bigint DEFAULT NULL COMMENT '智能体编号',
  `user_id` bigint DEFAULT NULL COMMENT '用户编号',
  `title` varchar(255) DEFAULT NULL COMMENT '会话标题',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_agent_user` (`agent_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='力企-智能体会话';

-- 3) 消息
DROP TABLE IF EXISTS `liqi_agent_message`;
CREATE TABLE `liqi_agent_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `conversation_id` bigint DEFAULT NULL COMMENT '会话编号',
  `role` varchar(32) DEFAULT NULL COMMENT '角色：user/assistant/tool',
  `content` text COMMENT '消息内容',
  `tool_calls` text COMMENT '工具调用留痕（证据：入参+返回，JSON 文本）',
  `creator` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_conversation` (`conversation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='力企-智能体消息';
