-- 管理后台新增表
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `conversation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `customer_phone` VARCHAR(20) DEFAULT NULL COMMENT '客户手机号',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_customer_phone` (`customer_phone`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记录表';

CREATE TABLE IF NOT EXISTS `faq` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question` VARCHAR(500) NOT NULL COMMENT '问题',
    `answer` TEXT NOT NULL COMMENT '答案',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词(逗号分隔)',
    `weight` DOUBLE DEFAULT 1.0 COMMENT '匹配权重',
    `status` TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FAQ知识库表';

CREATE TABLE IF NOT EXISTS `system_prompt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content` TEXT NOT NULL COMMENT '提示词内容',
    `version` INT NOT NULL COMMENT '版本号',
    `is_active` TINYINT DEFAULT 0 COMMENT '1当前生效 0历史版本',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统提示词版本表';
