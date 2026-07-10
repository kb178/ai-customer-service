-- ============================================
-- AI智能客服系统 - 安全升级脚本
-- 执行前请先备份数据库！
-- ============================================

-- 1. 创建新表（使用IF NOT EXISTS，安全）
CREATE TABLE IF NOT EXISTS province (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '省份名称',
    code VARCHAR(10) NOT NULL COMMENT '省份编码',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='省份表';

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

-- 2. 安全修改表结构（先检查字段是否存在）
-- campus表
SET @dbname = DATABASE();
SET @tablename = 'campus';
SET @columnname = 'province_id';
SET @pre_count = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);
SET @sqlstmt = IF(@pre_count = 0, 'ALTER TABLE campus ADD COLUMN province_id BIGINT COMMENT ''所属省份ID'' AFTER address', 'SELECT ''Column province_id already exists'' AS status');
PREPARE alter_stmt FROM @sqlstmt;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @columnname = 'city_id';
SET @pre_count = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);
SET @sqlstmt = IF(@pre_count = 0, 'ALTER TABLE campus ADD COLUMN city_id BIGINT COMMENT ''所属城市ID'' AFTER province_id', 'SELECT ''Column city_id already exists'' AS status');
PREPARE alter_stmt FROM @sqlstmt;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

-- course表
SET @tablename = 'course';
SET @columnname = 'category_id';
SET @pre_count = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);
SET @sqlstmt = IF(@pre_count = 0, 'ALTER TABLE course ADD COLUMN category_id BIGINT COMMENT ''课程分类ID'' AFTER name', 'SELECT ''Column category_id already exists'' AS status');
PREPARE alter_stmt FROM @sqlstmt;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

-- customer表
SET @tablename = 'customer';
SET @columnname = 'province_id';
SET @pre_count = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);
SET @sqlstmt = IF(@pre_count = 0, 'ALTER TABLE customer ADD COLUMN province_id BIGINT COMMENT ''所在省份ID'' AFTER source', 'SELECT ''Column province_id already exists'' AS status');
PREPARE alter_stmt FROM @sqlstmt;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

SET @columnname = 'city_id';
SET @pre_count = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname);
SET @sqlstmt = IF(@pre_count = 0, 'ALTER TABLE customer ADD COLUMN city_id BIGINT COMMENT ''所在城市ID'' AFTER province_id', 'SELECT ''Column city_id already exists'' AS status');
PREPARE alter_stmt FROM @sqlstmt;
EXECUTE alter_stmt;
DEALLOCATE PREPARE alter_stmt;

-- 3. 插入数据（使用INSERT IGNORE避免重复）
-- 省份数据
INSERT IGNORE INTO province (id, name, code, sort_order) VALUES
(1, '北京', 'BJ', 1), (2, '天津', 'TJ', 2), (3, '河北', 'HE', 3), (4, '山西', 'SX', 4),
(5, '内蒙古', 'NM', 5), (6, '辽宁', 'LN', 6), (7, '吉林', 'JL', 7), (8, '黑龙江', 'HL', 8),
(9, '上海', 'SH', 9), (10, '江苏', 'JS', 10), (11, '浙江', 'ZJ', 11), (12, '安徽', 'AH', 12),
(13, '福建', 'FJ', 13), (14, '江西', 'JX', 14), (15, '山东', 'SD', 15), (16, '河南', 'HA', 16),
(17, '湖北', 'HB', 17), (18, '湖南', 'HN', 18), (19, '广东', 'GD', 19), (20, '广西', 'GX', 20),
(21, '海南', 'HI', 21), (22, '重庆', 'CQ', 22), (23, '四川', 'SC', 23), (24, '贵州', 'GZ', 24),
(25, '云南', 'YN', 25), (26, '西藏', 'XZ', 26), (27, '陕西', 'SN', 27), (28, '甘肃', 'GS', 28),
(29, '青海', 'QH', 29), (30, '宁夏', 'NX', 30), (31, '新疆', 'XJ', 31), (32, '台湾', 'TW', 32),
(33, '香港', 'HK', 33), (34, '澳门', 'MO', 34);

-- 城市数据
INSERT IGNORE INTO city (id, name, code, province_id, sort_order) VALUES
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
INSERT IGNORE INTO course_category (id, name, description, icon, sort_order) VALUES
(1, '编程开发', '软件开发、编程语言、Web开发等', '💻', 1),
(2, '数据科学', '数据分析、人工智能、机器学习等', '📊', 2),
(3, 'UI/UX设计', '界面设计、用户体验、交互设计等', '🎨', 3),
(4, '云计算', '云服务、DevOps、容器技术等', '☁️', 4),
(5, '网络安全', '信息安全、渗透测试、安全运维等', '🔒', 5),
(6, '项目管理', '敏捷开发、PMP、团队管理等', '📋', 6);

-- 4. 更新已有数据（关联分类）
-- 只更新category_id为NULL的记录
UPDATE course SET category_id = 1 WHERE name LIKE '%Java%' AND category_id IS NULL;
UPDATE course SET category_id = 2 WHERE (name LIKE '%Python%' OR name LIKE '%数据%') AND category_id IS NULL;
UPDATE course SET category_id = 3 WHERE name LIKE '%设计%' AND category_id IS NULL;
UPDATE course SET category_id = 2 WHERE name LIKE '%人工智能%' AND category_id IS NULL;

-- 5. 更新校区的省份和城市（根据校区名称推断）
UPDATE campus SET province_id = 1, city_id = 1 WHERE name LIKE '%北京%' AND province_id IS NULL;
UPDATE campus SET province_id = 9, city_id = 13 WHERE name LIKE '%上海%' AND province_id IS NULL;
UPDATE campus SET province_id = 19, city_id = 36 WHERE name LIKE '%广州%' AND province_id IS NULL;
UPDATE campus SET province_id = 19, city_id = 37 WHERE name LIKE '%深圳%' AND province_id IS NULL;
UPDATE campus SET province_id = 11, city_id = 18 WHERE name LIKE '%杭州%' AND province_id IS NULL;
UPDATE campus SET province_id = 23, city_id = 43 WHERE name LIKE '%成都%' AND province_id IS NULL;
UPDATE campus SET province_id = 17, city_id = 32 WHERE name LIKE '%武汉%' AND province_id IS NULL;
UPDATE campus SET province_id = 10, city_id = 14 WHERE name LIKE '%南京%' AND province_id IS NULL;

-- 完成！
SELECT '数据库升级完成！' AS status;
