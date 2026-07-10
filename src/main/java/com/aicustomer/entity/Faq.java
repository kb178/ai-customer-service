package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("faq")
public class Faq {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 分类 */
    private String category;

    /** 关键词(逗号分隔) */
    private String keywords;

    /** 匹配权重 */
    private Double weight;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
