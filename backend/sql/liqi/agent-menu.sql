-- 智能体广场 菜单（三级：目录 → 菜单 → 查询按钮）
-- id 段 2100~2102（当前 system_menu max id = 2085，未占用）
-- 幂等：先 DELETE 再 INSERT
-- 说明：system_menu 为全局表；super_admin(admin, tenant 1) 默认可见全部菜单，无需 role_menu 绑定。
--       如需对特定租户/角色开放，把 2100,2101,2102 追加进 system_role_menu 或租户套餐 system_tenant_package.menu_ids。
DELETE FROM `system_menu` WHERE `id` IN (2100, 2101, 2102);

-- 一级目录：智能体广场
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(2100,'智能体广场','',1,7,0,'/agentSquare','ep:chat-dot-round',NULL,NULL,0,b'1',b'1',b'1','admin','2026-08-29 12:00:00','admin','2026-08-29 12:00:00',b'0');

-- 二级菜单：智能体广场
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(2101,'智能体广场','',2,1,2100,'index','ep:chat-line-round','agentSquare/index','AgentSquare',0,b'1',b'1',b'0','admin','2026-08-29 12:00:00','admin','2026-08-29 12:00:00',b'0');

-- 三级按钮：查询
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(2102,'智能体查询','liqi:agent:query',3,1,2101,'','',NULL,NULL,0,b'1',b'0',b'0','admin','2026-08-29 12:00:00','admin','2026-08-29 12:00:00',b'0');
