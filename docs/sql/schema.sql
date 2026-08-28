-- ============================================
-- AI智能客服系统 - 完整数据库脚本
-- 包含所有表结构和初始数据
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_customer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ai_customer;

-- ============================================
-- 第一部分：表结构
-- ============================================

-- 1. 省份表
DROP TABLE IF EXISTS `province`;
CREATE TABLE `province` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '省份名称',
    `code` VARCHAR(10) NOT NULL COMMENT '省份编码',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='省份表';

-- 2. 城市表
DROP TABLE IF EXISTS `city`;
CREATE TABLE `city` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '城市名称',
    `code` VARCHAR(10) NOT NULL COMMENT '城市编码',
    `province_id` BIGINT NOT NULL COMMENT '所属省份ID',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_province_id` (`province_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='城市表';

-- 3. 课程分类表
DROP TABLE IF EXISTS `course_category`;
CREATE TABLE `course_category` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
    `description` VARCHAR(200) COMMENT '分类描述',
    `icon` VARCHAR(50) COMMENT '图标',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类表';

-- 4. 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '课程名称',
    `category_id` BIGINT COMMENT '课程分类ID',
    `description` TEXT COMMENT '课程描述',
    `category` VARCHAR(50) COMMENT '课程分类名称(冗余)',
    `price` DECIMAL(10, 2) COMMENT '课程价格',
    `duration` INT COMMENT '课时(小时)',
    `target_audience` VARCHAR(200) COMMENT '目标人群',
    `max_students` INT DEFAULT 0 COMMENT '最大学员数',
    `current_students` INT DEFAULT 0 COMMENT '当前学员数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 5. 校区表
DROP TABLE IF EXISTS `campus`;
CREATE TABLE `campus` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(100) NOT NULL COMMENT '校区名称',
    `address` VARCHAR(200) COMMENT '地址',
    `province_id` BIGINT COMMENT '所属省份ID',
    `city_id` BIGINT COMMENT '所属城市ID',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `business_hours` VARCHAR(100) COMMENT '营业时间',
    `latitude` DOUBLE COMMENT '纬度',
    `longitude` DOUBLE COMMENT '经度',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_province_id` (`province_id`),
    INDEX `idx_city_id` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校区表';

-- 6. 客户表
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) COMMENT '客户姓名',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `email` VARCHAR(100) COMMENT '邮箱',
    `age` INT COMMENT '年龄',
    `education` VARCHAR(50) COMMENT '学历',
    `interest` VARCHAR(200) COMMENT '兴趣爱好',
    `source` VARCHAR(50) COMMENT '来源渠道',
    `province_id` BIGINT COMMENT '所在省份ID',
    `city_id` BIGINT COMMENT '所在城市ID',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 7. 预约表
DROP TABLE IF EXISTS `reservation`;
CREATE TABLE `reservation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `customer_name` VARCHAR(50) COMMENT '客户姓名',
    `phone` VARCHAR(20) COMMENT '联系电话',
    `course_id` BIGINT COMMENT '课程ID',
    `campus_id` BIGINT COMMENT '校区ID',
    `appointment_time` DATETIME COMMENT '预约时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0待确认 1已确认 2已完成 3已取消',
    `remark` TEXT COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 8. 预约状态变更日志表
DROP TABLE IF EXISTS `reservation_log`;
CREATE TABLE `reservation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `reservation_id` BIGINT NOT NULL COMMENT '预约ID',
    `old_status` INT COMMENT '原状态',
    `new_status` INT COMMENT '新状态',
    `operator` VARCHAR(50) COMMENT '操作人',
    `remark` VARCHAR(500) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_reservation_id` (`reservation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约状态变更日志表';

-- 9. 校区课程关联表
DROP TABLE IF EXISTS `campus_course`;
CREATE TABLE `campus_course` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `campus_id` BIGINT NOT NULL COMMENT '校区ID',
    `course_id` BIGINT NOT NULL COMMENT '课程ID',
    `max_students` INT DEFAULT 30 COMMENT '最大招生人数',
    `current_students` INT DEFAULT 0 COMMENT '当前学员数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1开设 0暂停',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_campus_course` (`campus_id`, `course_id`),
    INDEX `idx_campus_id` (`campus_id`),
    INDEX `idx_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校区课程关联表';

-- 10. 课程时间表
DROP TABLE IF EXISTS `course_schedule`;
CREATE TABLE `course_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `campus_course_id` BIGINT NOT NULL COMMENT '校区课程关联ID',
    `day_of_week` TINYINT NOT NULL COMMENT '星期几: 1-7(周一到周日)',
    `start_time` TIME NOT NULL COMMENT '开始时间',
    `end_time` TIME NOT NULL COMMENT '结束时间',
    `max_students` INT DEFAULT 30 COMMENT '该时间段最大人数',
    `current_students` INT DEFAULT 0 COMMENT '当前人数',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1可用 0不可用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_campus_course_id` (`campus_course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程时间表';

-- 11. 留言表
DROP TABLE IF EXISTS `leave_message`;
CREATE TABLE `leave_message` (
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

-- 12. 对话记录表
DROP TABLE IF EXISTS `conversation_log`;
CREATE TABLE `conversation_log` (
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

-- 13. FAQ知识库表
DROP TABLE IF EXISTS `faq`;
CREATE TABLE `faq` (
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

-- 14. 系统提示词版本表
DROP TABLE IF EXISTS `system_prompt`;
CREATE TABLE `system_prompt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content` TEXT NOT NULL COMMENT '提示词内容',
    `version` INT NOT NULL COMMENT '版本号',
    `is_active` TINYINT DEFAULT 0 COMMENT '1当前生效 0历史版本',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统提示词版本表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================
-- 第二部分：初始数据
-- ============================================

-- 省份数据（34个）
INSERT INTO `province` (`id`, `name`, `code`, `sort_order`) VALUES
(1, '北京', 'BJ', 1), (2, '天津', 'TJ', 2), (3, '河北', 'HE', 3), (4, '山西', 'SX', 4),
(5, '内蒙古', 'NM', 5), (6, '辽宁', 'LN', 6), (7, '吉林', 'JL', 7), (8, '黑龙江', 'HL', 8),
(9, '上海', 'SH', 9), (10, '江苏', 'JS', 10), (11, '浙江', 'ZJ', 11), (12, '安徽', 'AH', 12),
(13, '福建', 'FJ', 13), (14, '江西', 'JX', 14), (15, '山东', 'SD', 15), (16, '河南', 'HA', 16),
(17, '湖北', 'HB', 17), (18, '湖南', 'HN', 18), (19, '广东', 'GD', 19), (20, '广西', 'GX', 20),
(21, '海南', 'HI', 21), (22, '重庆', 'CQ', 22), (23, '四川', 'SC', 23), (24, '贵州', 'GZ', 24),
(25, '云南', 'YN', 25), (26, '西藏', 'XZ', 26), (27, '陕西', 'SN', 27), (28, '甘肃', 'GS', 28),
(29, '青海', 'QH', 29), (30, '宁夏', 'NX', 30), (31, '新疆', 'XJ', 31), (32, '台湾', 'TW', 32),
(33, '香港', 'HK', 33), (34, '澳门', 'MO', 34);

-- 城市数据（主要城市）
INSERT INTO `city` (`id`, `name`, `code`, `province_id`, `sort_order`) VALUES
(1, '北京', 'BJ01', 1, 1),
(2, '天津', 'TJ01', 2, 1),
(3, '石家庄', 'SJZ03', 3, 1), (4, '唐山', 'TS03', 3, 2), (5, '保定', 'BD03', 3, 3),
(6, '太原', 'TY04', 4, 1), (7, '大同', 'DT04', 4, 2),
(8, '呼和浩特', 'HHHT05', 5, 1),
(9, '沈阳', 'SY06', 6, 1), (10, '大连', 'DL06', 6, 2),
(11, '长春', 'CC07', 7, 1),
(12, '哈尔滨', 'HEB08', 8, 1),
(13, '上海', 'SH09', 9, 1),
(14, '南京', 'NJ10', 10, 1), (15, '苏州', 'SZ10', 10, 2), (16, '无锡', 'WX10', 10, 3), (17, '常州', 'CZ10', 10, 4),
(18, '杭州', 'HZ11', 11, 1), (19, '宁波', 'NB11', 11, 2), (20, '温州', 'WZ11', 11, 3),
(21, '合肥', 'HF12', 12, 1), (22, '芜湖', 'WH12', 12, 2),
(23, '福州', 'FZ13', 13, 1), (24, '厦门', 'XM13', 13, 2), (25, '泉州', 'QZ13', 13, 3),
(26, '南昌', 'NC14', 14, 1),
(27, '济南', 'JN15', 15, 1), (28, '青岛', 'QD15', 15, 2), (29, '烟台', 'YT15', 15, 3),
(30, '郑州', 'ZZ16', 16, 1), (31, '洛阳', 'LY16', 16, 2),
(32, '武汉', 'WH17', 17, 1), (33, '宜昌', 'YC17', 17, 2),
(34, '长沙', 'CS18', 18, 1), (35, '株洲', 'ZZ18', 18, 2),
(36, '广州', 'GZ19', 19, 1), (37, '深圳', 'SZ19', 19, 2), (38, '东莞', 'DG19', 19, 3), (39, '佛山', 'FS19', 19, 4),
(40, '南宁', 'NN20', 20, 1),
(41, '海口', 'HK21', 21, 1),
(42, '重庆', 'CQ22', 22, 1),
(43, '成都', 'CD23', 23, 1), (44, '绵阳', 'MY23', 23, 2),
(45, '贵阳', 'GY24', 24, 1),
(46, '昆明', 'KM25', 25, 1),
(47, '拉萨', 'LS26', 26, 1),
(48, '西安', 'XA27', 27, 1), (49, '咸阳', 'XY27', 27, 2),
(50, '兰州', 'LZ28', 28, 1),
(51, '西宁', 'XN29', 29, 1),
(52, '银川', 'YC30', 30, 1),
(53, '乌鲁木齐', 'WLMQ31', 31, 1);

-- 课程分类数据
INSERT INTO `course_category` (`id`, `name`, `description`, `icon`, `sort_order`) VALUES
(1, '编程开发', '软件开发、编程语言、Web开发等', '💻', 1),
(2, '数据科学', '数据分析、人工智能、机器学习等', '📊', 2),
(3, 'UI/UX设计', '界面设计、用户体验、交互设计等', '🎨', 3),
(4, '云计算', '云服务、DevOps、容器技术等', '☁️', 4),
(5, '网络安全', '信息安全、渗透测试、安全运维等', '🔒', 5),
(6, '项目管理', '敏捷开发、PMP、团队管理等', '📋', 6);

-- 课程数据
INSERT INTO `course` (`id`, `name`, `category_id`, `description`, `price`, `duration`, `target_audience`, `max_students`) VALUES
(1, 'Java全栈开发', 1, '从零开始学习Java Web全栈开发', 12800.00, 480, '零基础学员', 100),
(2, 'Python数据分析', 2, 'Python数据分析与可视化', 9800.00, 320, '有一定编程基础', 80),
(3, 'UI/UX设计', 3, '用户体验与界面设计', 8800.00, 240, '对设计感兴趣', 60),
(4, '人工智能入门', 2, 'AI基础与机器学习', 15800.00, 400, '计算机相关专业', 50),
(5, 'Web前端开发', 1, 'HTML/CSS/JavaScript/Vue/React', 9800.00, 360, '零基础学员', 100),
(6, 'Python爬虫', 1, '网络爬虫与数据采集', 6800.00, 180, '有Python基础', 40),
(7, '数据分析实战', 2, 'Excel/SQL/Python数据分析', 8800.00, 280, '职场人士', 60),
(8, 'Figma设计', 3, 'Figma界面设计实战', 5800.00, 160, '设计爱好者', 30);

-- 校区数据
INSERT INTO `campus` (`id`, `name`, `address`, `province_id`, `city_id`, `phone`, `business_hours`) VALUES
(1, '北京中关村校区', '北京市海淀区中关村大街100号', 1, 1, '010-88888888', '09:00-21:00'),
(2, '北京国贸校区', '北京市朝阳区国贸大厦B座', 1, 1, '010-66666666', '09:00-21:00'),
(3, '上海徐汇校区', '上海市徐汇区漕溪北路88号', 9, 13, '021-55555555', '09:00-21:00'),
(4, '上海浦东校区', '上海市浦东新区陆家嘴金融中心', 9, 13, '021-66666666', '09:00-21:00'),
(5, '广州天河校区', '广州市天河区天河路385号', 19, 36, '020-33333333', '09:00-21:00'),
(6, '深圳南山校区', '深圳市南山区科技园南区', 19, 37, '0755-22222222', '09:00-21:00'),
(7, '杭州西湖校区', '杭州市西湖区文三路100号', 11, 18, '0571-88888888', '09:00-21:00'),
(8, '成都高新校区', '成都市高新区天府大道999号', 23, 43, '028-77777777', '09:00-21:00'),
(9, '武汉光谷校区', '武汉市东湖高新区光谷大道88号', 17, 32, '027-66666666', '09:00-21:00'),
(10, '南京新街口校区', '南京市秦淮区新街口商圈', 10, 14, '025-55555555', '09:00-21:00');

-- 校区课程关联数据
INSERT INTO `campus_course` (`campus_id`, `course_id`, `max_students`, `current_students`) VALUES
-- 北京中关村校区（全部课程）
(1, 1, 30, 0), (1, 2, 25, 0), (1, 3, 20, 0), (1, 4, 20, 0), (1, 5, 30, 0), (1, 6, 20, 0), (1, 7, 25, 0), (1, 8, 15, 0),
-- 北京国贸校区
(2, 1, 35, 0), (2, 2, 30, 0), (2, 5, 35, 0), (2, 7, 25, 0),
-- 上海徐汇校区
(3, 1, 30, 0), (3, 2, 30, 0), (3, 3, 25, 0), (3, 4, 25, 0), (3, 5, 30, 0),
-- 上海浦东校区
(4, 1, 40, 0), (4, 2, 35, 0), (4, 4, 30, 0), (4, 5, 35, 0), (4, 8, 20, 0),
-- 广州天河校区
(5, 1, 25, 0), (5, 2, 25, 0), (5, 3, 20, 0), (5, 5, 25, 0),
-- 深圳南山校区
(6, 1, 40, 0), (6, 2, 35, 0), (6, 4, 30, 0), (6, 5, 40, 0), (6, 7, 30, 0),
-- 杭州西湖校区
(7, 1, 25, 0), (7, 2, 25, 0), (7, 3, 20, 0), (7, 6, 15, 0),
-- 成都高新校区
(8, 1, 30, 0), (8, 2, 25, 0), (8, 5, 30, 0),
-- 武汉光谷校区
(9, 1, 25, 0), (9, 2, 25, 0), (9, 5, 25, 0),
-- 南京新街口校区
(10, 1, 25, 0), (10, 2, 20, 0), (10, 3, 15, 0), (10, 5, 25, 0);

-- 课程时间表数据
-- 为每个校区课程创建6个时间段（周一、三、六的上午和下午）
INSERT INTO `course_schedule` (`campus_course_id`, `day_of_week`, `start_time`, `end_time`, `max_students`)
SELECT `id`, 1, '09:00:00', '12:00:00', `max_students` FROM `campus_course`;
INSERT INTO `course_schedule` (`campus_course_id`, `day_of_week`, `start_time`, `end_time`, `max_students`)
SELECT `id`, 1, '14:00:00', '17:00:00', `max_students` FROM `campus_course`;
INSERT INTO `course_schedule` (`campus_course_id`, `day_of_week`, `start_time`, `end_time`, `max_students`)
SELECT `id`, 3, '09:00:00', '12:00:00', `max_students` FROM `campus_course`;
INSERT INTO `course_schedule` (`campus_course_id`, `day_of_week`, `start_time`, `end_time`, `max_students`)
SELECT `id`, 3, '14:00:00', '17:00:00', `max_students` FROM `campus_course`;
INSERT INTO `course_schedule` (`campus_course_id`, `day_of_week`, `start_time`, `end_time`, `max_students`)
SELECT `id`, 6, '09:00:00', '12:00:00', `max_students` FROM `campus_course`;
INSERT INTO `course_schedule` (`campus_course_id`, `day_of_week`, `start_time`, `end_time`, `max_students`)
SELECT `id`, 6, '14:00:00', '17:00:00', `max_students` FROM `campus_course`;

-- FAQ知识库示例数据
INSERT INTO `faq` (`question`, `answer`, `category`, `keywords`, `weight`) VALUES
('课程价格是多少？', '我们的课程价格从5800元到15800元不等，具体价格取决于课程类型和课时长度。', '课程咨询', '价格,费用,多少钱', 1.0),
('可以试听吗？', '可以的！我们提供免费试听课程，您可以预约体验后再决定是否报名。', '课程咨询', '试听,体验', 1.0),
('上课时间怎么安排？', '我们有上午班(9:00-12:00)、下午班(14:00-17:00)可选，周一、三、六上课。', '课程咨询', '时间,安排,几点', 1.0),
('支持退款吗？', '退款政策请咨询我们的客服人员，会为您详细解答。', '退款', '退款,退费', 1.0);

-- 完成
SELECT '数据库初始化完成！共创建14张表' AS status;
