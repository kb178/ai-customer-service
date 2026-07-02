package com.aicustomer.annotation;

import java.lang.annotation.*;

/**
 * 意图匹配注解
 * 用于定义对话中的意图关键词，自动匹配用户消息是否表达了该意图
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Intent {
    /** 意图名称 */
    String name();

    /** 触发该意图的关键词列表（忽略大小写） */
    String[] keywords();
}
