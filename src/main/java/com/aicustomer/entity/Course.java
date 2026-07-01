package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程实体类
 * 
 * 对应数据库表：course
 * 功能：存储课程相关信息
 */
@Data
@TableName("course")
public class Course {

    /** 课程ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 课程名称 */
    private String name;

    /** 课程描述 */
    private String description;

    /** 课程分类ID */
    private Long categoryId;

    /** 课程分类名称（冗余字段，方便查询） */
    private String category;

    /** 课程价格 */
    private BigDecimal price;

    /** 课时（单位：小时） */
    private Integer duration;

    /** 目标人群（如：零基础学员、有一定编程基础） */
    private String targetAudience;

    /** 最大学员数 */
    private Integer maxStudents;

    /** 当前学员数 */
    private Integer currentStudents;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记：0未删除 1已删除 */
    @TableLogic
    private Integer deleted;
}
