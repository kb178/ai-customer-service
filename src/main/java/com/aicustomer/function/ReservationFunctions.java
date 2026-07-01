package com.aicustomer.function;

import com.aicustomer.entity.Course;
import com.aicustomer.entity.Campus;
import com.aicustomer.entity.CampusCourse;
import com.aicustomer.entity.CourseSchedule;
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
import java.util.List;
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
                List<Campus> otherCampuses = campusService.listByIds(otherCampusIds);

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
                reservation.setAppointmentTime(LocalDateTime.parse(request.getAppointmentTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                reservation.setAppointmentTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
            }

            reservationService.createReservation(reservation);

            // 记录日志
            reservationLogService.addLog(reservation.getId(), null, 0, "system", "创建预约");

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
     */
    @Bean
    @Description("修改已有预约。参数：reservationId(预约ID)、courseId(课程ID，可选)、campusId(校区ID，可选)")
    public Function<UpdateReservationRequest, UpdateReservationResponse> updateReservation() {
        return request -> {
            log.info("Function Calling - 修改预约: reservationId={}", request.getReservationId());

            Reservation reservation = reservationService.getById(request.getReservationId());
            if (reservation == null) {
                UpdateReservationResponse response = new UpdateReservationResponse();
                response.setSuccess(false);
                response.setMessage("预约记录不存在");
                return response;
            }

            Integer oldStatus = reservation.getStatus();

            if (request.getCustomerName() != null && !request.getCustomerName().isEmpty()) {
                reservation.setCustomerName(request.getCustomerName());
            }
            if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                reservation.setPhone(request.getPhone());
            }
            if (request.getCourseId() != null) {
                reservation.setCourseId(request.getCourseId());
            }
            if (request.getCampusId() != null) {
                reservation.setCampusId(request.getCampusId());
            }

            reservationService.updateById(reservation);

            // 记录日志
            reservationLogService.addLog(reservation.getId(), oldStatus, reservation.getStatus(), "system", "修改预约");

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
        };
    }

    /**
     * 取消预约函数
     */
    @Bean
    @Description("取消已有预约。参数：reservationId(预约ID)、reason(取消原因)")
    public Function<CancelReservationRequest, CancelReservationResponse> cancelReservation() {
        return request -> {
            log.info("Function Calling - 取消预约: reservationId={}, reason={}", request.getReservationId(), request.getReason());

            Reservation reservation = reservationService.getById(request.getReservationId());
            if (reservation == null) {
                CancelReservationResponse response = new CancelReservationResponse();
                response.setSuccess(false);
                response.setMessage("预约记录不存在");
                return response;
            }

            Integer oldStatus = reservation.getStatus();
            reservation.setStatus(3);
            reservation.setRemark("取消原因：" + request.getReason());
            reservationService.updateById(reservation);

            // 记录日志
            reservationLogService.addLog(reservation.getId(), oldStatus, 3, "system", "取消预约：" + request.getReason());

            // 释放校区课程容量
            if (reservation.getCampusId() != null && reservation.getCourseId() != null) {
                CampusCourse campusCourse = campusCourseService.getCampusCourse(reservation.getCampusId(), reservation.getCourseId());
                if (campusCourse != null && campusCourse.getCurrentStudents() != null && campusCourse.getCurrentStudents() > 0) {
                    campusCourse.setCurrentStudents(campusCourse.getCurrentStudents() - 1);
                    campusCourseService.updateById(campusCourse);
                }
            }

            CancelReservationResponse response = new CancelReservationResponse();
            response.setSuccess(true);
            response.setMessage("预约已取消");
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

            Reservation reservation = null;
            if (request.getReservationId() != null) {
                reservation = reservationService.getById(request.getReservationId());
            } else if (request.getPhone() != null && !request.getPhone().isEmpty()) {
                reservation = reservationService.lambdaQuery()
                        .eq(Reservation::getPhone, request.getPhone())
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

            String statusText = switch (reservation.getStatus()) {
                case 0 -> "待确认";
                case 1 -> "已确认";
                case 2 -> "已完成";
                case 3 -> "已取消";
                default -> "未知";
            };

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
