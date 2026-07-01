package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 课程时间表实体类
 * 记录每个校区课程的时间安排
 */
@Data
@TableName("course_schedule")
public class CourseSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 校区课程关联ID */
    private Long campusCourseId;

    /** 星期几: 1-7(周一到周日) */
    private Integer dayOfWeek;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 该时间段最大人数 */
    private Integer maxStudents;

    /** 当前人数 */
    private Integer currentStudents;

    /** 状态：1可用 0不可用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
