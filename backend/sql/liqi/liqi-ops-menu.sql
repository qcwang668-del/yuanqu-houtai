-- =============================================================
-- 惠企运营（liqi-ops）菜单与按钮级权限
-- 目录 2090；子菜单 2091 园区政策发布 / 2093 直播大讲堂 / 2094 推送记录
-- 2092「申报线索」已下线：H5 政策申报统一并入「预留信息管理」，保持 deleted=1。
-- 按钮权限 id 段 2110~2124，permission 与后端 @PreAuthorize 一一对应。
-- 导入：mysql -h127.0.0.1 -uroot --default-character-set=utf8mb4 -D ruoyi-vue-pro < liqi-ops-menu.sql
-- =============================================================
SET NAMES utf8mb4;

-- 1) 目录与菜单：补齐 permission（菜单本身留空，权限落在按钮上）
DELETE FROM `system_menu` WHERE `id` IN (2090,2091,2093,2094);
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
(2090,'惠企运营','',1,7,0,'/liqi-ops','ep:guide',NULL,NULL,0,b'1',b'1',b'1','admin',NOW(),'admin',NOW(),b'0'),
(2091,'园区政策发布','',2,1,2090,'policy-publish','ep:document','liqiOps/policyPublish/index','LiqiPolicyPublish',0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2093,'直播大讲堂','',2,3,2090,'live','ep:video-camera','liqiOps/live/index','LiqiLive',0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2094,'推送记录','',2,4,2090,'push-message','ep:bell','liqiOps/pushMessage/index','LiqiPushMessage',0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0');

-- 2) 按钮级权限（type=3）
DELETE FROM `system_menu` WHERE `id` BETWEEN 2110 AND 2124;
INSERT INTO `system_menu`
(`id`,`name`,`permission`,`type`,`sort`,`parent_id`,`path`,`icon`,`component`,`component_name`,`status`,`visible`,`keep_alive`,`always_show`,`creator`,`create_time`,`updater`,`update_time`,`deleted`)
VALUES
-- 园区政策发布 2091
(2110,'政策查询','liqi:policy:query',3,1,2091,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2111,'政策新增','liqi:policy:create',3,2,2091,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2112,'政策修改','liqi:policy:update',3,3,2091,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2113,'政策删除','liqi:policy:delete',3,4,2091,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
-- 直播大讲堂 2093
(2115,'直播查询','liqi:live:query',3,1,2093,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2116,'直播新增','liqi:live:create',3,2,2093,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2117,'直播修改','liqi:live:update',3,3,2093,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2118,'直播删除','liqi:live:delete',3,4,2093,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
-- 推送记录 2094
(2120,'推送记录查询','liqi:push-message:query',3,1,2094,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0'),
(2121,'执行推送','liqi:push-message:push',3,2,2094,'','','',NULL,0,b'1',b'1',b'0','admin',NOW(),'admin',NOW(),b'0');

-- 3) 授权给 super_admin(role_id=1) 与 tenant_admin(109/111)，幂等
INSERT INTO `system_role_menu` (`role_id`,`menu_id`,`creator`,`create_time`,`updater`,`update_time`,`deleted`,`tenant_id`)
SELECT r.id, m.id, 'admin', NOW(), 'admin', NOW(), b'0', IFNULL(r.tenant_id,0)
  FROM `system_role` r
  JOIN `system_menu` m ON m.id IN (2090,2091,2093,2094,2110,2111,2112,2113,2115,2116,2117,2118,2120,2121)
 WHERE r.id IN (1,109,111) AND r.deleted = b'0'
   AND NOT EXISTS (SELECT 1 FROM `system_role_menu` rm
                    WHERE rm.role_id = r.id AND rm.menu_id = m.id AND rm.deleted = b'0');

-- 4) 核对
SELECT id,parent_id,name,permission,type,component FROM system_menu
 WHERE id IN (2090,2091,2093,2094) OR (id BETWEEN 2110 AND 2124)
 ORDER BY parent_id,id;
