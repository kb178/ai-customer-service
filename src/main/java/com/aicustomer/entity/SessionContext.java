package com.aicustomer.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 会话上下文实体类
 * 
 * 功能：保存用户对话过程中的上下文信息，实现多轮对话记忆
 * 
 * 主要作用：
 * - 记录用户已提供的信息（姓名、电话、学历等）
 * - 记录用户选择的课程和校区
 * - 记录当前预约ID，支持预约修改
 * - 保存对话历史，提供上下文给AI
 */
@Data
public class SessionContext {

    /** 会话ID（唯一标识一次对话） */
    private String sessionId;

    /** 客户姓名（从对话中提取） */
    private String customerName;

    /** 联系电话（从对话中提取） */
    private String phone;

    /** 邮箱地址 */
    private String email;

    /** 年龄 */
    private Integer age;

    /** 学历（从对话中提取，如：大一、研究生） */
    private String education;

    /** 兴趣爱好（从对话中提取，如：编程开发） */
    private String interest;

    /** 已选课程ID */
    private Long selectedCourseId;

    /** 已选课程名称 */
    private String selectedCourseName;

    /** 已选校区ID */
    private Long selectedCampusId;

    /** 已选校区名称 */
    private String selectedCampusName;

    /** 当前预约ID（用于修改预约） */
    private Long reservationId;

    /** 待确认的修改数据（修改预约时暂存，确认后才保存到数据库） */
    private java.util.Map<String, Object> pendingUpdate;

    /** 待确认的取消预约操作（暂存取消原因，确认后才保存到数据库） */
    private String pendingCancelReason;

    /** 对话历史记录（最近20条） */
    private List<String> conversationHistory = new ArrayList<>();

    /** 会话创建时间 */
    private LocalDateTime createTime;

    /**
     * 构造函数 - 初始化会话创建时间
     */
    public SessionContext() {
        this.createTime = LocalDateTime.now();
    }

    /**
     * 添加对话消息到历史记录
     * 
     * @param role 角色（用户/助手）
     * @param content 消息内容
     */
    public void addMessage(String role, String content) {
        conversationHistory.add(String.format("[%s] %s: %s", LocalDateTime.now(), role, content));
        // 只保留最近20条记录，避免内存溢出
        if (conversationHistory.size() > 20) {
            conversationHistory = new ArrayList<>(conversationHistory.subList(conversationHistory.size() - 20, conversationHistory.size()));
        }
    }

    /**
     * 检查是否已收集到指定信息
     * 
     * @param field 字段名（name/phone/education/interest/course/campus）
     * @return true表示已有该信息，false表示未收集到
     */
    public boolean hasInfo(String field) {
        switch (field) {
            case "name": return customerName != null && !customerName.isEmpty();
            case "phone": return phone != null && !phone.isEmpty();
            case "education": return education != null && !education.isEmpty();
            case "interest": return interest != null && !interest.isEmpty();
            case "course": return selectedCourseId != null;
            case "campus": return selectedCampusId != null;
            default: return false;
        }
    }

    /**
     * 获取已知用户信息摘要
     * 
     * 功能：生成已收集信息的文本摘要，用于注入到AI系统提示词
     * 
     * @return 信息摘要字符串
     */
    public String getKnownInfoSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("已知用户信息：\n");

        if (hasInfo("name")) sb.append("- 姓名: ").append(customerName).append("\n");
        if (hasInfo("phone")) sb.append("- 电话: ").append(phone).append("\n");
        if (hasInfo("education")) sb.append("- 学历: ").append(education).append("\n");
        if (hasInfo("interest")) sb.append("- 兴趣: ").append(interest).append("\n");
        if (hasInfo("course")) sb.append("- 已选课程: ").append(selectedCourseName).append("\n");
        if (hasInfo("campus")) sb.append("- 已选校区: ").append(selectedCampusName).append("\n");

        if (sb.toString().equals("已知用户信息：\n")) {
            return "暂无已知用户信息";
        }

        return sb.toString();
    }

    /**
     * 获取预约所需但缺失的信息
     * 
     * 功能：检查创建预约所需的信息是否齐全，返回缺失项
     * 
     * @return 缺失信息提示字符串，如果信息齐全则返回空字符串
     */
    public String getMissingInfoForReservation() {
        List<String> missing = new ArrayList<>();

        if (!hasInfo("name")) missing.add("姓名");
        if (!hasInfo("phone")) missing.add("电话");
        if (!hasInfo("course")) missing.add("课程");
        if (!hasInfo("campus")) missing.add("校区");

        if (missing.isEmpty()) {
            return "";
        }
        return "预约还需要以下信息：" + String.join("、", missing);
    }
}
