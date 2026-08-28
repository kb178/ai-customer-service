package com.aicustomer.function;

/**
 * Long 类型解析工具
 *
 * 解决问题：Function Calling 时 LLM 可能对可选参数传 "None"、"null"、"" 等字符串
 * 而 DTO 的 Long 字段无法反序列化这些值
 *
 * 用法：在 Request DTO 的 setter 中调用 LongParser.parseLong(value)
 */
public class LongParser {

    /**
     * 将任意值安全地解析为 Long
     *
     * @param value 可以是 Number、String、null
     * @return Long 值，无法解析时返回 null
     */
    public static Long parse(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();

        String str = value.toString().trim().toLowerCase();
        if (str.isEmpty() || str.equals("none") || str.equals("null") || str.equals("undefined")) {
            return null;
        }
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
