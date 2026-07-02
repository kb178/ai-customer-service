package com.aicustomer.annotation;

/**
 * 关键词映射：匹配到任意关键词时，将消息中的值映射为目标值
 */
public @interface KeywordMapping {
    /** 触发关键词列表（忽略大小写） */
    String[] keywords();

    /** 匹配后设置的目标值 */
    String value();
}
