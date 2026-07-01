package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 校区实体类
 * 
 * 对应数据库表：campus
 * 功能：存储校区相关信息
 */
@Data
@TableName("campus")
public class Campus {

    /** 校区ID（主键，自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 校区名称 */
    private String name;

    /** 校区地址 */
    private String address;

    /** 所属省份ID */
    private Long provinceId;

    /** 所属城市ID */
    private Long cityId;

    /** 联系电话 */
    private String phone;

    /** 营业时间（如：09:00-21:00） */
    private String businessHours;

    /** 纬度（用于地图定位） */
    private Double latitude;

    /** 经度（用于地图定位） */
    private Double longitude;

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
