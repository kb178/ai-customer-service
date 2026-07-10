package com.aicustomer.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SessionContext 实体单元测试
 *
 * SessionContext 是对话系统的核心数据结构，负责保存一次对话中的所有上下文信息。
 * 每个用户会话对应一个 SessionContext 实例，贯穿整个对话生命周期。
 *
 * 测试覆盖：
 * - hasInfo：判断某个信息是否已被收集（决定是否重复询问用户）
 * - getKnownInfoSummary：生成已知信息摘要（注入到AI系统提示词中）
 * - getMissingInfoForReservation：检测预约所需信息是否齐全
 * - addMessage：对话历史记录管理（防止内存溢出）
 * - touch / 构造函数：会话活跃时间管理（用于过期清理）
 * - pendingUpdate / pendingCancelReason：预约修改/取消的确认暂存机制
 */
class SessionContextTest {

    // ==================== hasInfo 测试 ====================
    // hasInfo 是信息收集的核心判断方法，AI 根据它决定是否需要询问用户

    /**
     * 测试：已设置姓名时，hasInfo("name") 应返回 true
     * 场景：用户说了"我叫张三"，系统提取到姓名后，后续不应再问"请问您贵姓"
     */
    @Test
    void hasInfo_姓名已设置_返回true() {
        SessionContext ctx = new SessionContext();
        ctx.setCustomerName("张三");
        assertTrue(ctx.hasInfo("name"));
    }

    /**
     * 测试：姓名为空时，hasInfo("name") 应返回 false
     * 场景：新会话刚开始，还没收集到姓名
     */
    @Test
    void hasInfo_姓名为空_返回false() {
        SessionContext ctx = new SessionContext();
        assertFalse(ctx.hasInfo("name"));
    }

    /**
     * 测试：已设置电话时，hasInfo("phone") 应返回 true
     * 场景：用户提供了手机号，系统应记住
     */
    @Test
    void hasInfo_电话已设置_返回true() {
        SessionContext ctx = new SessionContext();
        ctx.setPhone("13800138000");
        assertTrue(ctx.hasInfo("phone"));
    }

    /**
     * 测试：电话为空字符串时，hasInfo("phone") 应返回 false
     * 边界情况：setPhone("") 不应被认为已收集到电话
     */
    @Test
    void hasInfo_电话为空字符串_返回false() {
        SessionContext ctx = new SessionContext();
        ctx.setPhone("");
        assertFalse(ctx.hasInfo("phone"));
    }

    /**
     * 测试：已选择课程时，hasInfo("course") 应返回 true
     * 场景：用户说"我想学Python"，系统识别到课程选择
     */
    @Test
    void hasInfo_课程已选择_返回true() {
        SessionContext ctx = new SessionContext();
        ctx.setSelectedCourseId(1L);
        assertTrue(ctx.hasInfo("course"));
    }

    /**
     * 测试：已选择校区时，hasInfo("campus") 应返回 true
     * 场景：用户说"国贸校区"，系统识别到校区选择
     */
    @Test
    void hasInfo_校区已选择_返回true() {
        SessionContext ctx = new SessionContext();
        ctx.setSelectedCampusId(2L);
        assertTrue(ctx.hasInfo("campus"));
    }

    /**
     * 测试：传入未定义的字段名时，应返回 false
     * 防御性测试：确保不会因为错误的字段名导致异常
     */
    @Test
    void hasInfo_未知字段_返回false() {
        SessionContext ctx = new SessionContext();
        assertFalse(ctx.hasInfo("unknown"));
    }

    /**
     * 测试：学历和兴趣字段能正确判断
     * 场景：AI 提取到用户学历为"研究生"、兴趣为"编程开发"
     */
    @Test
    void hasInfo_学历和兴趣_返回true() {
        SessionContext ctx = new SessionContext();
        ctx.setEducation("研究生");
        ctx.setInterest("编程开发");
        assertTrue(ctx.hasInfo("education"));
        assertTrue(ctx.hasInfo("interest"));
    }

    // ==================== getKnownInfoSummary 测试 ====================
    // 此方法生成的摘要会注入到 AI 系统提示词中，直接影响 AI 回复质量

    /**
     * 测试：没有任何已知信息时，返回"暂无已知用户信息"
     * 场景：新用户第一次对话
     */
    @Test
    void getKnownInfoSummary_无信息_返回暂无提示() {
        SessionContext ctx = new SessionContext();
        assertEquals("暂无已知用户信息", ctx.getKnownInfoSummary());
    }

    /**
     * 测试：有姓名和电话时，摘要中包含两者
     * 验证：AI 收到的上下文中能看到"姓名: 李四"和"电话: 13900139000"
     */
    @Test
    void getKnownInfoSummary_有姓名和电话_包含两者() {
        SessionContext ctx = new SessionContext();
        ctx.setCustomerName("李四");
        ctx.setPhone("13900139000");
        String summary = ctx.getKnownInfoSummary();
        assertTrue(summary.contains("姓名: 李四"));
        assertTrue(summary.contains("电话: 13900139000"));
    }

    /**
     * 测试：所有信息都已收集时，摘要包含全部字段
     * 验证：姓名、电话、学历、兴趣、课程、校区全部出现在摘要中
     */
    @Test
    void getKnownInfoSummary_全部信息_全部包含() {
        SessionContext ctx = new SessionContext();
        ctx.setCustomerName("王五");
        ctx.setPhone("13700137000");
        ctx.setEducation("大三");
        ctx.setInterest("UI/UX设计");
        ctx.setSelectedCourseId(3L);
        ctx.setSelectedCourseName("UI/UX设计");
        ctx.setSelectedCampusId(1L);
        ctx.setSelectedCampusName("中关村校区");
        String summary = ctx.getKnownInfoSummary();
        assertTrue(summary.contains("姓名: 王五"));
        assertTrue(summary.contains("电话: 13700137000"));
        assertTrue(summary.contains("学历: 大三"));
        assertTrue(summary.contains("兴趣: UI/UX设计"));
        assertTrue(summary.contains("已选课程: UI/UX设计"));
        assertTrue(summary.contains("已选校区: 中关村校区"));
    }

    // ==================== getMissingInfoForReservation 测试 ====================
    // 创建预约需要4项信息：姓名、电话、课程、校区，此方法检测缺失项

    /**
     * 测试：4项信息齐全时，返回空字符串
     * 场景：可以直接创建预约，不需要再询问
     */
    @Test
    void getMissingInfoForReservation_信息齐全_返回空字符串() {
        SessionContext ctx = new SessionContext();
        ctx.setCustomerName("赵六");
        ctx.setPhone("13600136000");
        ctx.setSelectedCourseId(1L);
        ctx.setSelectedCampusId(2L);
        assertEquals("", ctx.getMissingInfoForReservation());
    }

    /**
     * 测试：只缺姓名时，提示中包含"姓名"但不包含"电话"
     * 验证：只提示缺失项，已有的信息不会被重复提示
     */
    @Test
    void getMissingInfoForReservation_缺姓名_返回包含姓名() {
        SessionContext ctx = new SessionContext();
        ctx.setPhone("13600136000");
        ctx.setSelectedCourseId(1L);
        ctx.setSelectedCampusId(2L);
        String missing = ctx.getMissingInfoForReservation();
        assertTrue(missing.contains("姓名"));
        assertFalse(missing.contains("电话"));
    }

    /**
     * 测试：什么信息都没有时，提示包含全部4项
     * 场景：新用户直接说"帮我预约"，系统需要收集所有信息
     */
    @Test
    void getMissingInfoForReservation_全部缺失_包含全部() {
        SessionContext ctx = new SessionContext();
        String missing = ctx.getMissingInfoForReservation();
        assertTrue(missing.contains("姓名"));
        assertTrue(missing.contains("电话"));
        assertTrue(missing.contains("课程"));
        assertTrue(missing.contains("校区"));
    }

    // ==================== addMessage 测试 ====================
    // 对话历史用于调试和审计，但不能无限增长，否则会内存溢出

    /**
     * 测试：添加一条消息后，历史记录中能正确读取
     * 验证：角色和内容都被正确保存
     */
    @Test
    void addMessage_添加消息_记录到历史() {
        SessionContext ctx = new SessionContext();
        ctx.addMessage("用户", "你好");
        assertEquals(1, ctx.getConversationHistory().size());
        assertTrue(ctx.getConversationHistory().get(0).contains("用户"));
        assertTrue(ctx.getConversationHistory().get(0).contains("你好"));
    }

    /**
     * 测试：添加超过20条消息后，只保留最近20条
     * 防止长时间对话导致 SessionContext 对象占用过多内存
     */
    @Test
    void addMessage_超过20条_自动截断保留最近20条() {
        SessionContext ctx = new SessionContext();
        for (int i = 0; i < 25; i++) {
            ctx.addMessage("用户", "消息" + i);
        }
        assertEquals(20, ctx.getConversationHistory().size());
        // 前5条被丢弃，第一条应该是"消息5"
        assertTrue(ctx.getConversationHistory().get(0).contains("消息5"));
    }

    // ==================== touch / 构造函数测试 ====================
    // lastActiveTime 用于会话过期清理，30分钟无操作自动清除

    /**
     * 测试：构造函数应初始化创建时间和最后活跃时间
     * 验证：两个时间字段都不为 null
     */
    @Test
    void constructor_初始化时间不为null() {
        SessionContext ctx = new SessionContext();
        assertNotNull(ctx.getCreateTime());
        assertNotNull(ctx.getLastActiveTime());
    }

    /**
     * 测试：调用 touch() 后，lastActiveTime 应更新
     * 场景：每次用户发消息时调用 touch()，重置过期倒计时
     */
    @Test
    void touch_更新最后活跃时间() {
        SessionContext ctx = new SessionContext();
        LocalDateTime before = ctx.getLastActiveTime();
        ctx.touch();
        assertFalse(ctx.getLastActiveTime().isBefore(before));
    }

    // ==================== pendingUpdate / pendingCancelReason 测试 ====================
    // 修改/取消预约采用"预览-确认"两步机制，这两个字段用于暂存待确认的操作

    /**
     * 测试：pendingUpdate 的存取正常
     * 场景：AI 解析出用户想修改课程，先暂存修改数据，等用户确认后再保存
     */
    @Test
    void pendingUpdate_存取正常() {
        SessionContext ctx = new SessionContext();
        assertNull(ctx.getPendingUpdate());
        Map<String, Object> update = new HashMap<>();
        update.put("courseId", 2L);
        ctx.setPendingUpdate(update);
        assertEquals(2L, ctx.getPendingUpdate().get("courseId"));
    }

    /**
     * 测试：pendingCancelReason 的存取正常
     * 场景：用户说"不想学了要取消"，系统记录取消原因，等用户确认后再执行
     */
    @Test
    void pendingCancelReason_存取正常() {
        SessionContext ctx = new SessionContext();
        assertNull(ctx.getPendingCancelReason());
        ctx.setPendingCancelReason("不想学了");
        assertEquals("不想学了", ctx.getPendingCancelReason());
    }
}
