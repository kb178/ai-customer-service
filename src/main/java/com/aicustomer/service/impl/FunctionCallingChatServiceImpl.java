package com.aicustomer.service.impl;

import com.aicustomer.annotation.IntentMatcher;
import com.aicustomer.config.BizConstants;
import com.aicustomer.entity.Course;
import com.aicustomer.entity.Campus;
import com.aicustomer.entity.Customer;
import com.aicustomer.entity.Reservation;
import com.aicustomer.entity.SessionContext;
import com.aicustomer.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Function Calling模式的对话服务实现
 *
 * 功能说明：
 * - 使用Spring AI的Function Calling能力
 * - AI自动决定调用哪个函数
 * - 不需要手动解析指令
 */
@Slf4j
@Service("functionCallingChatService")
@RequiredArgsConstructor
public class FunctionCallingChatServiceImpl implements FunctionCallingChatService {

    private final ChatModel chatModel;
    private final ChatMemory chatMemory;
    private final CourseService courseService;
    private final CampusService campusService;
    private final ReservationService reservationService;
    private final CustomerService customerService;
    private final IntentMatcher intentMatcher;

    /** 会话上下文存储 */
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    /** Function Calling模式的系统提示词 */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的课程咨询顾问，负责为潜在学员提供课程咨询服务。

            ## 最重要的规则：必须调用函数
            **当你需要执行任何操作时，必须调用对应的函数，绝对不能只回复文字！**
            - 用户要创建预约 → 必须调用 createReservation
            - 用户要取消预约 → 必须调用 cancelReservation
            - 用户要修改预约 → 必须调用 updateReservation
            - 用户要查询预约 → 必须调用 queryReservation
            - 用户要查课程 → 必须调用 searchCourses
            - 用户要查校区 → 必须调用 getCampuses
            **只回复文字而不调用函数 = 严重错误！**

            ## 核心规则
            1. **永远不要询问用户已经提供的信息** - 如果已知用户信息，直接使用
            2. **不要重复确认已知信息**
            3. **预约时直接使用已知信息**

            ## 你的主要任务
            1. 了解用户的兴趣、学历背景等信息
            2. 根据用户需求推荐合适的课程
            3. 引导用户预约试听课程
            4. 引导用户留下联系方式

            ## 可用函数

            ### 地区相关
            - getProvinces: 获取有校区的省份列表
            - getCities: 获取指定省份下有校区的城市（参数：provinceId）
            - getCampuses: 获取校区列表（可按provinceId、cityId、courseId筛选）
              - 用户说"我在上海" → 用cityId筛选
              - 用户说"我想学Python，哪里有" → 用courseId筛选

            ### 课程相关
            - getCategories: 获取所有课程分类
            - searchCourses: 搜索课程（可按keyword和categoryId筛选）
            - getCampusCourses: 获取某个校区开设的课程（参数：campusId）
            - getCourseSchedules: 获取校区课程的时间安排（参数：campusId、courseId）

            ### 预约相关
            - createReservation: 创建课程预约（参数：customerName、phone、courseId、campusId、scheduleId可选）
            - updateReservation: 修改已有预约
            - cancelReservation: 取消已有预约（参数：reservationId、reason）
            - queryReservation: 查询预约信息（可按reservationId或phone查询）

            ## 交互流程
            1. 用户咨询课程 → getCategories → searchCourses
            2. 用户询问校区 → getProvinces → getCities → getCampuses
            3. 用户问"这个校区有Python课吗" → getCampusCourses
            4. 用户问"什么时候上课" → getCourseSchedules
            5. 创建预约 → createReservation
            6. 修改/取消 → queryReservation → updateReservation/cancelReservation

            ## 重要提示
            - 每个校区开设的课程不同，如果校区没开设某课程，要提示用户其他校区
            - 课程有容量限制，满员时要提示
            - 修改/取消预约前先查询
            """;

    @Override
    public String chat(String sessionId, String message) {
        //如果sessionId存在返回对应的SessionContext，不存在就创建一个新的SessionContext
        SessionContext context = sessionContexts.computeIfAbsent(sessionId, k -> new SessionContext());
        //新建的赋值，已存在的覆盖
        context.setSessionId(sessionId);
        //保存用户消息
        context.addMessage("用户", message);

        // 检查用户是否确认了预约修改/取消
        if (context.getPendingUpdate() != null && isConfirmMessage(message)) {
            String result = executePendingUpdate(context);
            context.addMessage("助手", result);
            return result;
        }
        if (context.getPendingCancelReason() != null && isConfirmMessage(message)) {
            String result = executePendingCancel(context);
            context.addMessage("助手", result);
            return result;
        }

        // 提取用户信息
        extractInfoFromMessage(message, context);

        // 构建系统提示词（包含已知信息）
        String systemMessage = buildSystemMessage(context);

        // 使用Function Calling调用AI
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultFunctions(
                    // 课程相关
                    "searchCourses", "getCategories", "getCampusCourses", "getCourseSchedules",
                    // 地区相关
                    "getProvinces", "getCities", "getCampuses",
                    // 预约相关
                    "createReservation", "updateReservation", "cancelReservation", "queryReservation"
                )
                .build();

        String responseText = chatClient.prompt()
                .system(systemMessage)
                .user(message)
                .advisors(a -> a.param("chatMemoryConversationId", sessionId))
                .call()
                .content();

        log.info("Function Calling AI响应: {}", responseText);

        // 后处理响应 - 提取预约ID更新上下文
        String processedResponse = processResponse(responseText, context);

        context.addMessage("助手", processedResponse);
        return processedResponse;
    }

    private String buildSystemMessage(SessionContext context) {
        StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

        // 添加已知用户信息
        sb.append("\n\n").append(context.getKnownInfoSummary());

        // 添加预约状态
        if (context.getReservationId() != null) {
            sb.append("\n## 当前预约状态\n");
            sb.append("用户已有预约（ID: ").append(context.getReservationId()).append("）\n");
            sb.append("如果用户想修改或取消预约，请使用相应函数，并传入该预约ID\n");
        } else {
            sb.append("\n## 当前预约状态\n");
            sb.append("用户暂无预约\n");
        }

        return sb.toString();
    }

    private void extractInfoFromMessage(String message, SessionContext context) {
        // 使用注解驱动的实体提取
        Map<String, String> entities = intentMatcher.extractEntities(message);

        if (!context.hasInfo("interest") && entities.containsKey("interest")) {
            context.setInterest(entities.get("interest"));
        }
        if (!context.hasInfo("education") && entities.containsKey("education")) {
            context.setEducation(entities.get("education"));
        }

        // 姓名提取（正则，注解不易表达）
        if (!context.hasInfo("name")) {
            java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile(
                    "(?:我叫|我是|我姓|名字是|姓名是|叫我)[\\s]*([\\u4e00-\\u9fa5]{2,4})"
            ).matcher(message);
            if (nameMatcher.find()) {
                context.setCustomerName(nameMatcher.group(1));
            }
        }

        // 电话提取（正则）
        if (!context.hasInfo("phone")) {
            java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile(
                    "(?:电话|手机|联系方式|号码)[\\s:：]*(1[3-9]\\d{9})"
            ).matcher(message);
            if (phoneMatcher.find()) {
                context.setPhone(phoneMatcher.group(1));
            }
        }

        // 课程：从数据库动态匹配
        if (!context.hasInfo("course") && entities.containsKey("course")) {
            com.aicustomer.entity.Course matched = intentMatcher.lookupCourse(entities.get("course"));
            if (matched != null) {
                context.setSelectedCourseId(matched.getId());
                context.setSelectedCourseName(matched.getName());
            }
        }

        // 校区：从数据库动态匹配
        if (!context.hasInfo("campus") && entities.containsKey("campus")) {
            com.aicustomer.entity.Campus matched = intentMatcher.lookupCampus(entities.get("campus"));
            if (matched != null) {
                context.setSelectedCampusId(matched.getId());
                context.setSelectedCampusName(matched.getName());
            }
        }

        // 保存客户信息
        if (context.hasInfo("name") && context.hasInfo("phone")) {
            Customer customer = new Customer();
            customer.setName(context.getCustomerName());
            customer.setPhone(context.getPhone());
            customer.setEducation(context.getEducation());
            customer.setInterest(context.getInterest());
            customer.setSource(BizConstants.SOURCE_FUNCTION_CALLING);
            customerService.saveCustomer(customer);
        }
    }

    private boolean isConfirmMessage(String message) {
        return intentMatcher.matchIntent(message, "CONFIRM");
    }

    /**
     * 后处理AI响应，提取预约ID更新上下文
     */
    private String processResponse(String responseText, SessionContext context) {
        try {
            // 取消成功后清理上下文中的预约ID
            if (responseText.contains("已取消") || responseText.contains("已成功取消")) {
                context.setReservationId(null);
            }

            // Function Calling模式下，AI返回的是自然语言
            // 我们需要从响应中判断是否创建/修改了预约
            if (responseText.contains("预约成功") && context.getReservationId() == null) {
                // 尝试查询最近的预约
                if (context.getPhone() != null) {
                    Reservation reservation = reservationService.lambdaQuery()
                            .eq(Reservation::getPhone, context.getPhone())
                            .orderByDesc(Reservation::getCreateTime)
                            .last("LIMIT 1")
                            .one();
                    if (reservation != null) {
                        context.setReservationId(reservation.getId());
                        log.info("Function Calling - 自动关联预约ID: {}", reservation.getId());
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理响应失败", e);
        }
        return responseText;
    }

    private String executePendingUpdate(SessionContext context) {
        try {
            Long reservationId = context.getReservationId();
            Reservation reservation = reservationService.getById(reservationId);
            if (reservation == null) {
                context.setPendingUpdate(null);
                return "预约记录不存在";
            }

            Map<String, Object> pendingData = context.getPendingUpdate();
            if (pendingData.containsKey("courseId")) {
                reservation.setCourseId((Long) pendingData.get("courseId"));
            }
            if (pendingData.containsKey("campusId")) {
                reservation.setCampusId((Long) pendingData.get("campusId"));
            }

            reservationService.updateById(reservation);
            context.setPendingUpdate(null);

            Course course = courseService.getById(reservation.getCourseId());
            Campus campus = campusService.getById(reservation.getCampusId());

            return String.format("预约修改成功！\n课程：%s\n校区：%s",
                    course != null ? course.getName() : "未知",
                    campus != null ? campus.getName() : "未知");
        } catch (Exception e) {
            context.setPendingUpdate(null);
            return "修改失败，请稍后重试";
        }
    }

    private String executePendingCancel(SessionContext context) {
        try {
            Long reservationId = context.getReservationId();
            Reservation reservation = reservationService.getById(reservationId);
            if (reservation == null) {
                context.setPendingCancelReason(null);
                return "预约记录不存在";
            }

            reservation.setStatus(BizConstants.STATUS_CANCELLED);
            reservation.setRemark("取消原因：" + context.getPendingCancelReason());
            reservationService.updateById(reservation);

            context.setPendingCancelReason(null);
            context.setReservationId(null);

            return "预约已成功取消";
        } catch (Exception e) {
            context.setPendingCancelReason(null);
            return "取消失败，请稍后重试";
        }
    }
}
