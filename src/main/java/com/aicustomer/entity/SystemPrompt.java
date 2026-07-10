package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_prompt")
public class SystemPrompt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提示词内容 */
    private String content;

    /** 版本号 */
    private Integer version;

    /** 是否当前生效：1是 0否 */
    @TableField("is_active")
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
