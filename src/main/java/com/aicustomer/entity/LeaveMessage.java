package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 留言实体类
 *
 * 对应数据库表：leave_message
 * 功能：记录AI无法处理时的学员留言
 */
@Data
@TableName("leave_message")
public class LeaveMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 学员姓名 */
    private String customerName;

    /** 联系电话 */
    private String customerPhone;

    /** 留言内容 */
    private String message;

    /** 留言分类（退款咨询/课程问题/投诉建议等） */
    private String category;

    /** 状态：0待处理 1处理中 2已解决 3已忽略 */
    private Integer status;

    /** 处理人 */
    private String handler;

    /** 处理备注 */
    private String handleRemark;

    /** 处理时间 */
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
