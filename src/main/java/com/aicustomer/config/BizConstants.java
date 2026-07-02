package com.aicustomer.config;

/**
 * 业务常量定义
 *
 * 集中管理课程ID、校区ID、预约状态、数据来源等魔法数字/字符串，
 * 避免散落在各处导致修改遗漏。
 */
public final class BizConstants {

    private BizConstants() {}

    // ========== 课程ID ==========

    public static final Long COURSE_JAVA_ID = 1L;
    public static final Long COURSE_PYTHON_ID = 2L;
    public static final Long COURSE_UI_ID = 3L;
    public static final Long COURSE_AI_ID = 4L;

    // ========== 校区ID ==========

    public static final Long CAMPUS_ZHONGGUANCUN_ID = 1L;
    public static final Long CAMPUS_GUOMAO_ID = 2L;
    public static final Long CAMPUS_XIZHIMEN_ID = 3L;

    // ========== 预约状态 ==========

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_CANCELLED = 3;

    // ========== 客户来源 ==========

    public static final String SOURCE_INSTRUCTION = "在线客服-咨询";
    public static final String SOURCE_FUNCTION_CALLING = "在线客服-FunctionCalling";
    public static final String SOURCE_RESERVATION = "在线客服-预约";
}
