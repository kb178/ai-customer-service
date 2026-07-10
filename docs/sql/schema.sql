-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_customer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_customer;

-- 课程表
CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    description TEXT COMMENT '课程描述',
    category VARCHAR(50) COMMENT '课程分类',
    price DECIMAL(10, 2) COMMENT '课程价格',
    duration INT COMMENT '课时(小时)',
    target_audience VARCHAR(200) COMMENT '目标人群',
    max_students INT DEFAULT 0 COMMENT '最大学员数',
    current_students INT DEFAULT 0 COMMENT '当前学员数',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 校区表
CREATE TABLE IF NOT EXISTS campus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '校区名称',
    address VARCHAR(200) COMMENT '地址',
    phone VARCHAR(20) COMMENT '联系电话',
    business_hours VARCHAR(100) COMMENT '营业时间',
    latitude DOUBLE COMMENT '纬度',
    longitude DOUBLE COMMENT '经度',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校区表';

-- 预约表
CREATE TABLE IF NOT EXISTS reservation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(50) COMMENT '客户姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    course_id BIGINT COMMENT '课程ID',
    campus_id BIGINT COMMENT '校区ID',
    appointment_time DATETIME COMMENT '预约时间',
    status TINYINT DEFAULT 0 COMMENT '状态: 0待确认 1已确认 2已完成 3已取消',
    remark TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 客户表
CREATE TABLE IF NOT EXISTS customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) COMMENT '客户姓名',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    age INT COMMENT '年龄',
    education VARCHAR(50) COMMENT '学历',
    interest VARCHAR(200) COMMENT '兴趣爱好',
    source VARCHAR(50) COMMENT '来源渠道',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- 插入示例数据
INSERT INTO course (name, description, category, price, duration, target_audience) VALUES
('Java全栈开发', '从零开始学习Java Web全栈开发', '编程', 12800.00, 480, '零基础学员'),
('Python数据分析', 'Python数据分析与可视化', '数据分析', 9800.00, 320, '有一定编程基础'),
('UI/UX设计', '用户体验与界面设计', '设计', 8800.00, 240, '对设计感兴趣'),
('人工智能入门', 'AI基础与机器学习', '人工智能', 15800.00, 400, '计算机相关专业');

INSERT INTO campus (name, address, phone, business_hours) VALUES
('中关村校区', '北京市海淀区中关村大街100号', '010-88888888', '09:00-21:00'),
('国贸校区', '北京市朝阳区国贸大厦B座', '010-66666666', '09:00-21:00'),
('西直门校区', '北京市西城区西直门南大街20号', '010-77777777', '09:00-21:00');
