-- ============================================
-- 校区课程关联表
-- 记录每个校区开设的课程、时间、容量
-- ============================================

-- 1. 校区课程关联表
CREATE TABLE IF NOT EXISTS campus_course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_id BIGINT NOT NULL COMMENT '校区ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    max_students INT DEFAULT 30 COMMENT '最大招生人数',
    current_students INT DEFAULT 0 COMMENT '当前学员数',
    status TINYINT DEFAULT 1 COMMENT '状态: 1开设 0暂停',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_campus_id (campus_id),
    INDEX idx_course_id (course_id),
    UNIQUE KEY uk_campus_course (campus_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校区课程关联表';

-- 2. 课程时间表
CREATE TABLE IF NOT EXISTS course_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campus_course_id BIGINT NOT NULL COMMENT '校区课程关联ID',
    day_of_week TINYINT NOT NULL COMMENT '星期几: 1-7(周一到周日)',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
    max_students INT DEFAULT 30 COMMENT '该时间段最大人数',
    current_students INT DEFAULT 0 COMMENT '当前人数',
    status TINYINT DEFAULT 1 COMMENT '状态: 1可用 0不可用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_campus_course_id (campus_course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程时间表';

-- 3. 插入校区课程关联数据
-- 北京中关村校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(1, 1, 30), (1, 2, 25), (1, 3, 20), (1, 4, 20), (1, 5, 30), (1, 6, 20);

-- 北京国贸校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(2, 1, 35), (2, 2, 30), (2, 5, 35), (2, 7, 25);

-- 上海徐汇校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(3, 1, 30), (3, 2, 30), (3, 3, 25), (3, 4, 25), (3, 5, 30);

-- 上海浦东校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(4, 1, 40), (4, 2, 35), (4, 4, 30), (4, 5, 35), (4, 8, 20);

-- 广州天河校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(5, 1, 25), (5, 2, 25), (5, 3, 20), (5, 5, 25);

-- 深圳南山校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(6, 1, 40), (6, 2, 35), (6, 4, 30), (6, 5, 40), (6, 7, 30);

-- 杭州西湖校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(7, 1, 25), (7, 2, 25), (7, 3, 20), (7, 6, 15);

-- 成都高新校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(8, 1, 30), (8, 2, 25), (8, 5, 30);

-- 武汉光谷校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(9, 1, 25), (9, 2, 25), (9, 5, 25);

-- 南京新街口校区
INSERT IGNORE INTO campus_course (campus_id, course_id, max_students) VALUES
(10, 1, 25), (10, 2, 20), (10, 3, 15), (10, 5, 25);

-- 4. 插入课程时间表数据
-- 每个校区课程默认4个时间段
INSERT IGNORE INTO course_schedule (campus_course_id, day_of_week, start_time, end_time, max_students)
SELECT id, 1, '09:00:00', '12:00:00', max_students FROM campus_course WHERE status = 1
ON DUPLICATE KEY UPDATE max_students = VALUES(max_students);

INSERT IGNORE INTO course_schedule (campus_course_id, day_of_week, start_time, end_time, max_students)
SELECT id, 1, '14:00:00', '17:00:00', max_students FROM campus_course WHERE status = 1
ON DUPLICATE KEY UPDATE max_students = VALUES(max_students);

INSERT IGNORE INTO course_schedule (campus_course_id, day_of_week, start_time, end_time, max_students)
SELECT id, 3, '09:00:00', '12:00:00', max_students FROM campus_course WHERE status = 1
ON DUPLICATE KEY UPDATE max_students = VALUES(max_students);

INSERT IGNORE INTO course_schedule (campus_course_id, day_of_week, start_time, end_time, max_students)
SELECT id, 3, '14:00:00', '17:00:00', max_students FROM campus_course WHERE status = 1
ON DUPLICATE KEY UPDATE max_students = VALUES(max_students);

INSERT IGNORE INTO course_schedule (campus_course_id, day_of_week, start_time, end_time, max_students)
SELECT id, 6, '09:00:00', '12:00:00', max_students FROM campus_course WHERE status = 1
ON DUPLICATE KEY UPDATE max_students = VALUES(max_students);

INSERT IGNORE INTO course_schedule (campus_course_id, day_of_week, start_time, end_time, max_students)
SELECT id, 6, '14:00:00', '17:00:00', max_students FROM campus_course WHERE status = 1
ON DUPLICATE KEY UPDATE max_students = VALUES(max_students);
