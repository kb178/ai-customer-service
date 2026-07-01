package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 校区课程关联实体类
 * 记录每个校区开设哪些课程
 */
@Data
@TableName("campus_course")
public class CampusCourse {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 校区ID */
    private Long campusId;

    /** 课程ID */
    private Long courseId;

    /** 最大招生人数 */
    private Integer maxStudents;

    /** 当前学员数 */
    private Integer currentStudents;

    /** 状态：1开设 0暂停 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
