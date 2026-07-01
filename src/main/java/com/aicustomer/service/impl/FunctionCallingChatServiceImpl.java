package com.aicustomer.service.impl;

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

    /** 会话上下文存储 */
    private final Map<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    /** Function Calling模式的系统提示词 */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的课程咨询顾问，负责为潜在学员提供课程咨询服务。

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
            - cancelReservation: 取消已有预约
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
        SessionContext context = sessionContexts.computeIfAbsent(sessionId, k -> new SessionContext());
        context.setSessionId(sessionId);

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
        String lowerMsg = message.toLowerCase();

        // 提取兴趣
        if (!context.hasInfo("interest")) {
            if (lowerMsg.contains("编程") || lowerMsg.contains("java") || lowerMsg.contains("python")) {
                context.setInterest("编程开发");
            } else if (lowerMsg.contains("设计") || lowerMsg.contains("ui")) {
                context.setInterest("UI/UX设计");
            } else if (lowerMsg.contains("数据") || lowerMsg.contains("ai")) {
                context.setInterest("数据分析");
            }
        }

        // 提取学历
        if (!context.hasInfo("education")) {
            if (lowerMsg.contains("大一")) context.setEducation("大一");
            else if (lowerMsg.contains("大二")) context.setEducation("大二");
            else if (lowerMsg.contains("大三")) context.setEducation("大三");
            else if (lowerMsg.contains("大四")) context.setEducation("大四");
            else if (lowerMsg.contains("研究生") || lowerMsg.contains("硕士")) context.setEducation("研究生");
            else if (lowerMsg.contains("零基础")) context.setEducation("零基础");
        }

        // 提取姓名
        if (!context.hasInfo("name")) {
            java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile(
                    "(?:我叫|我是|我姓|名字是|姓名是|叫我)[\\s]*([\\u4e00-\\u9fa5]{2,4})"
            ).matcher(message);
            if (nameMatcher.find()) {
                context.setCustomerName(nameMatcher.group(1));
            }
        }

        // 提取电话
        if (!context.hasInfo("phone")) {
            java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile(
                    "(?:电话|手机|联系方式|号码)[\\s:：]*(1[3-9]\\d{9})"
            ).matcher(message);
            if (phoneMatcher.find()) {
                context.setPhone(phoneMatcher.group(1));
            }
        }

        // 提取课程选择
        if (!context.hasInfo("course")) {
            if (lowerMsg.contains("java")) {
                context.setSelectedCourseId(1L);
                context.setSelectedCourseName("Java全栈开发");
            } else if (lowerMsg.contains("python") || lowerMsg.contains("数据分析")) {
                context.setSelectedCourseId(2L);
                context.setSelectedCourseName("Python数据分析");
            } else if (lowerMsg.contains("ui") || lowerMsg.contains("ux") || lowerMsg.contains("设计")) {
                context.setSelectedCourseId(3L);
                context.setSelectedCourseName("UI/UX设计");
            } else if (lowerMsg.contains("人工智能") || lowerMsg.contains("ai")) {
                context.setSelectedCourseId(4L);
                context.setSelectedCourseName("人工智能入门");
            }
        }

        // 提取校区选择
        if (!context.hasInfo("campus")) {
            if (lowerMsg.contains("中关村")) {
                context.setSelectedCampusId(1L);
                context.setSelectedCampusName("中关村校区");
            } else if (lowerMsg.contains("国贸")) {
                context.setSelectedCampusId(2L);
                context.setSelectedCampusName("国贸校区");
            } else if (lowerMsg.contains("西直门")) {
                context.setSelectedCampusId(3L);
                context.setSelectedCampusName("西直门校区");
            }
        }

        // 保存客户信息
        if (context.hasInfo("name") && context.hasInfo("phone")) {
            Customer customer = new Customer();
            customer.setName(context.getCustomerName());
            customer.setPhone(context.getPhone());
            customer.setEducation(context.getEducation());
            customer.setInterest(context.getInterest());
            customer.setSource("在线客服-FunctionCalling");
            customerService.saveCustomer(customer);
        }
    }

    private boolean isConfirmMessage(String message) {
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("确认") || lowerMsg.contains("确定") ||
               lowerMsg.contains("没问题") || lowerMsg.contains("好的") ||
               lowerMsg.contains("可以") || lowerMsg.contains("ok");
    }

    /**
     * 后处理AI响应，提取预约ID更新上下文
     */
    private String processResponse(String responseText, SessionContext context) {
        try {
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

            reservation.setStatus(3);
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
