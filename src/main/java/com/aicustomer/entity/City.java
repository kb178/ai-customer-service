package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 城市实体类
 */
@Data
@TableName("city")
public class City {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 城市名称 */
    private String name;

    /** 城市编码 */
    private String code;

    /** 所属省份ID */
    private Long provinceId;

    /** 排序 */
    private Integer sortOrder;

    /** 状态：1启用 0禁用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
