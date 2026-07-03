-- 留言表
CREATE TABLE IF NOT EXISTS `leave_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `customer_name` VARCHAR(50) COMMENT '学员姓名',
    `customer_phone` VARCHAR(20) COMMENT '联系电话',
    `message` TEXT NOT NULL COMMENT '留言内容',
    `category` VARCHAR(50) COMMENT '留言分类（退款咨询/课程问题/投诉建议等）',
    `status` TINYINT DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已解决 3已忽略',
    `handler` VARCHAR(50) COMMENT '处理人',
    `handle_remark` TEXT COMMENT '处理备注',
    `handle_time` DATETIME COMMENT '处理时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言表';
