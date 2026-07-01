package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 省份实体类
 */
@Data
@TableName("province")
public class Province {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 省份名称 */
    private String name;

    /** 省份编码 */
    private String code;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1启用 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
