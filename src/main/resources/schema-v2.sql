-- ============================================
-- AI智能客服系统 - 数据库优化脚本
-- ============================================

-- 1. 省份表
CREATE TABLE IF NOT EXISTS province (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '省份名称',
    code VARCHAR(10) NOT NULL COMMENT '省份编码',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='省份表';

-- 2. 城市表
CREATE TABLE IF NOT EXISTS city (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '城市名称',
    code VARCHAR(10) NOT NULL COMMENT '城市编码',
    province_id BIGINT NOT NULL COMMENT '所属省份ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_province_id (province_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='城市表';

-- 3. 课程分类表
CREATE TABLE IF NOT EXISTS course_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程分类表';

-- 4. 预约状态变更日志表
CREATE TABLE IF NOT EXISTS reservation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id BIGINT NOT NULL COMMENT '预约ID',
    old_status INT COMMENT '原状态',
    new_status INT COMMENT '新状态',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_reservation_id (reservation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约状态变更日志表';

-- 5. 修改校区表，增加省份和城市关联
ALTER TABLE campus ADD COLUMN province_id BIGINT COMMENT '所属省份ID' AFTER address;
ALTER TABLE campus ADD COLUMN city_id BIGINT COMMENT '所属城市ID' AFTER province_id;
ALTER TABLE campus ADD INDEX idx_province_id (province_id);
ALTER TABLE campus ADD INDEX idx_city_id (city_id);

-- 6. 修改课程表，关联分类
ALTER TABLE course ADD COLUMN category_id BIGINT COMMENT '课程分类ID' AFTER name;
ALTER TABLE course ADD INDEX idx_category_id (category_id);

-- 7. 修改客户表，增加地址信息
ALTER TABLE customer ADD COLUMN province_id BIGINT COMMENT '所在省份ID' AFTER source;
ALTER TABLE customer ADD COLUMN city_id BIGINT COMMENT '所在城市ID' AFTER province_id;

-- ============================================
-- 插入初始数据
-- ============================================

-- 插入省份数据
INSERT INTO province (name, code, sort_order) VALUES
('北京', 'BJ', 1), ('天津', 'TJ', 2), ('河北', 'HE', 3), ('山西', 'SX', 4),
('内蒙古', 'NM', 5), ('辽宁', 'LN', 6), ('吉林', 'JL', 7), ('黑龙江', 'HL', 8),
('上海', 'SH', 9), ('江苏', 'JS', 10), ('浙江', 'ZJ', 11), ('安徽', 'AH', 12),
('福建', 'FJ', 13), ('江西', 'JX', 14), ('山东', 'SD', 15), ('河南', 'HA', 16),
('湖北', 'HB', 17), ('湖南', 'HN', 18), ('广东', 'GD', 19), ('广西', 'GX', 20),
('海南', 'HI', 21), ('重庆', 'CQ', 22), ('四川', 'SC', 23), ('贵州', 'GZ', 24),
('云南', 'YN', 25), ('西藏', 'XZ', 26), ('陕西', 'SN', 27), ('甘肃', 'GS', 28),
('青海', 'QH', 29), ('宁夏', 'NX', 30), ('新疆', 'XJ', 31), ('台湾', 'TW', 32),
('香港', 'HK', 33), ('澳门', 'MO', 34);

-- 插入城市数据（主要城市）
INSERT INTO city (name, code, province_id, sort_order) VALUES
('北京', 'BJ01', 1, 1),
('天津', 'TJ01', 2, 1),
('石家庄', 'SJZ03', 3, 1), ('唐山', 'TS03', 3, 2), ('保定', 'BD03', 3, 3),
('太原', 'TY04', 4, 1), ('大同', 'DT04', 4, 2),
('呼和浩特', 'HHHT05', 5, 1),
('沈阳', 'SY06', 6, 1), ('大连', 'DL06', 6, 2),
('长春', 'CC07', 7, 1),
('哈尔滨', 'HEB08', 8, 1),
('上海', 'SH09', 9, 1),
('南京', 'NJ10', 10, 1), ('苏州', 'SZ10', 10, 2), ('无锡', 'WX10', 10, 3), ('常州', 'CZ10', 10, 4),
('杭州', 'HZ11', 11, 1), ('宁波', 'NB11', 11, 2), ('温州', 'WZ11', 11, 3),
('合肥', 'HF12', 12, 1), ('芜湖', 'WH12', 12, 2),
('福州', 'FZ13', 13, 1), ('厦门', 'XM13', 13, 2), ('泉州', 'QZ13', 13, 3),
('南昌', 'NC14', 14, 1),
('济南', 'JN15', 15, 1), ('青岛', 'QD15', 15, 2), ('烟台', 'YT15', 15, 3),
('郑州', 'ZZ16', 16, 1), ('洛阳', 'LY16', 16, 2),
('武汉', 'WH17', 17, 1), ('宜昌', 'YC17', 17, 2),
('长沙', 'CS18', 18, 1), ('株洲', 'ZZ18', 18, 2),
('广州', 'GZ19', 19, 1), ('深圳', 'SZ19', 19, 2), ('东莞', 'DG19', 19, 3), ('佛山', 'FS19', 19, 4),
('南宁', 'NN20', 20, 1),
('海口', 'HK21', 21, 1),
('重庆', 'CQ22', 22, 1),
('成都', 'CD23', 23, 1), ('绵阳', 'MY23', 23, 2),
('贵阳', 'GY24', 24, 1),
('昆明', 'KM25', 25, 1),
('拉萨', 'LS26', 26, 1),
('西安', 'XA27', 27, 1), ('咸阳', 'XY27', 27, 2),
('兰州', 'LZ28', 28, 1),
('西宁', 'XN29', 29, 1),
('银川', 'YC30', 30, 1),
('乌鲁木齐', 'WLMQ31', 31, 1);

-- 插入课程分类数据
INSERT INTO course_category (name, description, icon, sort_order) VALUES
('编程开发', '软件开发、编程语言、Web开发等', '💻', 1),
('数据科学', '数据分析、人工智能、机器学习等', '📊', 2),
('UI/UX设计', '界面设计、用户体验、交互设计等', '🎨', 3),
('云计算', '云服务、DevOps、容器技术等', '☁️', 4),
('网络安全', '信息安全、渗透测试、安全运维等', '🔒', 5),
('项目管理', '敏捷开发、PMP、团队管理等', '📋', 6);

-- 插入课程数据（关联分类）
INSERT INTO course (name, description, category_id, price, duration, target_audience) VALUES
('Java全栈开发', '从零开始学习Java Web全栈开发', 1, 12800.00, 480, '零基础学员'),
('Python数据分析', 'Python数据分析与可视化', 2, 9800.00, 320, '有一定编程基础'),
('UI/UX设计', '用户体验与界面设计', 3, 8800.00, 240, '对设计感兴趣'),
('人工智能入门', 'AI基础与机器学习', 2, 15800.00, 400, '计算机相关专业'),
('Web前端开发', 'HTML/CSS/JavaScript/Vue/React', 1, 9800.00, 360, '零基础学员'),
('Python爬虫', '网络爬虫与数据采集', 1, 6800.00, 180, '有Python基础'),
('数据分析实战', 'Excel/SQL/Python数据分析', 2, 8800.00, 280, '职场人士'),
('Figma设计', 'Figma界面设计实战', 3, 5800.00, 160, '设计爱好者');

-- 插入校区数据（包含省份和城市）
INSERT INTO campus (name, address, phone, business_hours, province_id, city_id) VALUES
('北京中关村校区', '北京市海淀区中关村大街100号', '010-88888888', '09:00-21:00', 1, 1),
('北京国贸校区', '北京市朝阳区国贸大厦B座', '010-66666666', '09:00-21:00', 1, 1),
('上海徐汇校区', '上海市徐汇区漕溪北路88号', '021-55555555', '09:00-21:00', 9, 13),
('上海浦东校区', '上海市浦东新区陆家嘴金融中心', '021-66666666', '09:00-21:00', 9, 13),
('广州天河校区', '广州市天河区天河路385号', '020-33333333', '09:00-21:00', 19, 45),
('深圳南山校区', '深圳市南山区科技园南区', '0755-22222222', '09:00-21:00', 19, 46),
('杭州西湖校区', '杭州市西湖区文三路100号', '0571-88888888', '09:00-21:00', 11, 21),
('成都高新校区', '成都市高新区天府大道999号', '028-77777777', '09:00-21:00', 23, 59),
('武汉光谷校区', '武汉市东湖高新区光谷大道88号', '027-66666666', '09:00-21:00', 17, 41),
('南京新街口校区', '南京市秦淮区新街口商圈', '025-55555555', '09:00-21:00', 10, 14);

-- 插入预约状态变更日志示例
-- （实际使用时由系统自动插入）
