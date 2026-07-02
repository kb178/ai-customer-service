package com.aicustomer.annotation;

import java.lang.annotation.*;

/**
 * 实体信息提取注解
 * 用于从用户消息中提取结构化信息（兴趣、学历、课程、校区等）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityExtract {
    /** 提取后设置到 SessionContext 的字段名 */
    String field();

    /** 关键词 → 目标值的映射列表 */
    KeywordMapping[] mappings();
}
