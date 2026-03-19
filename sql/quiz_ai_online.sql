-- ================================================
-- 在线答题系统 - 完整数据库脚本
-- ================================================

CREATE DATABASE IF NOT EXISTS `quiz_ai_online` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `quiz_ai_online`;

-- ================================================
-- 1. 用户表
-- ================================================
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid',
  `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信unionId',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `password` VARCHAR(255) DEFAULT NULL COMMENT '密码(手机号登录用)',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `is_vip` TINYINT NOT NULL DEFAULT 0 COMMENT 'VIP状态: 0-否 1-是',
  `vip_expire_time` DATETIME DEFAULT NULL COMMENT 'VIP到期时间',
  `settings` JSON DEFAULT NULL COMMENT '用户设置(JSON)',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ================================================
-- 2. 管理员表
-- ================================================
CREATE TABLE `admin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT '' COMMENT '头像',
  `role` VARCHAR(20) NOT NULL DEFAULT 'admin' COMMENT '角色: super_admin/admin',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- ================================================
-- 3. 题库分类表
-- ================================================
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(500) DEFAULT '' COMMENT '图标URL',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序(越小越前)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库分类表';

-- ================================================
-- 4. 题库表
-- ================================================
CREATE TABLE `question_bank` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题库ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '题库名称',
  `description` VARCHAR(500) DEFAULT '' COMMENT '题库描述',
  `cover` VARCHAR(500) DEFAULT '' COMMENT '封面图URL',
  `question_count` INT NOT NULL DEFAULT 0 COMMENT '题目总数(冗余)',
  `practice_count` INT NOT NULL DEFAULT 0 COMMENT '练习次数',
  `exam_time` INT DEFAULT 60 COMMENT '考试时长(分钟)',
  `exam_question_count` INT DEFAULT 50 COMMENT '考试抽题数量',
  `pass_score` INT DEFAULT 60 COMMENT '及格分数',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- ================================================
-- 5. 题目表
-- ================================================
CREATE TABLE `question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `bank_id` BIGINT NOT NULL COMMENT '所属题库ID',
  `type` TINYINT NOT NULL COMMENT '题型: 1-单选 2-多选 3-判断 4-填空',
  `content` TEXT NOT NULL COMMENT '题目内容',
  `answer` VARCHAR(500) NOT NULL COMMENT '正确答案(多选用逗号分隔)',
  `analysis` TEXT DEFAULT NULL COMMENT '答案解析',
  `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '难度: 1-简单 2-中等 3-困难',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bank_id` (`bank_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ================================================
-- 6. 题目选项表
-- ================================================
CREATE TABLE `question_option` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选项ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `label` VARCHAR(10) NOT NULL COMMENT '选项标签: A/B/C/D',
  `content` VARCHAR(1000) NOT NULL COMMENT '选项内容',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';

-- ================================================
-- 7. 练习记录表
-- ================================================
CREATE TABLE `practice_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `bank_id` BIGINT NOT NULL COMMENT '题库ID',
  `mode` VARCHAR(20) NOT NULL COMMENT '练习模式: ORDER/RANDOM/WRONG',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总题数',
  `answer_count` INT NOT NULL DEFAULT 0 COMMENT '已答题数',
  `correct_count` INT NOT NULL DEFAULT 0 COMMENT '正确数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-进行中 1-已完成',
  `last_index` INT NOT NULL DEFAULT 0 COMMENT '上次答到的题号索引',
  `question_ids` JSON DEFAULT NULL COMMENT '题目ID列表(JSON)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_bank` (`user_id`, `bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习记录表';

-- ================================================
-- 8. 练习答题明细表
-- ================================================
CREATE TABLE `practice_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL COMMENT '练习记录ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct` TINYINT DEFAULT NULL COMMENT '是否正确: 0-错 1-对',
  `answer_time` INT DEFAULT NULL COMMENT '答题用时(秒)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  UNIQUE KEY `uk_record_question` (`record_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习答题明细表';

-- ================================================
-- 9. 考试记录表
-- ================================================
CREATE TABLE `exam_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `bank_id` BIGINT NOT NULL COMMENT '题库ID',
  `total_count` INT NOT NULL COMMENT '总题数',
  `correct_count` INT NOT NULL DEFAULT 0 COMMENT '正确数',
  `score` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `duration` INT DEFAULT NULL COMMENT '用时(秒)',
  `pass_score` INT NOT NULL COMMENT '及格分',
  `is_pass` TINYINT NOT NULL DEFAULT 0 COMMENT '是否及格: 0-否 1-是',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-考试中 1-已交卷',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '交卷时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_bank_id` (`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';

-- ================================================
-- 10. 考试答题表
-- ================================================
CREATE TABLE `exam_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT NOT NULL COMMENT '考试记录ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct` TINYINT DEFAULT NULL COMMENT '是否正确',
  PRIMARY KEY (`id`),
  KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题表';

-- ================================================
-- 11. 收藏表
-- ================================================
CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ================================================
-- 12. 错题表
-- ================================================
CREATE TABLE `wrong_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `bank_id` BIGINT NOT NULL COMMENT '题库ID',
  `wrong_count` INT NOT NULL DEFAULT 1 COMMENT '错误次数',
  `last_wrong_answer` VARCHAR(500) DEFAULT NULL COMMENT '最近一次错误答案',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_bank` (`user_id`, `bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题表';

-- ================================================
-- 13. VIP套餐表
-- ================================================
CREATE TABLE `vip_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name` VARCHAR(50) NOT NULL COMMENT '套餐名称',
  `duration` INT NOT NULL COMMENT '时长(天)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格(元)',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价(划线价)',
  `description` VARCHAR(500) DEFAULT '' COMMENT '套餐描述',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP套餐表';

-- ================================================
-- 14. VIP订单表
-- ================================================
CREATE TABLE `vip_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `plan_name` VARCHAR(50) NOT NULL COMMENT '套餐名称(冗余)',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `duration` INT NOT NULL COMMENT '购买时长(天)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付 1-已支付 2-已取消',
  `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式: wxpay/mock',
  `paid_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `vip_start_time` DATETIME DEFAULT NULL COMMENT 'VIP生效时间',
  `vip_end_time` DATETIME DEFAULT NULL COMMENT 'VIP到期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP订单表';

-- ================================================
-- 15. AI配置表
-- ================================================
CREATE TABLE `ai_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `base_url` VARCHAR(500) NOT NULL DEFAULT 'https://apis.iflow.cn/v1' COMMENT 'API地址',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API Key',
  `model` VARCHAR(100) NOT NULL DEFAULT 'TBStars2-200B-A13B' COMMENT '模型名称',
  `prompt_analysis` TEXT NOT NULL COMMENT '生成解析的Prompt模板',
  `prompt_answer` TEXT NOT NULL COMMENT '推导答案的Prompt模板',
  `prompt_both` TEXT NOT NULL COMMENT '生成答案+解析的Prompt模板',
  `max_tokens` INT NOT NULL DEFAULT 2000 COMMENT '最大token数',
  `temperature` DECIMAL(2,1) NOT NULL DEFAULT 0.7 COMMENT '温度参数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `bx_auth` VARCHAR(500) DEFAULT NULL COMMENT 'iFlow平台BXAuth Cookie',
  `iflow_name` VARCHAR(100) DEFAULT NULL COMMENT 'iFlow平台用户名称',
  `expire_time` DATETIME DEFAULT NULL COMMENT 'API Key过期时间',
  `auto_renew` TINYINT DEFAULT 0 COMMENT '是否自动续期: 0-否 1-是',
  `last_renew_time` DATETIME DEFAULT NULL COMMENT '上次续期时间',
  `last_renew_result` VARCHAR(500) DEFAULT NULL COMMENT '上次续期结果',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';

-- ================================================
-- 16. AI调用日志表
-- ================================================
CREATE TABLE `ai_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT DEFAULT NULL COMMENT '关联题目ID',
  `call_type` VARCHAR(20) NOT NULL DEFAULT 'ADMIN' COMMENT '调用类型: ADMIN-管理端 USER-用户端',
  `mode` VARCHAR(30) NOT NULL COMMENT '调用模式',
  `prompt` TEXT NOT NULL COMMENT '实际发送的Prompt',
  `result` TEXT DEFAULT NULL COMMENT 'AI返回结果',
  `tokens_used` INT DEFAULT NULL COMMENT '消耗token数',
  `cost_ms` INT DEFAULT NULL COMMENT '耗时(毫秒)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
  `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作管理员ID（管理端调用时记录）',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID（用户端调用时记录）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_call_type` (`call_type`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';

-- ================================================
-- 17. 系统配置表
-- ================================================
CREATE TABLE `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT DEFAULT NULL COMMENT '配置值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ================================================
-- 初始数据
-- ================================================

-- 默认超级管理员 (密码: admin123)
INSERT INTO `admin` (`username`, `password`, `nickname`, `role`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '超级管理员', 'super_admin');

-- 默认AI配置
INSERT INTO `ai_config` (`base_url`, `api_key`, `model`, `prompt_analysis`, `prompt_answer`, `prompt_both`, `max_tokens`, `temperature`) VALUES
('https://apis.iflow.cn/v1', '', 'TBStars2-200B-A13B',
 '题目：{content}\n选项：{options}\n正确答案：{answer}\n\n请用简洁的语言输出解析：\n1. 一句话说明为什么选{answer}\n2. 简要指出其他选项的错误\n不要啰嗦，直接输出解析内容。',
 '题目：{content}\n选项：{options}\n解析：{analysis}\n\n请直接输出答案选项字母，多选题用逗号分隔。',
 '题目：{content}\n选项：{options}\n\n请按格式输出：\n答案：[选项字母]\n解析：[简洁说明正确答案的原因，并简要指出其他选项的错误]',
 2000, 0.7);

-- 默认系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`) VALUES
('siteName', 'Quiz AI 在线答题系统'),
('siteDescription', '基于AI的智能在线答题平台'),
('copyright', '© 2025 Quiz AI'),
('registerEnabled', '1'),
('defaultNickname', '微信用户'),
('defaultPracticeCount', '20'),
('examTimePerQuestion', '60'),
('showAnalysis', '1'),
('allowWrongRetry', '1'),
('maxFileSize', '10'),
('allowedFileTypes', 'jpg,jpeg,png,gif,webp,svg');

-- 默认VIP套餐
INSERT INTO `vip_plan` (`name`, `duration`, `price`, `original_price`, `description`, `sort`) VALUES
('月度VIP', 30, 19.90, 29.90, '解锁全部题目AI智能解析', 1),
('季度VIP', 90, 49.90, 89.70, '解锁全部题目AI智能解析，季度更优惠', 2),
('年度VIP', 365, 149.90, 358.80, '解锁全部题目AI智能解析，年度最划算', 3);

-- 示例分类
INSERT INTO `category` (`name`, `icon`, `sort`) VALUES
('计算机基础', '', 1),
('编程语言', '', 2),
('数据库', '', 3),
('网络安全', '', 4);

-- AI对话消息表
CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role` varchar(20) NOT NULL COMMENT '消息角色: user/assistant',
  `content` text NOT NULL COMMENT '消息内容',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除: 0-未删除 1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_deleted` (`user_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话消息';
