package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预约实体类
 * 
 * 对应数据库表：reservation
 * 功能：存储预约试听相关信息
 * 
 * 状态说明：
 * - 0: 待确认
 * - 1: 已确认
 * - 2: 已完成
 * - 3: 已取消
 */
@Data
@TableName("reservation")
public class Reservation {

    /** 预约ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户姓名 */
    private String customerName;

    /** 联系电话 */
    private String phone;

    /** 课程ID（关联course表） */
    private Long courseId;

    /** 校区ID（关联campus表） */
    private Long campusId;

    /** 预约时间 */
    private LocalDateTime appointmentTime;

    /** 预约状态：0待确认 1已确认 2已完成 3已取消 */
    private Integer status;

    /** 备注信息 */
    private String remark;

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
