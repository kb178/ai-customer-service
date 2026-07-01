package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 客户实体类
 * 
 * 对应数据库表：customer
 * 功能：存储客户（潜在学员）相关信息
 */
@Data
@TableName("customer")
public class Customer {

    /** 客户ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户姓名 */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 邮箱地址 */
    private String email;

    /** 年龄 */
    private Integer age;

    /** 学历（如：大一、研究生、零基础） */
    private String education;

    /** 兴趣爱好（如：编程开发、UI设计） */
    private String interest;

    /** 客户来源（如：在线客服、线下活动） */
    private String source;

    /** 所在省份ID */
    private Long provinceId;

    /** 所在城市ID */
    private Long cityId;

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
