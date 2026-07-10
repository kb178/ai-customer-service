package com.aicustomer.service.impl;

import com.aicustomer.config.BizConstants;
import com.aicustomer.entity.Course;
import com.aicustomer.entity.Campus;
import com.aicustomer.entity.Reservation;
import com.aicustomer.entity.SessionContext;
import com.aicustomer.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatServiceImpl 单元测试
 *
 * ChatServiceImpl 是"指令解析模式"的核心服务，负责：
 * 1. 接收用户消息并提取关键信息（姓名、电话、课程等）
 * 2. 调用 AI 生成回复
 * 3. 解析 AI 回复中的指令（如 SEARCH_COURSES、CREATE_RESERVATION）
 * 4. 执行对应的数据库操作
 *
 * 注意：由于 chat() 方法依赖 ChatClient（需要连接 DeepSeek API），
 * 这里主要测试它的私有方法（通过 Java 反射调用）：
 * - isConfirmMessage：判断用户是否在确认操作
 * - extractInfoFromMessage：从消息中提取用户信息
 * - executePendingUpdate：执行待确认的预约修改
 * - executePendingCancel：执行待确认的预约取消
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private CourseService courseService;

    @Mock
    private CampusService campusService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private CustomerService customerService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private ChatClient chatClient;

    @InjectMocks
    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatClient = ChatClient.builder(chatModel).build();
    }

    // ==================== isConfirmMessage 测试 ====================
    // 此方法判断用户消息是否是"确认"操作，决定是否执行待确认的预约修改/取消

    /**
     * 测试：8种常见确认词都能正确识别
     * 用户回复这些词时，系统应执行之前暂存的预约修改/取消操作
     */
    @Test
    void isConfirmMessage_各种确认词_全部返回true() throws Exception {
        // 通过反射获取私有方法
        Method method = ChatServiceImpl.class.getDeclaredMethod("isConfirmMessage", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(chatService, "确认"));
        assertTrue((boolean) method.invoke(chatService, "确定"));
        assertTrue((boolean) method.invoke(chatService, "没问题"));
        assertTrue((boolean) method.invoke(chatService, "好的"));
        assertTrue((boolean) method.invoke(chatService, "可以"));
        assertTrue((boolean) method.invoke(chatService, "ok"));
        assertTrue((boolean) method.invoke(chatService, "对的"));
        assertTrue((boolean) method.invoke(chatService, "是的"));
    }

    /**
     * 测试：取消类和普通消息不应被识别为确认
     * 防止用户说"取消"时误触发预约保存
     */
    @Test
    void isConfirmMessage_取消类消息_返回false() throws Exception {
        Method method = ChatServiceImpl.class.getDeclaredMethod("isConfirmMessage", String.class);
        method.setAccessible(true);

        assertFalse((boolean) method.invoke(chatService, "取消"));
        assertFalse((boolean) method.invoke(chatService, "不要了"));
        assertFalse((boolean) method.invoke(chatService, "我想学Java"));
    }

    /**
     * 测试：确认词应大小写不敏感
     * "OK"和"Ok"都应被识别
     */
    @Test
    void isConfirmMessage_大小写不敏感() throws Exception {
        Method method = ChatServiceImpl.class.getDeclaredMethod("isConfirmMessage", String.class);
        method.setAccessible(true);

        assertTrue((boolean) method.invoke(chatService, "OK"));
        assertTrue((boolean) method.invoke(chatService, "Ok"));
    }

    // ==================== extractInfoFromMessage 测试 ====================
    // 此方法从用户消息中提取结构化信息，是多轮对话记忆的基础

    /**
     * 测试：从"我叫张三"中提取姓名
     * 正则匹配"我叫/我是/我姓/名字是/姓名是/叫我"后的2-4个中文字符
     */
    @Test
    void extractInfoFromMessage_提取姓名_正则匹配() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我叫张三", context);
        assertEquals("张三", context.getCustomerName());
    }

    /**
     * 测试：从"我是李四"中也能提取姓名
     * "我是"是另一种常见的自我介绍格式
     */
    @Test
    void extractInfoFromMessage_提取姓名_我是格式() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我是李四", context);
        assertEquals("李四", context.getCustomerName());
    }

    /**
     * 测试：从"我的电话是13800138000"中提取带前缀的电话
     * 支持"电话/手机/联系方式/号码/tel"前缀
     */
    @Test
    void extractInfoFromMessage_提取电话_带前缀() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我的电话是13800138000", context);
        assertEquals("13800138000", context.getPhone());
    }

    /**
     * 测试：直接输入裸手机号也能识别
     * 用户可能直接发"13800138000"而不带任何前缀
     */
    @Test
    void extractInfoFromMessage_提取电话_裸手机号() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "13800138000", context);
        assertEquals("13800138000", context.getPhone());
    }

    /**
     * 测试：从"我想学编程"中提取兴趣为"编程开发"
     * 关键词匹配："编程/java/python/前端/后端/开发" → 编程开发
     */
    @Test
    void extractInfoFromMessage_提取兴趣_编程() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我想学编程", context);
        assertEquals("编程开发", context.getInterest());
    }

    /**
     * 测试：从"对设计感兴趣"中提取兴趣为"UI/UX设计"
     */
    @Test
    void extractInfoFromMessage_提取兴趣_UI设计() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "对设计感兴趣", context);
        assertEquals("UI/UX设计", context.getInterest());
    }

    /**
     * 测试：从"我是研究生"中提取学历
     * 学历信息帮助 AI 推荐合适难度的课程
     */
    @Test
    void extractInfoFromMessage_提取学历_研究生() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我是研究生", context);
        assertEquals("研究生", context.getEducation());
    }

    /**
     * 测试：从"我零基础"中提取学历为"零基础"
     * "零基础"用户需要推荐入门级课程
     */
    @Test
    void extractInfoFromMessage_提取学历_零基础() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我零基础", context);
        assertEquals("零基础", context.getEducation());
    }

    /**
     * 测试：从"我想学java"中识别课程 ID 和名称
     * 验证硬编码的关键词映射是否正确
     */
    @Test
    void extractInfoFromMessage_识别课程_Java() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我想学java", context);
        assertEquals(BizConstants.COURSE_JAVA_ID, context.getSelectedCourseId());
        assertEquals("Java全栈开发", context.getSelectedCourseName());
    }

    /**
     * 测试：从"python数据分析怎么样"中识别 Python 课程
     */
    @Test
    void extractInfoFromMessage_识别课程_Python() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "python数据分析怎么样", context);
        assertEquals(BizConstants.COURSE_PYTHON_ID, context.getSelectedCourseId());
        assertEquals("Python数据分析", context.getSelectedCourseName());
    }

    /**
     * 测试：从"人工智能好学吗"中识别 AI 课程
     */
    @Test
    void extractInfoFromMessage_识别课程_AI() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "人工智能好学吗", context);
        assertEquals(BizConstants.COURSE_AI_ID, context.getSelectedCourseId());
        assertEquals("人工智能入门", context.getSelectedCourseName());
    }

    /**
     * 测试：从"中关村有校区吗"中识别校区 ID 和名称
     */
    @Test
    void extractInfoFromMessage_识别校区_中关村() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "中关村有校区吗", context);
        assertEquals(BizConstants.CAMPUS_ZHONGGUANCUN_ID, context.getSelectedCampusId());
        assertEquals("中关村校区", context.getSelectedCampusName());
    }

    /**
     * 测试：从"国贸校区在哪"中识别校区
     */
    @Test
    void extractInfoFromMessage_识别校区_国贸() throws Exception {
        SessionContext context = new SessionContext();
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "国贸校区在哪", context);
        assertEquals(BizConstants.CAMPUS_GUOMAO_ID, context.getSelectedCampusId());
        assertEquals("国贸校区", context.getSelectedCampusName());
    }

    /**
     * 测试：已有信息不应被新消息覆盖
     * 用户先说"我叫张三"后说"我是李四"，姓名应保持"张三"
     * 核心规则：永远不要询问用户已经提供的信息
     */
    @Test
    void extractInfoFromMessage_已有信息不再覆盖() throws Exception {
        SessionContext context = new SessionContext();
        context.setCustomerName("已有姓名");
        Method method = ChatServiceImpl.class.getDeclaredMethod(
                "extractInfoFromMessage", String.class, SessionContext.class);
        method.setAccessible(true);

        method.invoke(chatService, "我叫新名字", context);
        assertEquals("已有姓名", context.getCustomerName());
    }

    // ==================== executePendingUpdate 测试 ====================
    // 修改预约采用"预览-确认"两步机制：AI 先输出修改预览，用户确认后才真正保存

    /**
     * 测试：预约记录不存在时，返回错误提示并清除待确认数据
     * 场景：用户确认修改时，预约已被其他人取消
     */
    @Test
    void executePendingUpdate_预约不存在_返回错误() throws Exception {
        SessionContext context = new SessionContext();
        context.setReservationId(1L);
        Map<String, Object> pendingUpdate = new HashMap<>();
        pendingUpdate.put("courseId", 2L);
        context.setPendingUpdate(pendingUpdate);

        when(reservationService.getById(1L)).thenReturn(null);

        Method method = ChatServiceImpl.class.getDeclaredMethod("executePendingUpdate", SessionContext.class);
        method.setAccessible(true);

        String result = (String) method.invoke(chatService, context);
        assertTrue(result.contains("预约记录不存在"));
        assertNull(context.getPendingUpdate());
    }

    /**
     * 测试：修改课程成功
     * 验证：课程ID被更新，待确认数据被清除，返回包含新课程名的成功消息
     */
    @Test
    void executePendingUpdate_修改课程_成功() throws Exception {
        SessionContext context = new SessionContext();
        context.setReservationId(1L);
        Map<String, Object> pendingUpdate = new HashMap<>();
        pendingUpdate.put("courseId", 2L);
        context.setPendingUpdate(pendingUpdate);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setCustomerName("张三");
        reservation.setPhone("13800138000");
        reservation.setCourseId(1L);
        reservation.setCampusId(1L);

        Course newCourse = new Course();
        newCourse.setId(2L);
        newCourse.setName("Python数据分析");

        when(reservationService.getById(1L)).thenReturn(reservation);
        when(courseService.getById(2L)).thenReturn(newCourse);
        when(reservationService.updateById(any())).thenReturn(true);

        Method method = ChatServiceImpl.class.getDeclaredMethod("executePendingUpdate", SessionContext.class);
        method.setAccessible(true);

        String result = (String) method.invoke(chatService, context);
        assertTrue(result.contains("预约修改已确认"));
        assertTrue(result.contains("Python数据分析"));
        assertNull(context.getPendingUpdate());
    }

    // ==================== executePendingCancel 测试 ====================
    // 取消预约同样采用"预览-确认"两步机制

    /**
     * 测试：预约不存在时，返回错误并清除取消原因
     */
    @Test
    void executePendingCancel_预约不存在_返回错误() throws Exception {
        SessionContext context = new SessionContext();
        context.setReservationId(1L);
        context.setPendingCancelReason("不想学了");

        when(reservationService.getById(1L)).thenReturn(null);

        Method method = ChatServiceImpl.class.getDeclaredMethod("executePendingCancel", SessionContext.class);
        method.setAccessible(true);

        String result = (String) method.invoke(chatService, context);
        assertTrue(result.contains("预约记录不存在"));
        assertNull(context.getPendingCancelReason());
    }

    /**
     * 测试：取消预约成功
     * 验证：状态更新为已取消，备注包含取消原因，
     *       待确认数据和预约ID都被清除
     */
    @Test
    void executePendingCancel_取消成功() throws Exception {
        SessionContext context = new SessionContext();
        context.setReservationId(1L);
        context.setPendingCancelReason("不想学了");

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setCustomerName("张三");
        reservation.setPhone("13800138000");
        reservation.setCourseId(1L);
        reservation.setCampusId(1L);
        reservation.setStatus(BizConstants.STATUS_PENDING);

        Course course = new Course();
        course.setId(1L);
        course.setName("Java全栈开发");

        Campus campus = new Campus();
        campus.setId(1L);
        campus.setName("中关村校区");

        when(reservationService.getById(1L)).thenReturn(reservation);
        when(courseService.getById(1L)).thenReturn(course);
        when(campusService.getById(1L)).thenReturn(campus);
        when(reservationService.updateById(any())).thenReturn(true);

        Method method = ChatServiceImpl.class.getDeclaredMethod("executePendingCancel", SessionContext.class);
        method.setAccessible(true);

        String result = (String) method.invoke(chatService, context);
        assertTrue(result.contains("预约已成功取消"));
        assertTrue(result.contains("不想学了"));
        assertNull(context.getPendingCancelReason());
        assertNull(context.getReservationId());
    }
}
