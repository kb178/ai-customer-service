package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 预约状态变更日志实体类
 */
@Data
@TableName("reservation_log")
public class ReservationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约ID */
    private Long reservationId;

    /** 原状态 */
    private Integer oldStatus;

    /** 新状态 */
    private Integer newStatus;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
