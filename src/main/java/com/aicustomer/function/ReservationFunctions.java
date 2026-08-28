package com.aicustomer.function;

import com.aicustomer.config.BizConstants;
import com.aicustomer.config.SessionContextHolder;
import com.aicustomer.entity.SessionContext;
import com.aicustomer.entity.Course;
import com.aicustomer.entity.Campus;
import com.aicustomer.entity.CampusCourse;
import com.aicustomer.entity.CourseSchedule;
import com.aicustomer.entity.Customer;
import com.aicustomer.entity.Reservation;
import com.aicustomer.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 预约相关Function定义
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReservationFunctions {

    private final ReservationService reservationService;
    private final ReservationLogService reservationLogService;
    private final CourseService courseService;
    private final CampusService campusService;
    private final CampusCourseService campusCourseService;
    private final CourseScheduleService courseScheduleService;
    private final CustomerService customerService;
    private final SessionContextHolder sessionContextHolder;

    /**
     * 创建预约函数
     */
    @Bean
    @Description("创建课程预约。参数：customerName(姓名)、phone(电话)、courseId(课程ID)、campusId(校区ID)、scheduleId(时间段ID，可选)")
    public Function<CreateReservationRequest, CreateReservationResponse> createReservation() {
        return request -> {
            log.info("Function Calling - 创建预约: name={}, phone={}, courseId={}, campusId={}, scheduleId={}",
                    request.getCustomerName(), request.getPhone(), request.getCourseId(), request.getCampusId(), request.getScheduleId());

            // 检查课程是否存在
            Course course = courseService.getById(request.getCourseId());
            if (course == null) {
                CreateReservationResponse response = new CreateReservationResponse();
                response.setSuccess(false);
                response.setMessage("课程不存在");
                return response;
            }

            // 检查校区是否存在
            Campus campus = campusService.getById(request.getCampusId());
            if (campus == null) {
                CreateReservationResponse response = new CreateReservationResponse();
                response.setSuccess(false);
                response.setMessage("校区不存在");
                return response;
            }

            // 检查该校区是否开设此课程
            CampusCourse campusCourse = campusCourseService.getCampusCourse(request.getCampusId(), request.getCourseId());
            if (campusCourse == null) {
                // 查找其他开设该课程的校区
                List<CampusCourse> otherCampusCourses = campusCourseService.getCampusesByCourseId(request.getCourseId());
                List<Long> otherCampusIds = otherCampusCourses.stream()
                        .map(CampusCourse::getCampusId)
                        .toList();
                List<Campus> otherCampuses = otherCampusIds.isEmpty()
                        ? List.of()
                        : campusService.listByIds(otherCampusIds);

                StringBuilder sb = new StringBuilder();
                sb.append("抱歉，").append(campus.getName()).append("未开设").append(course.getName()).append("课程。\n\n");
                if (!otherCampuses.isEmpty()) {
                    sb.append("以下校区开设了该课程：\n");
                    for (Campus c : otherCampuses) {
                        sb.append("- ").append(c.getName()).append("（").append(c.getAddress()).append("）\n");
                    }
                } else {
                    sb.append("目前没有校区开设此课程。");
                }

                CreateReservationResponse response = new CreateReservationResponse();
                response.setSuccess(false);
                response.setMessage(sb.toString());
                return response;
            }

            // 检查校区课程容量
            if (campusCourse.getMaxStudents() != null && campusCourse.getCurrentStudents() != null) {
                if (campusCourse.getCurrentStudents() >= campusCourse.getMaxStudents()) {
                    CreateReservationResponse response = new CreateReservationResponse();
                    response.setSuccess(false);
                    response.setMessage("该校区此课程已满员，请选择其他校区");
                    return response;
                }
            }

            // 如果指定了时间段，检查时间段容量
            if (request.getScheduleId() != null) {
                CourseSchedule schedule = courseScheduleService.getById(request.getScheduleId());
                if (schedule == null) {
                    CreateReservationResponse response = new CreateReservationResponse();
                    response.setSuccess(false);
                    response.setMessage("时间段不存在");
                    return response;
                }

                if (schedule.getMaxStudents() != null && schedule.getCurrentStudents() != null) {
                    if (schedule.getCurrentStudents() >= schedule.getMaxStudents()) {
                        CreateReservationResponse response = new CreateReservationResponse();
                        response.setSuccess(false);
                        response.setMessage("该时间段已满员，请选择其他时间段");
                        return response;
                    }
                }
            }

            Reservation reservation = new Reservation();
            reservation.setCustomerName(request.getCustomerName());
            reservation.setPhone(request.getPhone());
            reservation.setCourseId(request.getCourseId());
            reservation.setCampusId(request.getCampusId());

            if (request.getAppointmentTime() != null && !request.getAppointmentTime().isEmpty()) {
                String timeStr = request.getAppointmentTime().trim();
                DateTimeFormatter formatter = timeStr.length() > 16
                        ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                reservation.setAppointmentTime(LocalDateTime.parse(timeStr, formatter));
            } else {
                reservation.setAppointmentTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            }

            reservationService.createReservation(reservation);

            // 创建成功后，直接把预约ID存到 SessionContext
            SessionContext context = sessionContextHolder.getCurrentContext();
            if (context != null) {
                context.setReservationId(reservation.getId());
                // 同步更新客户信息到上下文
                if (request.getCustomerName() != null && !request.getCustomerName().isEmpty()) {
                    context.setCustomerName(request.getCustomerName());
                }
                if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                    context.setPhone(request.getPhone());
                }
                if (request.getCourseId() != null) {
                    context.setSelectedCourseId(request.getCourseId());
                    context.setSelectedCourseName(course.getName());
                }
                if (request.getCampusId() != null) {
                    context.setSelectedCampusId(request.getCampusId());
                    context.setSelectedCampusName(campus.getName());
                }
                sessionContextHolder.saveCurrentContext(context);
                log.info("预约ID已存入SessionContext: {}", reservation.getId());
            }

            // 记录日志
            reservationLogService.addLog(reservation.getId(), null, 0, "system", "创建预约");

            // 保存客户信息到customer表（有电话就存）
            if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                Customer customer = new Customer();
                customer.setPhone(request.getPhone());
                customer.setName(request.getCustomerName());
                customer.setSource(BizConstants.SOURCE_RESERVATION);
                customerService.saveCustomer(customer);
            }

            // 更新校区课程学员数
            campusCourse.setCurrentStudents(campusCourse.getCurrentStudents() != null ? campusCourse.getCurrentStudents() + 1 : 1);
            campusCourseService.updateById(campusCourse);

            // 如果指定了时间段，更新时间段学员数
            if (request.getScheduleId() != null) {
                CourseSchedule schedule = courseScheduleService.getById(request.getScheduleId());
                if (schedule != null) {
                    schedule.setCurrentStudents(schedule.getCurrentStudents() != null ? schedule.getCurrentStudents() + 1 : 1);
                    courseScheduleService.updateById(schedule);
                }
            }

            CreateReservationResponse response = new CreateReservationResponse();
            response.setSuccess(true);
            response.setMessage("预约成功");
            response.setReservationId(reservation.getId());
            response.setCustomerName(request.getCustomerName());
            response.setPhone(request.getPhone());
            response.setCourseName(course.getName());
            response.setCampusName(campus.getName());
            return response;
        };
    }

    /**
     * 修改预约函数
     *
     * 确认机制：
     * - 第一次调用：暂存修改数据到 pendingUpdate，返回确认提示
     * - 用户确认后 chat() 会调用 executePendingUpdate() 直接执行，不经过此函数
     * - 如果用户确认后 LLM 再次调用此函数，检测到 pending 匹配则直接执行
     */
    @Bean
    @Description("修改已有预约。参数：reservationId(预约ID)、courseId(课程ID，可选)、campusId(校区ID，可选)")
    public Function<UpdateReservationRequest, UpdateReservationResponse> updateReservation() {
        return request -> {
            log.info("Function Calling - 修改预约: reservationId={}", request.getReservationId());

            // 如果 LLM 没传 reservationId，从 SessionContext 获取
            Long reservationId = request.getReservationId();
            if (reservationId == null) {
                SessionContext ctx = sessionContextHolder.getCurrentContext();
                if (ctx != null && ctx.getReservationId() != null) {
                    reservationId = ctx.getReservationId();
                    log.info("从 SessionContext 获取预约ID: {}", reservationId);
                }
            }

            if (reservationId == null) {
                UpdateReservationResponse response = new UpdateReservationResponse();
                response.setSuccess(false);
                response.setMessage("未找到预约记录，请先提供预约ID或手机号查询");
                return response;
            }

            Reservation reservation = reservationService.getById(reservationId);
            if (reservation == null) {
                UpdateReservationResponse response = new UpdateReservationResponse();
                response.setSuccess(false);
                response.setMessage("预约记录不存在");
                return response;
            }

            // 检查是否有待确认的修改（用户已确认，LLM 再次调用的情况）
            SessionContext context = sessionContextHolder.getCurrentContext();
            if (context != null && context.getPendingUpdate() != null) {
                // 用户已确认，直接执行
                Map<String, Object> pendingData = context.getPendingUpdate();
                if (pendingData.containsKey("courseId")) {
                    reservation.setCourseId((Long) pendingData.get("courseId"));
                }
                if (pendingData.containsKey("campusId")) {
                    reservation.setCampusId((Long) pendingData.get("campusId"));
                }
                reservationService.updateById(reservation);
                context.setPendingUpdate(null);
                sessionContextHolder.saveCurrentContext(context);

                Course course = courseService.getById(reservation.getCourseId());
                Campus campus = campusService.getById(reservation.getCampusId());

                UpdateReservationResponse response = new UpdateReservationResponse();
                response.setSuccess(true);
                response.setMessage("预约修改成功");
                response.setCustomerName(reservation.getCustomerName());
                response.setPhone(reservation.getPhone());
                response.setCourseName(course != null ? course.getName() : "未知");
                response.setCampusName(campus != null ? campus.getName() : "未知");
                return response;
            }

            // 没有 pending → 暂存修改数据，等待用户确认
            Map<String, Object> pending = new HashMap<>();
            if (request.getCourseId() != null) {
                pending.put("courseId", request.getCourseId());
            }
            if (request.getCampusId() != null) {
                pending.put("campusId", request.getCampusId());
            }

            if (pending.isEmpty()) {
                UpdateReservationResponse response = new UpdateReservationResponse();
                response.setSuccess(false);
                response.setMessage("请提供要修改的内容（课程或校区）");
                return response;
            }

            // 暂存到 SessionContext
            if (context != null) {
                context.setPendingUpdate(pending);
                sessionContextHolder.saveCurrentContext(context);
            }

            // 构建确认提示
            StringBuilder sb = new StringBuilder("您确认要修改预约吗？\n");
            if (pending.containsKey("courseId")) {
                Course course = courseService.getById((Long) pending.get("courseId"));
                sb.append("- 课程改为：").append(course != null ? course.getName() : "未知").append("\n");
            }
            if (pending.containsKey("campusId")) {
                Campus campus = campusService.getById((Long) pending.get("campusId"));
                sb.append("- 校区改为：").append(campus != null ? campus.getName() : "未知").append("\n");
            }
            sb.append("\n请回复【确认】执行修改，或回复其他内容取消。");

            UpdateReservationResponse response = new UpdateReservationResponse();
            response.setSuccess(true);
            response.setMessage(sb.toString());
            return response;
        };
    }

    /**
     * 取消预约函数
     *
     * 确认机制：
     * - 第一次调用：暂存取消原因到 pendingCancelReason，返回确认提示
     * - 用户确认后 chat() 会调用 executePendingCancel() 直接执行
     * - 如果用户确认后 LLM 再次调用此函数，检测到 pending 则直接执行
     */
    @Bean
    @Description("取消已有预约。参数：reservationId(预约ID)、reason(取消原因，可选)")
    public Function<CancelReservationRequest, CancelReservationResponse> cancelReservation() {
        return request -> {
            log.info("Function Calling - 取消预约: reservationId={}, reason={}", request.getReservationId(), request.getReason());

            // 如果 LLM 没传 reservationId，从 SessionContext 获取
            Long reservationId = request.getReservationId();
            if (reservationId == null) {
                SessionContext ctx = sessionContextHolder.getCurrentContext();
                if (ctx != null && ctx.getReservationId() != null) {
                    reservationId = ctx.getReservationId();
                    log.info("从 SessionContext 获取预约ID: {}", reservationId);
                }
            }

            if (reservationId == null) {
                CancelReservationResponse response = new CancelReservationResponse();
                response.setSuccess(false);
                response.setMessage("未找到预约记录，请先提供预约ID或手机号查询");
                return response;
            }

            Reservation reservation = reservationService.getById(reservationId);
            if (reservation == null) {
                CancelReservationResponse response = new CancelReservationResponse();
                response.setSuccess(false);
                response.setMessage("预约记录不存在");
                return response;
            }

            // 检查是否有待确认的取消（用户已确认，LLM 再次调用的情况）
            SessionContext context = sessionContextHolder.getCurrentContext();
            if (context != null && context.getPendingCancelReason() != null) {
                // 用户已确认，直接执行取消
                String reason = context.getPendingCancelReason();
                Integer oldStatus = reservation.getStatus();
                reservation.setStatus(BizConstants.STATUS_CANCELLED);
                reservation.setRemark("取消原因：" + reason);
                reservationService.updateById(reservation);

                reservationLogService.addLog(reservation.getId(), oldStatus, 3, "system", "取消预约：" + reason);

                // 释放校区课程容量
                if (reservation.getCampusId() != null && reservation.getCourseId() != null) {
                    CampusCourse campusCourse = campusCourseService.getCampusCourse(reservation.getCampusId(), reservation.getCourseId());
                    if (campusCourse != null && campusCourse.getCurrentStudents() != null && campusCourse.getCurrentStudents() > 0) {
                        campusCourse.setCurrentStudents(campusCourse.getCurrentStudents() - 1);
                        campusCourseService.updateById(campusCourse);
                    }
                }

                context.setPendingCancelReason(null);
                context.setReservationId(null);
                sessionContextHolder.saveCurrentContext(context);

                CancelReservationResponse response = new CancelReservationResponse();
                response.setSuccess(true);
                response.setMessage("预约已取消");
                return response;
            }

            // 没有 pending → 暂存取消原因，等待用户确认
            String reason = (request.getReason() != null && !request.getReason().isEmpty())
                    ? request.getReason() : "用户主动取消";

            if (context != null) {
                context.setPendingCancelReason(reason);
                sessionContextHolder.saveCurrentContext(context);
            }

            CancelReservationResponse response = new CancelReservationResponse();
            response.setSuccess(true);
            response.setMessage("您确认要取消预约吗？\n取消原因：" + reason + "\n\n请回复【确认】执行取消，或回复其他内容取消操作。");
            return response;
        };
    }

    /**
     * 查询预约信息
     */
    @Bean
    @Description("查询预约信息。参数：reservationId(预约ID，可选)、phone(手机号，可选)")
    public Function<QueryReservationRequest, QueryReservationResponse> queryReservation() {
        return request -> {
            log.info("Function Calling - 查询预约: reservationId={}, phone={}", request.getReservationId(), request.getPhone());

            // 从 SessionContext 兜底获取参数
            SessionContext ctx = sessionContextHolder.getCurrentContext();
            Long reservationId = request.getReservationId();
            String phone = request.getPhone();

            if (reservationId == null && ctx != null && ctx.getReservationId() != null) {
                reservationId = ctx.getReservationId();
                log.info("从 SessionContext 获取预约ID: {}", reservationId);
            }
            if ((phone == null || phone.isEmpty()) && ctx != null && ctx.getPhone() != null) {
                phone = ctx.getPhone();
                log.info("从 SessionContext 获取手机号: {}", phone);
            }

            Reservation reservation = null;
            if (reservationId != null) {
                reservation = reservationService.getById(reservationId);
            } else if (phone != null && !phone.isEmpty()) {
                reservation = reservationService.lambdaQuery()
                        .eq(Reservation::getPhone, phone)
                        .orderByDesc(Reservation::getCreateTime)
                        .last("LIMIT 1")
                        .one();
            }

            QueryReservationResponse response = new QueryReservationResponse();
            if (reservation == null) {
                response.setFound(false);
                response.setMessage("未找到预约记录");
                return response;
            }

            Course course = reservation.getCourseId() != null ? courseService.getById(reservation.getCourseId()) : null;
            Campus campus = reservation.getCampusId() != null ? campusService.getById(reservation.getCampusId()) : null;

            String statusText;
            switch (reservation.getStatus()) {
                case BizConstants.STATUS_PENDING: statusText = "待确认"; break;
                case BizConstants.STATUS_CONFIRMED: statusText = "已确认"; break;
                case BizConstants.STATUS_COMPLETED: statusText = "已完成"; break;
                case BizConstants.STATUS_CANCELLED: statusText = "已取消"; break;
                default: statusText = "未知";
            }

            response.setFound(true);
            response.setReservationId(reservation.getId());
            response.setCustomerName(reservation.getCustomerName());
            response.setPhone(reservation.getPhone());
            response.setCourseName(course != null ? course.getName() : "未知");
            response.setCampusName(campus != null ? campus.getName() : "未知");
            response.setStatus(statusText);
            response.setAppointmentTime(reservation.getAppointmentTime() != null ? reservation.getAppointmentTime().toString() : null);
            response.setRemark(reservation.getRemark());
            return response;
        };
    }

    @Data
    public static class CreateReservationRequest {
        private String customerName;
        private String phone;
        private Long courseId;
        private Long campusId;
        private Long scheduleId;
        private String appointmentTime;
        public void setCourseId(Object value) { this.courseId = LongParser.parse(value); }
        public void setCampusId(Object value) { this.campusId = LongParser.parse(value); }
        public void setScheduleId(Object value) { this.scheduleId = LongParser.parse(value); }
    }

    @Data
    public static class CreateReservationResponse {
        private boolean success;
        private String message;
        private Long reservationId;
        private String customerName;
        private String phone;
        private String courseName;
        private String campusName;
    }

    @Data
    public static class UpdateReservationRequest {
        private Long reservationId;
        private String customerName;
        private String phone;
        private Long courseId;
        private Long campusId;
        public void setReservationId(Object value) { this.reservationId = LongParser.parse(value); }
        public void setCourseId(Object value) { this.courseId = LongParser.parse(value); }
        public void setCampusId(Object value) { this.campusId = LongParser.parse(value); }
    }

    @Data
    public static class UpdateReservationResponse {
        private boolean success;
        private String message;
        private String customerName;
        private String phone;
        private String courseName;
        private String campusName;
    }

    @Data
    public static class CancelReservationRequest {
        private Long reservationId;
        private String reason;
        public void setReservationId(Object value) { this.reservationId = LongParser.parse(value); }
    }

    @Data
    public static class CancelReservationResponse {
        private boolean success;
        private String message;
    }

    @Data
    public static class QueryReservationRequest {
        private Long reservationId;
        private String phone;
        public void setReservationId(Object value) { this.reservationId = LongParser.parse(value); }
    }

    @Data
    public static class QueryReservationResponse {
        private boolean found;
        private String message;
        private Long reservationId;
        private String customerName;
        private String phone;
        private String courseName;
        private String campusName;
        private String status;
        private String appointmentTime;
        private String remark;
    }
}
