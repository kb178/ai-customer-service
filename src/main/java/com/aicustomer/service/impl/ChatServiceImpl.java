package com.aicustomer.service.impl;

import com.aicustomer.config.BizConstants;
import com.aicustomer.entity.Course;
import com.aicustomer.entity.Campus;
import com.aicustomer.entity.Reservation;
import com.aicustomer.entity.Customer;
import com.aicustomer.entity.SessionContext;
import com.aicustomer.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能客服对话服务实现类
 * 
 * 功能说明：
 * - 核心业务类，整合AI对话和数据库操作
 * - 实现多轮对话记忆功能
 * - 自动提取用户信息（姓名、电话、学历等）
 * - 解析AI指令并执行相应的数据库操作
 * 
 * 主要流程：
 * 1. 接收用户消息
 * 2. 提取用户信息到会话上下文
 * 3. 构建包含上下文的系统提示词
 * 4. 调用AI模型生成回复
 * 5. 解析AI回复中的指令（如查询课程、创建预约）
 * 6. 执行相应的数据库操作
 * 7. 返回处理后的回复给用户
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /** ChatClient - 支持对话记忆 */
    private final ChatClient chatClient;
    /** 课程服务 */
    private final CourseService courseService;
    /** 校区服务 */
    private final CampusService campusService;
    /** 预约服务 */
    private final ReservationService reservationService;
    /** 客户服务 */
    private final CustomerService customerService;
    /** JSON序列化工具 */
    private final ObjectMapper objectMapper;

    /**
     * 会话上下文存储
     * 
     * key: sessionId（会话ID）
     * value: SessionContext（会话上下文）
     * 
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    /**
     * AI系统提示词
     * 
     * 定义AI的角色、任务和指令格式
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的课程咨询顾问，负责为潜在学员提供课程咨询服务。

            ## 核心规则（必须遵守）
            1. **永远不要询问用户已经提供的信息** - 查看"已知用户信息"部分，如果已有则直接使用
            2. **不要重复确认已知信息** - 例如用户已说姓名，不要再说"请问您贵姓"
            3. **预约时直接使用已知信息** - 如果用户说"帮我预约"，且信息已齐全，直接执行预约

            ## 你的主要任务
            1. 了解用户的兴趣、学历背景等信息
            2. 根据用户需求推荐合适的课程
            3. 引导用户预约试听课程
            4. 引导用户留下联系方式

            ## 预约流程
            当用户说"预约"、"报名"、"想学"等意向词时：
            - 先检查"预约状态"部分，如果已有预约，说明用户想修改预约
            - 修改预约时，只修改用户提到的内容，其他信息保持不变
            - 如果是新预约，检查是否已有足够信息（姓名、电话、课程、校区）
            - 如果信息齐全，直接输出预约指令
            - 如果信息不全，只询问缺少的信息，不要询问已有的信息

            ## 修改预约流程（非常重要！！！）
            当用户说"修改"相关的内容时：
            1. **必须确认用户想修改什么** - 如果用户只说"修改课程"但没说修改成什么课程，必须询问"请问您想修改成哪个课程？"
            2. **必须确认用户修改的内容** - 如果用户说"修改成python的课程"，需要明确识别出是哪门课程
            3. **显示修改预览** - 输出 UPDATE_RESERVATION 指令时，系统会自动显示修改前后的对比
            4. **等待用户确认** - 用户需要说"确认"、"确定"、"没问题"等确认词后才会真正保存

            ## 修改预约时JSON格式要求（极其重要！！！）
            **UPDATE_RESERVATION的JSON中只能包含用户明确要求修改的字段！！！**
            - 用户说"修改校区" → 只传campusId，不要传courseId、customerName、phone
            - 用户说"修改课程" → 只传courseId，不要传campusId、customerName、phone
            - 用户说"修改成国贸校区" → 只传{"campusId":2}，不要传其他字段
            - 用户说"修改成Python课程" → 只传{"courseId":2}，不要传其他字段
            - **绝对不要传null值或空字符串的字段，不要传用户没提到的字段**

            ## 识别课程的方法
            - "python" 或 "数据分析" → Python数据分析 (ID:2)
            - "java" 或 "全栈" 或 "开发" → Java全栈开发 (ID:1)
            - "ui" 或 "ux" 或 "设计" → UI/UX设计 (ID:3)
            - "人工智能" 或 "ai" 或 "机器学习" → 人工智能入门 (ID:4)

            ## 识别校区的方法
            - "中关村" → 中关村校区 (ID:1)
            - "国贸" → 国贸校区 (ID:2)
            - "西直门" → 西直门校区 (ID:3)

            ## 指令格式（必须严格遵守）
            当需要查询课程信息时，请回复格式：SEARCH_COURSES:关键词
            当需要查询校区信息时，请回复格式：SEARCH_CAMPUS
            当用户询问"我的预约"、"查询预约"、"预约信息"等时，请回复格式：SEARCH_RESERVATION
            当需要创建新预约时，请回复格式：CREATE_RESERVATION:{"customerName":"姓名","phone":"手机号","courseId":课程ID,"campusId":校区ID,"appointmentTime":"预约时间"}
            当需要预览修改预约时（先预览，等用户确认），请回复格式：UPDATE_RESERVATION:{"customerName":"姓名","phone":"手机号","courseId":课程ID,"campusId":校区ID,"appointmentTime":"预约时间"}
            当用户确认修改后，回复：CONFIRM_UPDATE
            当需要预览取消预约时（先询问取消原因，等用户确认），请回复格式：CANCEL_RESERVATION:{"reason":"取消原因"}
            当用户确认取消后，回复：CONFIRM_CANCEL
            当需要保存客户信息时，请回复格式：SAVE_CUSTOMER:{"name":"姓名","phone":"手机号","email":"邮箱","education":"学历","interest":"兴趣"}

            ## 注意事项
            - 预约时间格式：yyyy-MM-dd HH:mm:ss
            - courseId和campusId从课程和校区列表中获取
            - 用友好、专业的语气与用户交流
            - 修改预约时，只需包含用户想修改的字段，未提及的字段不要包含在JSON中
            """;

    /**
     * 处理对话消息（核心方法）
     * 
     * 流程：
     * 1. 获取或创建会话上下文
     * 2. 记录用户消息到历史
     * 3. 从消息中提取用户信息
     * 4. 构建系统提示词（包含上下文）
     * 5. 调用AI生成回复
     * 6. 处理AI回复中的指令
     * 7. 返回最终回复
     * 
     * @param sessionId 会话ID
     * @param message 用户消息
     * @return AI回复内容
     */
    @Override
    public String chat(String sessionId, String message) {
        // 获取或创建会话上下文
        SessionContext context = sessionContexts.computeIfAbsent(sessionId, k -> new SessionContext());
        context.setSessionId(sessionId);

        // 记录用户消息
        context.addMessage("用户", message);

        // 检查用户是否确认了预约修改
        if (context.getPendingUpdate() != null && isConfirmMessage(message)) {
            // 执行待确认的修改
            String result = executePendingUpdate(context);
            context.addMessage("助手", result);
            return result;
        }

        // 检查用户是否确认了取消预约
        if (context.getPendingCancelReason() != null && isConfirmMessage(message)) {
            // 执行待确认的取消预约
            String result = executePendingCancel(context);
            context.addMessage("助手", result);
            return result;
        }

        // 从消息中提取用户信息（姓名、电话、学历、兴趣等）
        extractInfoFromMessage(message, context);

        // 构建包含上下文的系统提示词
        String systemMessage = buildSystemMessage(context);

        // 使用ChatClient调用AI（自动管理对话历史）
        // ChatMemory会根据conversationId自动保存和读取对话历史
        String responseText = chatClient.prompt()
                .system(systemMessage)
                .user(message)
                .advisors(a -> a.param("chatMemoryConversationId", sessionId))
                .call()
                .content();
        log.info("AI响应: {}", responseText);

        // 记录AI原始回复
        context.addMessage("助手", responseText);

        // 处理AI回复中的指令（如查询课程、创建预约等）
        String processedResponse = processAiResponse(responseText, context);

        // 记录处理后的回复
        context.addMessage("助手(处理后)", processedResponse);

        return processedResponse;
    }

    /**
     * 判断用户消息是否为确认消息
     */
    private boolean isConfirmMessage(String message) {
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("确认") || lowerMsg.contains("确定") ||
               lowerMsg.contains("没问题") || lowerMsg.contains("好的") ||
               lowerMsg.contains("可以") || lowerMsg.contains("ok") ||
               lowerMsg.contains("对的") || lowerMsg.contains("是的");
    }

    /**
     * 执行待确认的预约修改
     */
    private String executePendingUpdate(SessionContext context) {
        try {
            Map<String, Object> pendingData = context.getPendingUpdate();
            Long reservationId = context.getReservationId();

            Reservation existingReservation = reservationService.getById(reservationId);
            if (existingReservation == null) {
                context.setPendingUpdate(null);
                return "预约记录不存在，请重新创建预约。";
            }

            // 更新预约数据（只更新用户明确要求修改的字段）
            if (pendingData.containsKey("customerName")) {
                existingReservation.setCustomerName((String) pendingData.get("customerName"));
                context.setCustomerName((String) pendingData.get("customerName"));
            }
            if (pendingData.containsKey("phone")) {
                existingReservation.setPhone((String) pendingData.get("phone"));
                context.setPhone((String) pendingData.get("phone"));
            }
            if (pendingData.containsKey("courseId")) {
                existingReservation.setCourseId((Long) pendingData.get("courseId"));
                context.setSelectedCourseId((Long) pendingData.get("courseId"));
                Course course = courseService.getById((Long) pendingData.get("courseId"));
                context.setSelectedCourseName(course != null ? course.getName() : null);
            }
            if (pendingData.containsKey("campusId")) {
                existingReservation.setCampusId((Long) pendingData.get("campusId"));
                context.setSelectedCampusId((Long) pendingData.get("campusId"));
                Campus campus = campusService.getById((Long) pendingData.get("campusId"));
                context.setSelectedCampusName(campus != null ? campus.getName() : null);
            }

            // 保存到数据库
            reservationService.updateById(existingReservation);

            // 清除待确认数据
            context.setPendingUpdate(null);

            // 构建成功消息
            StringBuilder successMsg = new StringBuilder();
            successMsg.append("✅ 预约修改已确认并保存！\n\n");
            successMsg.append("最终预约信息：\n");
            successMsg.append("- 姓名：").append(existingReservation.getCustomerName()).append("\n");
            successMsg.append("- 电话：").append(existingReservation.getPhone()).append("\n");
            if (existingReservation.getCourseId() != null) {
                Course course = courseService.getById(existingReservation.getCourseId());
                successMsg.append("- 课程：").append(course != null ? course.getName() : "未知").append("\n");
            }
            if (existingReservation.getCampusId() != null) {
                Campus campus = campusService.getById(existingReservation.getCampusId());
                successMsg.append("- 校区：").append(campus != null ? campus.getName() : "未知").append("\n");
            }

            return successMsg.toString();
        } catch (Exception e) {
            log.error("执行预约修改失败", e);
            context.setPendingUpdate(null);
            return "保存修改时出现错误，请稍后重试。";
        }
    }

    /**
     * 执行待确认的取消预约操作
     *
     * @param context 会话上下文
     * @return 执行结果消息
     */
    private String executePendingCancel(SessionContext context) {
        try {
            String reason = context.getPendingCancelReason();
            Long reservationId = context.getReservationId();

            Reservation existingReservation = reservationService.getById(reservationId);
            if (existingReservation == null) {
                context.setPendingCancelReason(null);
                return "预约记录不存在，请重新创建预约。";
            }

            // 更新预约状态为已取消（3），并保存取消原因
            existingReservation.setStatus(BizConstants.STATUS_CANCELLED);
            existingReservation.setRemark("取消原因：" + reason);
            reservationService.updateById(existingReservation);

            // 清除待确认数据和预约ID
            context.setPendingCancelReason(null);
            context.setReservationId(null);

            // 构建成功消息
            StringBuilder successMsg = new StringBuilder();
            successMsg.append("✅ 预约已成功取消！\n\n");
            successMsg.append("已取消的预约信息：\n");
            successMsg.append("- 姓名：").append(existingReservation.getCustomerName()).append("\n");
            successMsg.append("- 电话：").append(existingReservation.getPhone()).append("\n");
            if (existingReservation.getCourseId() != null) {
                Course course = courseService.getById(existingReservation.getCourseId());
                successMsg.append("- 课程：").append(course != null ? course.getName() : "未知").append("\n");
            }
            if (existingReservation.getCampusId() != null) {
                Campus campus = campusService.getById(existingReservation.getCampusId());
                successMsg.append("- 校区：").append(campus != null ? campus.getName() : "未知").append("\n");
            }
            successMsg.append("- 取消原因：").append(reason).append("\n");
            successMsg.append("\n如需重新预约，随时告诉我。");

            return successMsg.toString();
        } catch (Exception e) {
            log.error("执行取消预约失败", e);
            context.setPendingCancelReason(null);
            return "取消预约时出现错误，请稍后重试。";
        }
    }

    /**
     * 从用户消息中提取信息
     * 
     * 功能：使用正则表达式和关键词匹配，自动提取以下信息：
     * - 兴趣（编程、设计、数据分析等）
     * - 学历（大一、研究生、零基础等）
     * - 姓名（我叫xxx、我是xxx）
     * - 电话（11位手机号）
     * - 课程选择（Java、Python、UI设计、AI）
     * - 校区选择（中关村、国贸、西直门）
     * 
     * @param message 用户消息
     * @param context 会话上下文
     */
    private void extractInfoFromMessage(String message, SessionContext context) {
        String lowerMsg = message.toLowerCase();

        // 提取兴趣信息
        if (!context.hasInfo("interest")) {
            if (lowerMsg.contains("编程") || lowerMsg.contains("java") || lowerMsg.contains("python") ||
                lowerMsg.contains("前端") || lowerMsg.contains("后端") || lowerMsg.contains("开发")) {
                context.setInterest("编程开发");
            } else if (lowerMsg.contains("设计") || lowerMsg.contains("ui") || lowerMsg.contains("ux")) {
                context.setInterest("UI/UX设计");
            } else if (lowerMsg.contains("数据") || lowerMsg.contains("分析") || lowerMsg.contains("ai")) {
                context.setInterest("数据分析");
            }
        }

        // 提取学历信息
        if (!context.hasInfo("education")) {
            if (lowerMsg.contains("大一") || lowerMsg.contains("大一新生")) {
                context.setEducation("大一");
            } else if (lowerMsg.contains("大二")) {
                context.setEducation("大二");
            } else if (lowerMsg.contains("大三")) {
                context.setEducation("大三");
            } else if (lowerMsg.contains("大四")) {
                context.setEducation("大四");
            } else if (lowerMsg.contains("研究生") || lowerMsg.contains("硕士")) {
                context.setEducation("研究生");
            } else if (lowerMsg.contains("博士")) {
                context.setEducation("博士");
            } else if (lowerMsg.contains("高中") || lowerMsg.contains("高三")) {
                context.setEducation("高中");
            } else if (lowerMsg.contains("零基础") || lowerMsg.contains("没有基础")) {
                context.setEducation("零基础");
            }
        }

        // 提取姓名（正则匹配：我叫xxx、我是xxx等）
        if (!context.hasInfo("name")) {
            java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile(
                    "(?:我叫|我是|我姓|名字是|姓名是|叫我)[\\s]*([\\u4e00-\\u9fa5]{2,4})"
            ).matcher(message);
            if (nameMatcher.find()) {
                context.setCustomerName(nameMatcher.group(1));
            }
        }

        // 提取电话（正则匹配11位手机号）
        if (!context.hasInfo("phone")) {
            java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile(
                    "(?:电话|手机|联系方式|号码|tel)[\\s:：]*((?:1[3-9]\\d{9}|\\d{3,4}-?\\d{7,8}))|(?:1[3-9]\\d{9})"
            ).matcher(message);
            if (phoneMatcher.find()) {
                String phone = phoneMatcher.group(1) != null ? phoneMatcher.group(1) : phoneMatcher.group(0);
                context.setPhone(phone.replaceAll("[^0-9]", ""));
            }
        }

        // 当用户提供了姓名和电话时，自动保存客户信息
        if (context.hasInfo("name") && context.hasInfo("phone")) {
            Customer customer = new Customer();
            customer.setName(context.getCustomerName());
            customer.setPhone(context.getPhone());
            customer.setEducation(context.getEducation());
            customer.setInterest(context.getInterest());
            customer.setSource(BizConstants.SOURCE_INSTRUCTION);
            customerService.saveCustomer(customer);
        }

        // 识别课程选择
        if (!context.hasInfo("course")) {
            if (lowerMsg.contains("java") || lowerMsg.contains("java全栈")) {
                context.setSelectedCourseId(BizConstants.COURSE_JAVA_ID);
                context.setSelectedCourseName("Java全栈开发");
            } else if (lowerMsg.contains("python") || lowerMsg.contains("数据分析")) {
                context.setSelectedCourseId(BizConstants.COURSE_PYTHON_ID);
                context.setSelectedCourseName("Python数据分析");
            } else if (lowerMsg.contains("ui") || lowerMsg.contains("ux") || lowerMsg.contains("设计")) {
                context.setSelectedCourseId(BizConstants.COURSE_UI_ID);
                context.setSelectedCourseName("UI/UX设计");
            } else if (lowerMsg.contains("人工智能") || lowerMsg.contains("ai") || lowerMsg.contains("机器学习")) {
                context.setSelectedCourseId(BizConstants.COURSE_AI_ID);
                context.setSelectedCourseName("人工智能入门");
            }
        }

        // 识别校区选择
        if (!context.hasInfo("campus")) {
            if (lowerMsg.contains("中关村")) {
                context.setSelectedCampusId(BizConstants.CAMPUS_ZHONGGUANCUN_ID);
                context.setSelectedCampusName("中关村校区");
            } else if (lowerMsg.contains("国贸")) {
                context.setSelectedCampusId(BizConstants.CAMPUS_GUOMAO_ID);
                context.setSelectedCampusName("国贸校区");
            } else if (lowerMsg.contains("西直门")) {
                context.setSelectedCampusId(BizConstants.CAMPUS_XIZHIMEN_ID);
                context.setSelectedCampusName("西直门校区");
            }
        }
    }

    /**
     * 构建系统提示词
     * 
     * 功能：将基础提示词与动态信息结合，包括：
     * - 基础角色和任务定义
     * - 已知用户信息
     * - 预约状态
     * - 缺失信息提示
     * - 可选课程列表
     * - 校区信息列表
     * 
     * @param context 会话上下文
     * @return 完整的系统提示词
     */
    private String buildSystemMessage(SessionContext context) {
        // 查询所有可用课程和校区
        List<Course> courses = courseService.searchCourses(null, null);
        List<Campus> campuses = campusService.getAllCampuses();

        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        // 添加已知用户信息
        sb.append("\n\n## ").append(context.getKnownInfoSummary()).append("\n");

        // 添加预约状态（如果有）
        if (context.getReservationId() != null) {
            sb.append("\n## 预约状态：已有预约（ID: ").append(context.getReservationId()).append("）\n");
            sb.append("用户如需修改预约，请使用 UPDATE_RESERVATION 指令\n");
        }

        // 添加缺失信息提示
        String missingInfo = context.getMissingInfoForReservation();
        if (!missingInfo.isEmpty()) {
            sb.append("\n## ").append(missingInfo).append("\n");
        }

        // 添加可选课程列表
        sb.append("\n## 当前可选课程：\n");
        for (Course course : courses) {
            sb.append(String.format("- ID:%d %s (%s) 价格:%.0f元\n",
                    course.getId(), course.getName(), course.getDescription(), course.getPrice()));
        }

        // 添加校区信息列表
        sb.append("\n## 当前校区信息：\n");
        for (Campus campus : campuses) {
            sb.append(String.format("- ID:%d %s 地址:%s 电话:%s\n",
                    campus.getId(), campus.getName(), campus.getAddress(), campus.getPhone()));
        }

        return sb.toString();
    }

    /**
     * 构建用户消息
     *
     * 功能：返回原始消息
     * 注意：对话历史现在由ChatMemory自动管理，不再需要手动拼接
     *
     * @param message 当前用户消息
     * @param context 会话上下文
     * @return 用户消息
     */
    private String buildUserMessageWithContext(String message, SessionContext context) {
        // ChatMemory会自动管理对话历史，直接返回原始消息
        return message;
    }

    /**
     * 处理AI回复中的指令
     * 
     * 功能：解析AI回复文本，识别并执行以下指令：
     * - SEARCH_COURSES: 查询课程
     * - SEARCH_CAMPUS: 查询校区
     * - CREATE_RESERVATION: 创建预约
     * - UPDATE_RESERVATION: 修改预约
     * - SAVE_CUSTOMER: 保存客户信息
     * 
     * @param responseText AI原始回复
     * @param context 会话上下文
     * @return 处理后的回复文本
     */
    private String processAiResponse(String responseText, SessionContext context) {
        // 处理查询课程指令
        if (responseText.contains("SEARCH_COURSES:")) {
            String keyword = responseText.split("SEARCH_COURSES:")[1].trim();
            List<Course> courses = courseService.searchCourses(keyword, null);
            StringBuilder result = new StringBuilder();
            result.append("为您找到以下相关课程：\n");
            for (Course course : courses) {
                result.append(String.format("- %s: %s，价格%.0f元\n",
                        course.getName(), course.getDescription(), course.getPrice()));
            }
            return result.toString();
        }

        // 处理查询校区指令
        if (responseText.contains("SEARCH_CAMPUS")) {
            List<Campus> campuses = campusService.getAllCampuses();
            StringBuilder result = new StringBuilder();
            result.append("以下是我们的校区信息：\n");
            for (Campus campus : campuses) {
                result.append(String.format("- %s: %s，电话：%s，营业时间：%s\n",
                        campus.getName(), campus.getAddress(), campus.getPhone(), campus.getBusinessHours()));
            }
            return result.toString();
        }

        // 处理查询预约指令
        if (responseText.contains("SEARCH_RESERVATION")) {
            Long reservationId = context.getReservationId();
            if (reservationId == null) {
                return "您目前没有预约记录。如需预约课程，请告诉我您感兴趣的课程。";
            }

            Reservation reservation = reservationService.getById(reservationId);
            if (reservation == null) {
                context.setReservationId(null);
                return "预约记录不存在，可能已被取消。";
            }

            Course course = reservation.getCourseId() != null ? courseService.getById(reservation.getCourseId()) : null;
            Campus campus = reservation.getCampusId() != null ? campusService.getById(reservation.getCampusId()) : null;

            String statusText = switch (reservation.getStatus()) {
                case BizConstants.STATUS_PENDING -> "待确认";
                case BizConstants.STATUS_CONFIRMED -> "已确认";
                case BizConstants.STATUS_COMPLETED -> "已完成";
                case BizConstants.STATUS_CANCELLED -> "已取消";
                default -> "未知";
            };

            StringBuilder result = new StringBuilder();
            result.append("📋 您的预约信息：\n\n");
            result.append("- 姓名：").append(reservation.getCustomerName()).append("\n");
            result.append("- 电话：").append(reservation.getPhone()).append("\n");
            result.append("- 课程：").append(course != null ? course.getName() : "未知").append("\n");
            result.append("- 校区：").append(campus != null ? campus.getName() : "未知").append("\n");
            result.append("- 状态：").append(statusText).append("\n");
            if (reservation.getAppointmentTime() != null) {
                result.append("- 预约时间：").append(reservation.getAppointmentTime()).append("\n");
            }
            if (reservation.getRemark() != null && !reservation.getRemark().isEmpty()) {
                result.append("- 备注：").append(reservation.getRemark()).append("\n");
            }
            result.append("\n如需修改或取消预约，请告诉我。");

            return result.toString();
        }

        // 处理创建预约指令
        if (responseText.contains("CREATE_RESERVATION:")) {
            try {
                // 解析JSON参数
                String jsonStr = responseText.split("CREATE_RESERVATION:")[1].trim();
                if (jsonStr.contains("}")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("}") + 1);
                }

                JsonNode jsonNode = objectMapper.readTree(jsonStr);

                // 创建预约对象，优先使用JSON中的值，其次使用上下文中的值
                Reservation reservation = new Reservation();

                String name = jsonNode.has("customerName") ? jsonNode.get("customerName").asText() : context.getCustomerName();
                String phone = jsonNode.has("phone") ? jsonNode.get("phone").asText() : context.getPhone();
                Long courseId = jsonNode.has("courseId") ? jsonNode.get("courseId").asLong() : context.getSelectedCourseId();
                Long campusId = jsonNode.has("campusId") ? jsonNode.get("campusId").asLong() : context.getSelectedCampusId();

                // 设置默认值
                if (name == null || name.isEmpty()) name = "未知用户";
                if (phone == null || phone.isEmpty()) phone = "未提供";

                reservation.setCustomerName(name);
                reservation.setPhone(phone);
                reservation.setCourseId(courseId);
                reservation.setCampusId(campusId);

                // 设置预约时间（默认明天上午10点）
                String timeStr = jsonNode.has("appointmentTime") ? jsonNode.get("appointmentTime").asText() : null;
                if (timeStr != null && !timeStr.isEmpty()) {
                    reservation.setAppointmentTime(LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else {
                    reservation.setAppointmentTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
                }

                // 保存预约
                reservationService.createReservation(reservation);

                // 获取新创建的预约ID，保存到上下文以便后续修改
                Reservation savedReservation = reservationService.lambdaQuery()
                        .eq(Reservation::getPhone, phone)
                        .orderByDesc(Reservation::getCreateTime)
                        .last("LIMIT 1")
                        .one();
                if (savedReservation != null) {
                    context.setReservationId(savedReservation.getId());
                }

                // 同时保存客户信息到客户表
                if (name != null && !name.equals("未知用户") && phone != null && !phone.equals("未提供")) {
                    Customer customer = new Customer();
                    customer.setName(name);
                    customer.setPhone(phone);
                    customer.setEducation(context.getEducation());
                    customer.setInterest(context.getInterest());
                    customer.setSource(BizConstants.SOURCE_RESERVATION);
                    customerService.saveCustomer(customer);
                }

                // 构建成功消息
                StringBuilder successMsg = new StringBuilder();
                successMsg.append("✅ 预约成功！\n\n");
                successMsg.append("预约信息：\n");
                successMsg.append("- 姓名：").append(name).append("\n");
                successMsg.append("- 电话：").append(phone).append("\n");
                if (courseId != null) {
                    successMsg.append("- 课程：").append(context.getSelectedCourseName() != null ? context.getSelectedCourseName() : "课程ID:" + courseId).append("\n");
                }
                if (campusId != null) {
                    successMsg.append("- 校区：").append(context.getSelectedCampusName() != null ? context.getSelectedCampusName() : "校区ID:" + campusId).append("\n");
                }
                successMsg.append("\n如需修改预约，请直接告诉我修改内容即可。");

                return successMsg.toString();
            } catch (Exception e) {
                log.error("创建预约失败", e);
                return "预约过程中出现错误，请稍后重试或直接致电我们的客服热线。";
            }
        }

        // 处理修改预约指令（先预览，不直接保存）
        if (responseText.contains("UPDATE_RESERVATION:")) {
            try {
                // 解析JSON参数
                String jsonStr = responseText.split("UPDATE_RESERVATION:")[1].trim();
                if (jsonStr.contains("}")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("}") + 1);
                }

                JsonNode jsonNode = objectMapper.readTree(jsonStr);

                // 检查是否有可修改的预约
                Long reservationId = context.getReservationId();
                if (reservationId == null) {
                    return "您还没有预约记录，请先创建预约。";
                }

                // 查询已有预约
                Reservation existingReservation = reservationService.getById(reservationId);
                if (existingReservation == null) {
                    context.setReservationId(null);
                    return "预约记录不存在，请重新创建预约。";
                }

                // 构建待确认的修改数据
                Map<String, Object> pendingUpdate = new java.util.HashMap<>();

                // 构建修改前的信息
                StringBuilder previewMsg = new StringBuilder();
                previewMsg.append("📋 修改预览：\n\n");
                previewMsg.append("【修改前】\n");
                previewMsg.append("- 姓名：").append(existingReservation.getCustomerName()).append("\n");
                previewMsg.append("- 电话：").append(existingReservation.getPhone()).append("\n");
                if (existingReservation.getCourseId() != null) {
                    Course oldCourse = courseService.getById(existingReservation.getCourseId());
                    previewMsg.append("- 课程：").append(oldCourse != null ? oldCourse.getName() : "未知").append("\n");
                }
                if (existingReservation.getCampusId() != null) {
                    Campus oldCampus = campusService.getById(existingReservation.getCampusId());
                    previewMsg.append("- 校区：").append(oldCampus != null ? oldCampus.getName() : "未知").append("\n");
                }

                previewMsg.append("\n【修改后】\n");

                // 计算修改后的值
                String newName = existingReservation.getCustomerName();
                String newPhone = existingReservation.getPhone();
                Long newCourseId = existingReservation.getCourseId();
                Long newCampusId = existingReservation.getCampusId();

                if (jsonNode.has("customerName") && !jsonNode.get("customerName").asText().isEmpty()) {
                    newName = jsonNode.get("customerName").asText();
                    pendingUpdate.put("customerName", newName);
                }
                if (jsonNode.has("phone") && !jsonNode.get("phone").asText().isEmpty()) {
                    newPhone = jsonNode.get("phone").asText();
                    pendingUpdate.put("phone", newPhone);
                }
                if (jsonNode.has("courseId") && !jsonNode.get("courseId").isNull()) {
                    newCourseId = jsonNode.get("courseId").asLong();
                    pendingUpdate.put("courseId", newCourseId);
                }
                if (jsonNode.has("campusId") && !jsonNode.get("campusId").isNull()) {
                    newCampusId = jsonNode.get("campusId").asLong();
                    pendingUpdate.put("campusId", newCampusId);
                }

                previewMsg.append("- 姓名：").append(newName).append("\n");
                previewMsg.append("- 电话：").append(newPhone).append("\n");
                if (newCourseId != null) {
                    Course newCourse = courseService.getById(newCourseId);
                    previewMsg.append("- 课程：").append(newCourse != null ? newCourse.getName() : "未知").append("\n");
                }
                if (newCampusId != null) {
                    Campus newCampus = campusService.getById(newCampusId);
                    previewMsg.append("- 校区：").append(newCampus != null ? newCampus.getName() : "未知").append("\n");
                }

                // 保存待确认数据到上下文
                context.setPendingUpdate(pendingUpdate);

                previewMsg.append("\n请确认是否保存以上修改？回复【确认】保存，或继续修改其他内容。");

                return previewMsg.toString();
            } catch (Exception e) {
                log.error("修改预约预览失败", e);
                return "修改预约时出现错误，请稍后重试。";
            }
        }

        // 处理取消预约指令（先预览，不直接保存）
        if (responseText.contains("CANCEL_RESERVATION:")) {
            try {
                // 检查是否有可取消的预约
                Long reservationId = context.getReservationId();
                if (reservationId == null) {
                    return "您还没有预约记录，无法取消。";
                }

                // 查询已有预约
                Reservation existingReservation = reservationService.getById(reservationId);
                if (existingReservation == null) {
                    context.setReservationId(null);
                    return "预约记录不存在，请重新创建预约。";
                }

                // 解析取消原因
                String jsonStr = responseText.split("CANCEL_RESERVATION:")[1].trim();
                if (jsonStr.contains("}")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("}") + 1);
                }
                JsonNode jsonNode = objectMapper.readTree(jsonStr);
                String reason = jsonNode.has("reason") ? jsonNode.get("reason").asText() : "用户主动取消";

                // 保存取消原因到上下文（待确认）
                context.setPendingCancelReason(reason);

                // 构建取消预览
                StringBuilder previewMsg = new StringBuilder();
                previewMsg.append("⚠️ 取消预约确认：\n\n");
                previewMsg.append("【即将取消的预约信息】\n");
                previewMsg.append("- 姓名：").append(existingReservation.getCustomerName()).append("\n");
                previewMsg.append("- 电话：").append(existingReservation.getPhone()).append("\n");
                if (existingReservation.getCourseId() != null) {
                    Course course = courseService.getById(existingReservation.getCourseId());
                    previewMsg.append("- 课程：").append(course != null ? course.getName() : "未知").append("\n");
                }
                if (existingReservation.getCampusId() != null) {
                    Campus campus = campusService.getById(existingReservation.getCampusId());
                    previewMsg.append("- 校区：").append(campus != null ? campus.getName() : "未知").append("\n");
                }
                previewMsg.append("- 取消原因：").append(reason).append("\n");
                previewMsg.append("\n请确认是否取消以上预约？回复【确认】取消，或回复其他内容保留预约。");

                return previewMsg.toString();
            } catch (Exception e) {
                log.error("取消预约预览失败", e);
                return "取消预约时出现错误，请稍后重试。";
            }
        }

        // 处理保存客户信息指令
        if (responseText.contains("SAVE_CUSTOMER:")) {
            try {
                // 解析JSON参数
                String jsonStr = responseText.split("SAVE_CUSTOMER:")[1].trim();
                if (jsonStr.contains("}")) {
                    jsonStr = jsonStr.substring(0, jsonStr.indexOf("}") + 1);
                }

                JsonNode jsonNode = objectMapper.readTree(jsonStr);

                // 创建客户对象
                Customer customer = new Customer();
                customer.setName(jsonNode.has("name") ? jsonNode.get("name").asText() : context.getCustomerName());
                customer.setPhone(jsonNode.has("phone") ? jsonNode.get("phone").asText() : context.getPhone());
                customer.setEmail(jsonNode.has("email") ? jsonNode.get("email").asText() : null);
                customer.setEducation(jsonNode.has("education") ? jsonNode.get("education").asText() : context.getEducation());
                customer.setInterest(jsonNode.has("interest") ? jsonNode.get("interest").asText() : context.getInterest());
                customer.setSource(BizConstants.SOURCE_INSTRUCTION);

                // 保存客户信息（新增或更新）
                customerService.saveCustomer(customer);

                // 更新上下文中的信息
                if (jsonNode.has("name")) context.setCustomerName(jsonNode.get("name").asText());
                if (jsonNode.has("phone")) context.setPhone(jsonNode.get("phone").asText());
                if (jsonNode.has("education")) context.setEducation(jsonNode.get("education").asText());
                if (jsonNode.has("interest")) context.setInterest(jsonNode.get("interest").asText());

                return "感谢您提供信息！我们会根据您的需求为您推荐最合适的课程。";
            } catch (Exception e) {
                log.error("保存客户信息失败", e);
                return "保存信息时出现错误，请稍后重试。";
            }
        }

        // 没有特殊指令，直接返回AI回复
        return responseText;
    }
}
