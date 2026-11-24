-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: ruoyi-vue-pro
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `liqi_park_member`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liqi_park_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `park_id` bigint NOT NULL COMMENT '所属园区',
  `user_id` bigint NOT NULL COMMENT '平台账号编号',
  `phone` varchar(20) NOT NULL DEFAULT '' COMMENT '注册手机号',
  `user_name` varchar(64) NOT NULL DEFAULT '' COMMENT '账号名称',
  `company` varchar(255) DEFAULT '' COMMENT '指派时企业快照',
  `plan_code` varchar(32) NOT NULL DEFAULT '' COMMENT '套餐编码',
  `plan_name` varchar(64) NOT NULL DEFAULT '' COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '套餐年价',
  `start_time` datetime NOT NULL COMMENT '生效时间',
  `expire_time` datetime NOT NULL COMMENT '到期时间',
  `source` varchar(32) NOT NULL DEFAULT 'PARK_SEAT' COMMENT '来源：PARK_SEAT=园区席位',
  `batch_id` bigint DEFAULT NULL COMMENT '扣减批次',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0生效,1已过期',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_park_member_user` (`user_id`,`expire_time`),
  KEY `idx_park_member_park` (`park_id`,`expire_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='力企-园区会员权益';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liqi_park_seat_batch`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liqi_park_seat_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `park_id` bigint NOT NULL COMMENT '园区编号',
  `batch_no` varchar(64) NOT NULL DEFAULT '' COMMENT '授权批次号',
  `source` varchar(32) NOT NULL DEFAULT '' COMMENT '来源：初始授权/增购/续费',
  `qty` int NOT NULL DEFAULT '0' COMMENT '授权席位',
  `used_qty` int NOT NULL DEFAULT '0' COMMENT '已指派（累计）',
  `start_date` date DEFAULT NULL COMMENT '生效日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日期',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0有效,1已过期',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_seat_batch_no` (`batch_no`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='力企-园区席位授权批次';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liqi_park_seat_log`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liqi_park_seat_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `park_id` bigint NOT NULL COMMENT '园区编号',
  `batch_id` bigint DEFAULT NULL COMMENT '扣减批次',
  `user_id` bigint DEFAULT NULL COMMENT '账号编号',
  `phone` varchar(20) DEFAULT '' COMMENT '手机号',
  `user_name` varchar(64) DEFAULT '' COMMENT '账号名称',
  `company` varchar(255) DEFAULT '' COMMENT '企业',
  `plan_code` varchar(32) DEFAULT '' COMMENT '套餐编码',
  `plan_name` varchar(64) DEFAULT '' COMMENT '套餐名称',
  `action` varchar(20) NOT NULL DEFAULT 'ASSIGN' COMMENT '动作：ASSIGN=指派',
  `result` varchar(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '结果',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `operator` varchar(64) DEFAULT '' COMMENT '操作人',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_park_seat_log_park` (`park_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='力企-园区席位指派记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liqi_park_seat_order`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liqi_park_seat_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `park_id` bigint NOT NULL COMMENT '园区编号',
  `order_no` varchar(64) NOT NULL DEFAULT '' COMMENT '订单号',
  `order_type` varchar(20) NOT NULL DEFAULT 'PURCHASE' COMMENT 'PURCHASE=增购,RENEW=续费',
  `qty` int NOT NULL DEFAULT '0' COMMENT '席位数',
  `unit_price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '单价',
  `amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '合同金额',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态:0待支付,1已支付',
  `pay_time` datetime DEFAULT NULL COMMENT '支付时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_seat_order_no` (`order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='力企-园区席位增购续费记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `liqi_member_plan`
--

/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `liqi_member_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `plan_code` varchar(32) NOT NULL DEFAULT '' COMMENT '套餐编码',
  `plan_name` varchar(64) NOT NULL DEFAULT '' COMMENT '套餐名称',
  `price` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '年价',
  `duration_days` int NOT NULL DEFAULT '365' COMMENT '有效期天数',
  `benefits` varchar(500) DEFAULT '' COMMENT '权益摘要',
  `sort` int NOT NULL DEFAULT '0' COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态:0上架,1下架',
  `creator` varchar(64) DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT '0' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member_plan_code` (`plan_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='力企-会员套餐定义';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-24 16:08:30
