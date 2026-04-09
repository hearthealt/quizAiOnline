/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80041 (8.0.41)
 Source Host           : localhost:3306
 Source Schema         : quiz_ai_online

 Target Server Type    : MySQL
 Target Server Version : 80041 (8.0.41)
 File Encoding         : 65001

 Date: 09/04/2026 14:18:40
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admin
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'admin' COMMENT '角色: super_admin/admin',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '管理员表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_call_log
-- ----------------------------
DROP TABLE IF EXISTS `ai_call_log`;
CREATE TABLE `ai_call_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NULL DEFAULT NULL COMMENT '关联题目ID',
  `call_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'ADMIN' COMMENT '调用类型: ADMIN-管理端 USER-用户端',
  `mode` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调用模式',
  `route` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '实际调用链路: responses / chat / chat-minimal / chat-stream-fallback',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '实际发送的Prompt',
  `result` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI返回结果',
  `tokens_used` int NULL DEFAULT NULL COMMENT '消耗token数',
  `cost_ms` int NULL DEFAULT NULL COMMENT '耗时(毫秒)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作管理员ID（管理端调用时记录）',
  `user_id` bigint NULL DEFAULT NULL COMMENT '用户ID（用户端调用时记录）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_call_type`(`call_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI调用日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息角色: user/assistant',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_deleted`(`user_id` ASC, `deleted` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_config`;
CREATE TABLE `ai_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `provider` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'CUSTOM' COMMENT '提供商: OPENAI/DEEPSEEK/CUSTOM',
  `base_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'https://apis.iflow.cn/v1' COMMENT 'API地址',
  `api_key` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'API Key',
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT 'TBStars2-200B-A13B' COMMENT '模型名称',
  `max_tokens` int NOT NULL DEFAULT 2000 COMMENT '最大token数',
  `temperature` decimal(2, 1) NOT NULL DEFAULT 0.7 COMMENT '温度参数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `icon` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '图标URL',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序(越小越前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题库分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_answer
-- ----------------------------
DROP TABLE IF EXISTS `exam_answer`;
CREATE TABLE `exam_answer`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `exam_id` bigint NOT NULL COMMENT '考试记录ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `user_answer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户答案',
  `is_correct` tinyint NULL DEFAULT NULL COMMENT '是否正确',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_exam_id`(`exam_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考试答题表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for exam_record
-- ----------------------------
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bank_id` bigint NOT NULL COMMENT '题库ID',
  `total_count` int NOT NULL COMMENT '总题数',
  `correct_count` int NOT NULL DEFAULT 0 COMMENT '正确数',
  `score` int NOT NULL DEFAULT 0 COMMENT '得分',
  `duration` int NULL DEFAULT NULL COMMENT '用时(秒)',
  `pass_score` int NOT NULL COMMENT '及格分',
  `is_pass` tinyint NOT NULL DEFAULT 0 COMMENT '是否及格: 0-否 1-是',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-考试中 1-已交卷',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '交卷时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_bank_id`(`bank_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '考试记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for favorite
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_question`(`user_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for practice_detail
-- ----------------------------
DROP TABLE IF EXISTS `practice_detail`;
CREATE TABLE `practice_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `record_id` bigint NOT NULL COMMENT '练习记录ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `user_answer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户答案',
  `is_correct` tinyint NULL DEFAULT NULL COMMENT '是否正确: 0-错 1-对',
  `answer_time` int NULL DEFAULT NULL COMMENT '答题用时(秒)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_record_question`(`record_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_record_id`(`record_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习答题明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for practice_record
-- ----------------------------
DROP TABLE IF EXISTS `practice_record`;
CREATE TABLE `practice_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `bank_id` bigint NOT NULL COMMENT '题库ID',
  `mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '练习模式: ORDER/RANDOM/WRONG',
  `total_count` int NOT NULL DEFAULT 0 COMMENT '总题数',
  `answer_count` int NOT NULL DEFAULT 0 COMMENT '已答题数',
  `correct_count` int NOT NULL DEFAULT 0 COMMENT '正确数',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-进行中 1-已完成',
  `last_index` int NOT NULL DEFAULT 0 COMMENT '上次答到的题号索引',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `question_ids` json NULL COMMENT '题目ID列表(JSON)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_bank`(`user_id` ASC, `bank_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '练习记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `bank_id` bigint NOT NULL COMMENT '所属题库ID',
  `type` tinyint NOT NULL COMMENT '题型: 1-单选 2-多选 3-判断 4-填空',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题目内容',
  `answer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '正确答案(多选用逗号分隔)',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '答案解析',
  `difficulty` tinyint NOT NULL DEFAULT 1 COMMENT '难度: 1-简单 2-中等 3-困难',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_bank_id`(`bank_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题目表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question_bank
-- ----------------------------
DROP TABLE IF EXISTS `question_bank`;
CREATE TABLE `question_bank`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '题库ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '题库名称',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '题库描述',
  `cover` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '封面图URL',
  `question_count` int NOT NULL DEFAULT 0 COMMENT '题目总数(冗余)',
  `practice_count` int NULL DEFAULT 0 COMMENT '练习次数',
  `pass_score` int NULL DEFAULT 60 COMMENT '及格分数',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for question_option
-- ----------------------------
DROP TABLE IF EXISTS `question_option`;
CREATE TABLE `question_option`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '选项ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `label` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '选项标签: A/B/C/D',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '选项内容',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_question_id`(`question_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题目选项表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信openid',
  `union_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '微信unionId',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码(手机号登录用)',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像URL',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `is_vip` tinyint NOT NULL DEFAULT 0 COMMENT 'VIP状态: 0-否 1-是',
  `vip_expire_time` datetime NULL DEFAULT NULL COMMENT 'VIP到期时间',
  `settings` json NULL COMMENT '用户设置(JSON)',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_openid`(`openid` ASC) USING BTREE,
  UNIQUE INDEX `uk_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for vip_order
-- ----------------------------
DROP TABLE IF EXISTS `vip_order`;
CREATE TABLE `vip_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `plan_id` bigint NOT NULL COMMENT '套餐ID',
  `plan_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐名称(冗余)',
  `amount` decimal(10, 2) NOT NULL COMMENT '实付金额',
  `duration` int NOT NULL COMMENT '购买时长(天)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付 1-已支付 2-已取消',
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '支付方式: wxpay/mock',
  `paid_time` datetime NULL DEFAULT NULL COMMENT '支付时间',
  `vip_start_time` datetime NULL DEFAULT NULL COMMENT 'VIP生效时间',
  `vip_end_time` datetime NULL DEFAULT NULL COMMENT 'VIP到期时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'VIP订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for vip_plan
-- ----------------------------
DROP TABLE IF EXISTS `vip_plan`;
CREATE TABLE `vip_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '套餐名称',
  `duration` int NOT NULL COMMENT '时长(天)',
  `price` decimal(10, 2) NOT NULL COMMENT '价格(元)',
  `original_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '原价(划线价)',
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '套餐描述',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'VIP套餐表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wrong_question
-- ----------------------------
DROP TABLE IF EXISTS `wrong_question`;
CREATE TABLE `wrong_question`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `question_id` bigint NOT NULL COMMENT '题目ID',
  `bank_id` bigint NOT NULL COMMENT '题库ID',
  `wrong_count` int NOT NULL DEFAULT 1 COMMENT '错误次数',
  `last_wrong_answer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最近一次错误答案',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_question`(`user_id` ASC, `question_id` ASC) USING BTREE,
  INDEX `idx_user_bank`(`user_id` ASC, `bank_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '错题表' ROW_FORMAT = Dynamic;

INSERT INTO `admin` VALUES (1, 'admin', '$2a$10$RJwdMklUVLK1S0noSMvX.e3Q3Bl4V.zReAlPP6k5PSwd2qTVzApd.', '管理员', '/uploads/21180087953643e09761fbb65dc13afd.jpg', 'super_admin', 1, '2026-04-04 12:56:54', '2026-03-18 09:10:02', '2026-04-04 12:56:54', 0);

INSERT INTO `vip_plan` VALUES (1, '月度VIP', 30, 19.90, 29.90, '解锁全部题目AI智能解析', 1, 1, '2026-03-18 09:10:02', '2026-03-18 09:10:02', 0);
INSERT INTO `vip_plan` VALUES (2, '季度VIP', 90, 49.90, 89.70, '解锁全部题目AI智能解析，季度更优惠', 2, 1, '2026-03-18 09:10:02', '2026-03-18 09:10:02', 0);
INSERT INTO `vip_plan` VALUES (3, '年度VIP', 365, 149.90, 358.80, '解锁全部题目AI智能解析，年度最划算', 3, 1, '2026-03-18 09:10:02', '2026-03-18 09:10:02', 0);

SET FOREIGN_KEY_CHECKS = 1;