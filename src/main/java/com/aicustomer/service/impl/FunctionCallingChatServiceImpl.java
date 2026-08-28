package com.aicustomer.service.impl;

import com.aicustomer.annotation.IntentMatcher;
import com.aicustomer.config.BizConstants;
import com.aicustomer.config.SessionContextService;
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

import com.aicustomer.config.SessionContextHolder;

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
    private final SessionContextService sessionContextService;
    private final ConversationLogService conversationLogService;
    private final SessionContextHolder sessionContextHolder;

    /** Function Calling模式的系统提示词 */
    private static final String SYSTEM_PROMPT =
            "你是一个专业的课程咨询顾问，负责为潜在学员提供课程咨询服务。\n" +
            "\n" +
            "## 最重要的规则：必须调用函数\n" +
            "**当你需要执行任何操作时，必须调用对应的函数，绝对不能只回复文字！**\n" +
            "- 用户要创建预约 → 必须调用 createReservation\n" +
            "- 用户要取消预约 → 先调用 queryReservation 查到预约，再调用 cancelReservation 取消\n" +
            "- 用户要修改预约 → 先调用 queryReservation 查到预约，再调用 updateReservation 修改\n" +
            "- 用户要查询预约 → 必须调用 queryReservation\n" +
            "- 用户要查课程 → 必须调用 searchCourses\n" +
            "- 用户要查校区 → 必须调用 getCampuses\n" +
            "- 用户提供了手机号 → 必须调用 queryCustomerByPhone 识别客户\n" +
            "- 用户问\"我的预约\" → 必须调用 listReservationsByPhone\n" +
            "**只回复文字而不调用函数 = 严重错误！**\n" +
            "\n" +
            "## 核心规则\n" +
            "1. **永远不要询问用户已经提供的信息** - 如果已知用户信息，直接使用\n" +
            "2. **不要重复确认已知信息**\n" +
            "3. **预约时直接使用已知信息**\n" +
            "\n" +
            "## 客户识别规则（重要！）\n" +
            "- 当用户提供了手机号（无论是否带\"电话\"前缀），先调用 queryCustomerByPhone 查询客户\n" +
            "- 如果查到客户，主动问候：\"您好，XX同学，又见面了！\"\n" +
            "- 如果没查到，正常服务即可\n" +
            "- 用户说\"我有哪些预约\"、\"我的预约\"时，调用 listReservationsByPhone 查询\n" +
            "\n" +
            "## 留言规则（重要！）\n" +
            "以下情况必须调用 leaveMessage 记录留言：\n" +
            "- 学员询问退款、退费政策\n" +
            "- 学员表达不满、投诉、建议\n" +
            "- 学员问的问题超出你的知识范围（如具体合同条款、特殊优惠政策等）\n" +
            "- 学员要求转人工但你无法处理\n" +
            "- 任何你无法给出准确回答的问题\n" +
            "记录留言时，先问清楚学员的姓名和电话（如果还不知道的话），然后调用 leaveMessage。\n" +
            "记录后告诉学员：\"已为您记录，客服会在2小时内联系您。\"\n" +
            "\n" +
            "## 你的主要任务\n" +
            "1. 识别回头客，提供个性化服务\n" +
            "2. 了解用户的兴趣、学历背景等信息\n" +
            "3. 根据用户需求推荐合适的课程\n" +
            "4. 引导用户预约试听课程\n" +
            "5. 引导用户留下联系方式\n" +
            "6. 无法回答时主动建议留言\n" +
            "\n" +
            "## 可用函数\n" +
            "\n" +
            "### 客户相关\n" +
            "- queryCustomerByPhone: 根据手机号查询客户信息（参数：phone）\n" +
            "- listReservationsByPhone: 查询某手机号的所有预约（参数：phone）\n" +
            "\n" +
            "### 留言相关\n" +
            "- leaveMessage: 记录学员留言（参数：sessionId、customerName可选、customerPhone可选、message、category可选）\n" +
            "\n" +
            "### 地区相关\n" +
            "- getProvinces: 获取有校区的省份列表\n" +
            "- getCities: 获取指定省份下有校区的城市（参数：provinceId）\n" +
            "- getCampuses: 获取校区列表（可按provinceId、cityId、courseId筛选）\n" +
            "  - 用户说\"我在上海\" → 用cityId筛选\n" +
            "  - 用户说\"我想学Python，哪里有\" → 用courseId筛选\n" +
            "\n" +
            "### 课程相关\n" +
            "- getCategories: 获取所有课程分类\n" +
            "- searchCourses: 搜索课程（可按keyword和categoryId筛选）\n" +
            "- getCampusCourses: 获取某个校区开设的课程（参数：campusId）\n" +
            "- getCourseSchedules: 获取校区课程的时间安排（参数：campusId、courseId）\n" +
            "\n" +
            "### 预约相关\n" +
            "- createReservation: 创建课程预约（参数：customerName、phone、courseId、campusId、scheduleId可选）\n" +
            "- updateReservation: 修改已有预约\n" +
            "- cancelReservation: 取消已有预约（参数：reservationId、reason）\n" +
            "- queryReservation: 查询预约信息（可按reservationId或phone查询）\n" +
            "\n" +
            "## 交互流程\n" +
            "1. 用户提供手机号 → queryCustomerByPhone → 识别回头客\n" +
            "2. 用户咨询课程 → getCategories → searchCourses\n" +
            "3. 用户询问校区 → getProvinces → getCities → getCampuses\n" +
            "4. 用户问\"这个校区有Python课吗\" → getCampusCourses\n" +
            "5. 用户问\"什么时候上课\" → getCourseSchedules\n" +
            "6. 创建预约 → createReservation\n" +
            "7. 修改/取消 → queryReservation → updateReservation/cancelReservation\n" +
            "8. 用户问\"我的预约\" → listReservationsByPhone\n" +
            "9. 遇到无法回答的问题 → leaveMessage\n" +
            "\n" +
            "## 重要提示\n" +
            "- 每个校区开设的课程不同，如果校区没开设某课程，要提示用户其他校区\n" +
            "- 课程有容量限制，满员时要提示\n" +
            "- 修改/取消预约前先查询\n" +
            "- 涉及退款、投诉、特殊优惠等问题，不要猜测答案，直接建议留言";

    @Override
    public String chat(String sessionId, String message) {
        // 从Redis获取或创建SessionContext（支持持久化和过期清理）
        SessionContext context = sessionContextService.getOrCreate(sessionId);
        context.setSessionId(sessionId);

        // 保存用户消息
        context.addMessage("用户", message);

        // 检查用户是否确认了预约修改/取消
        if (context.getPendingUpdate() != null && isConfirmMessage(message)) {
            String result = executePendingUpdate(context);
            context.addMessage("助手", result);
            sessionContextService.save(sessionId, context);
            // 写入对话记录
            String phone1 = context.getPhone();
            conversationLogService.saveLog(sessionId, phone1, "user", message);
            conversationLogService.saveLog(sessionId, phone1, "assistant", result);
            return result;
        }
        if (context.getPendingCancelReason() != null && isConfirmMessage(message)) {
            String result = executePendingCancel(context);
            context.addMessage("助手", result);
            sessionContextService.save(sessionId, context);
            // 写入对话记录
            String phone2 = context.getPhone();
            conversationLogService.saveLog(sessionId, phone2, "user", message);
            conversationLogService.saveLog(sessionId, phone2, "assistant", result);
            return result;
        }

        // 用户有待确认操作但回复的不是确认消息 → 视为拒绝，清除暂存
        if (context.getPendingUpdate() != null && !isConfirmMessage(message)) {
            context.setPendingUpdate(null);
            sessionContextService.save(sessionId, context);
        }
        if (context.getPendingCancelReason() != null && !isConfirmMessage(message)) {
            context.setPendingCancelReason(null);
            sessionContextService.save(sessionId, context);
        }

        // 提取用户信息
        extractInfoFromMessage(message, context);

        // 持久化上下文（提取信息后）
        sessionContextService.save(sessionId, context);

        // 构建系统提示词（包含已知信息）
        String systemMessage = buildSystemMessage(context);

        // 设置 ThreadLocal，让 Function Calling 函数能访问当前用户的 SessionContext
        SessionContextHolder.setSessionId(sessionId);

        String responseText;
        try {
            // 使用Function Calling调用AI，让ai知道有哪些方法，下面的方法全是根据Bean注册进来的因为Bean注解没有写，所以默认是函数名
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                    .defaultFunctions(
                        // 课程相关
                        "searchCourses", "getCategories", "getCampusCourses", "getCourseSchedules",
                        // 地区相关
                        "getProvinces", "getCities", "getCampuses",
                        // 预约相关
                        "createReservation", "updateReservation", "cancelReservation", "queryReservation",
                        // 客户相关
                        "queryCustomerByPhone", "listReservationsByPhone",
                        // 留言相关
                        "leaveMessage"
                    )
                    .build();

            responseText = chatClient.prompt()
                    .system(systemMessage)
                    .user(message)
                    .advisors(a -> a.param("chatMemoryConversationId", sessionId))
                    .call()
                    .content();

            log.info("Function Calling AI响应: {}", responseText);
        } finally {
            // 清除 ThreadLocal，防止内存泄漏
            SessionContextHolder.clear();
        }

        // 后处理响应 - 提取预约ID更新上下文，有些数据没有保存所以要拿到刚预约的id去根据id进行保存
        String processedResponse = processResponse(responseText, context);

        context.addMessage("助手", processedResponse);

        // 持久化上下文（响应处理后）
        sessionContextService.save(sessionId, context);

        // 写入对话记录
        String phone3 = context.getPhone();
        conversationLogService.saveLog(sessionId, phone3, "user", message);
        conversationLogService.saveLog(sessionId, phone3, "assistant", processedResponse);

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

        // 电话提取——优先有前缀，兜底裸手机号
        if (!context.hasInfo("phone")) {
            java.util.regex.Matcher phoneMatcher = java.util.regex.Pattern.compile(
                    "(?:电话|手机|联系方式|号码|tel)[\\s:：]*(1[3-9]\\d{9})"
            ).matcher(message);
            if (phoneMatcher.find()) {
                context.setPhone(phoneMatcher.group(1));
            } else {
                java.util.regex.Matcher bareMatcher = java.util.regex.Pattern.compile(
                        "(?<![\\d])(1[3-9]\\d{9})(?![\\d])"
                ).matcher(message);
                if (bareMatcher.find()) {
                    context.setPhone(bareMatcher.group(1));
                }
            }
        }

        // 姓名提取——只从明确表达中提取，不猜测
        if (!context.hasInfo("name")) {
            java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile(
                    "(?:我叫|我是|我姓|名字是|姓名是|叫我)[\\s]*([\\u4e00-\\u9fa5]{2,4})"
            ).matcher(message);
            if (nameMatcher.find()) {
                context.setCustomerName(nameMatcher.group(1));
            }
        }

        // 课程：从数据库动态匹配
        if (!context.hasInfo("course") && entities.containsKey("course")) {
            Course matched = intentMatcher.lookupCourse(entities.get("course"));
            if (matched != null) {
                context.setSelectedCourseId(matched.getId());
                context.setSelectedCourseName(matched.getName());
            }
        }

        // 校区：从数据库动态匹配
        if (!context.hasInfo("campus") && entities.containsKey("campus")) {
            Campus matched = intentMatcher.lookupCampus(entities.get("campus"));
            if (matched != null) {
                context.setSelectedCampusId(matched.getId());
                context.setSelectedCampusName(matched.getName());
            }
        }

        // 保存客户信息——有电话就存，姓名可以后续补全
        if (context.hasInfo("phone")) {
            //获取用户信息
            Customer customer = new Customer();
            customer.setPhone(context.getPhone());
            customer.setName(context.getCustomerName());
            customer.setEducation(context.getEducation());
            customer.setInterest(context.getInterest());
            customer.setSource(BizConstants.SOURCE_FUNCTION_CALLING);
            //保存用户信息
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
                    //根据手机号码查找最近的预约
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
            //获取预约id
            Long reservationId = context.getReservationId();
            //去数据库查询
            Reservation reservation = reservationService.getById(reservationId);
            if (reservation == null) {
                context.setPendingUpdate(null);
                return "预约记录不存在";
            }

            Map<String, Object> pendingData = context.getPendingUpdate();
            if (pendingData.containsKey("customerName")) {
                reservation.setCustomerName((String) pendingData.get("customerName"));
            }
            if (pendingData.containsKey("phone")) {
                reservation.setPhone((String) pendingData.get("phone"));
            }
            if (pendingData.containsKey("courseId")) {
                reservation.setCourseId((Long) pendingData.get("courseId"));
            }
            if (pendingData.containsKey("campusId")) {
                reservation.setCampusId((Long) pendingData.get("campusId"));
            }

            reservationService.updateById(reservation);
            context.setPendingUpdate(null);

            StringBuilder result = new StringBuilder("预约修改成功！");
            if (pendingData.containsKey("customerName")) {
                result.append("\n姓名：").append(reservation.getCustomerName());
            }
            if (pendingData.containsKey("phone")) {
                result.append("\n电话：").append(reservation.getPhone());
            }
            if (pendingData.containsKey("courseId")) {
                Course course = courseService.getById(reservation.getCourseId());
                result.append("\n课程：").append(course != null ? course.getName() : "未知");
            }
            if (pendingData.containsKey("campusId")) {
                Campus campus = campusService.getById(reservation.getCampusId());
                result.append("\n校区：").append(campus != null ? campus.getName() : "未知");
            }
            return result.toString();
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
