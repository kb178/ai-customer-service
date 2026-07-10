package com.aicustomer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BizConstants 单元测试
 *
 * BizConstants 集中定义了系统中所有的业务常量：
 * - 课程ID：COURSE_JAVA_ID(1)、COURSE_PYTHON_ID(2)、COURSE_UI_ID(3)、COURSE_AI_ID(4)
 * - 校区ID：CAMPUS_ZHONGGUANCUN_ID(1)、CAMPUS_GUOMAO_ID(2)、CAMPUS_XIZHIMEN_ID(3)
 * - 预约状态：PENDING(0)、CONFIRMED(1)、COMPLETED(2)、CANCELLED(3)
 * - 客户来源：SOURCE_INSTRUCTION、SOURCE_FUNCTION_CALLING、SOURCE_RESERVATION
 *
 * 测试目的：
 * 这些常量散落在系统的各个角落（指令解析、Function Calling、数据库操作），
 * 如果某个常量被错误修改或重复定义，会导致严重Bug。
 * 这组测试确保常量的唯一性和正确性，作为"安全网"。
 */
class BizConstantsTest {

    /**
     * 测试：4个课程ID互不相同
     * 如果两个课程ID相同，AI输出的课程ID指令会混淆
     */
    @Test
    void 课程ID_唯一性() {
        assertNotEquals(BizConstants.COURSE_JAVA_ID, BizConstants.COURSE_PYTHON_ID);
        assertNotEquals(BizConstants.COURSE_JAVA_ID, BizConstants.COURSE_UI_ID);
        assertNotEquals(BizConstants.COURSE_JAVA_ID, BizConstants.COURSE_AI_ID);
        assertNotEquals(BizConstants.COURSE_PYTHON_ID, BizConstants.COURSE_UI_ID);
        assertNotEquals(BizConstants.COURSE_PYTHON_ID, BizConstants.COURSE_AI_ID);
        assertNotEquals(BizConstants.COURSE_UI_ID, BizConstants.COURSE_AI_ID);
    }

    /**
     * 测试：3个校区ID互不相同
     * 校区ID用于预约创建和修改，重复会导致预约关联错误
     */
    @Test
    void 校区ID_唯一性() {
        assertNotEquals(BizConstants.CAMPUS_ZHONGGUANCUN_ID, BizConstants.CAMPUS_GUOMAO_ID);
        assertNotEquals(BizConstants.CAMPUS_ZHONGGUANCUN_ID, BizConstants.CAMPUS_XIZHIMEN_ID);
        assertNotEquals(BizConstants.CAMPUS_GUOMAO_ID, BizConstants.CAMPUS_XIZHIMEN_ID);
    }

    /**
     * 测试：预约状态码是0/1/2/3连续值
     * 状态流转：待确认(0) → 已确认(1) → 已完成(2) 或 已取消(3)
     */
    @Test
    void 预约状态_连续性() {
        assertEquals(0, BizConstants.STATUS_PENDING);
        assertEquals(1, BizConstants.STATUS_CONFIRMED);
        assertEquals(2, BizConstants.STATUS_COMPLETED);
        assertEquals(3, BizConstants.STATUS_CANCELLED);
    }

    /**
     * 测试：4个预约状态码互不相同
     * 重复的状态码会导致预约状态判断错误
     */
    @Test
    void 预约状态_唯一性() {
        assertNotEquals(BizConstants.STATUS_PENDING, BizConstants.STATUS_CONFIRMED);
        assertNotEquals(BizConstants.STATUS_PENDING, BizConstants.STATUS_COMPLETED);
        assertNotEquals(BizConstants.STATUS_PENDING, BizConstants.STATUS_CANCELLED);
        assertNotEquals(BizConstants.STATUS_CONFIRMED, BizConstants.STATUS_COMPLETED);
        assertNotEquals(BizConstants.STATUS_CONFIRMED, BizConstants.STATUS_CANCELLED);
        assertNotEquals(BizConstants.STATUS_COMPLETED, BizConstants.STATUS_CANCELLED);
    }

    /**
     * 测试：3个客户来源字符串非空
     * 来源字段用于统计分析，空字符串会导致数据质量问题
     */
    @Test
    void 客户来源_非空() {
        assertNotNull(BizConstants.SOURCE_INSTRUCTION);
        assertNotNull(BizConstants.SOURCE_FUNCTION_CALLING);
        assertNotNull(BizConstants.SOURCE_RESERVATION);
        assertFalse(BizConstants.SOURCE_INSTRUCTION.isEmpty());
        assertFalse(BizConstants.SOURCE_FUNCTION_CALLING.isEmpty());
        assertFalse(BizConstants.SOURCE_RESERVATION.isEmpty());
    }

    /**
     * 测试：3个客户来源字符串互不相同
     * 用于区分客户是通过哪种模式进入系统的
     */
    @Test
    void 客户来源_互不相同() {
        assertNotEquals(BizConstants.SOURCE_INSTRUCTION, BizConstants.SOURCE_FUNCTION_CALLING);
        assertNotEquals(BizConstants.SOURCE_INSTRUCTION, BizConstants.SOURCE_RESERVATION);
        assertNotEquals(BizConstants.SOURCE_FUNCTION_CALLING, BizConstants.SOURCE_RESERVATION);
    }
}
