package com.aicustomer.function;

import com.aicustomer.entity.Course;
import com.aicustomer.entity.Campus;
import com.aicustomer.entity.Customer;
import com.aicustomer.entity.Reservation;
import com.aicustomer.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 客户相关Function定义
 *
 * 让AI能够识别回头客，查询客户资料和历史预约
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class CustomerFunctions {

    private final CustomerService customerService;
    private final ReservationService reservationService;
    private final CourseService courseService;
    private final CampusService campusService;

    /**
     * 根据手机号查询客户信息
     */
    @Bean
    @Description("根据手机号查询客户信息。参数：phone(手机号)")
    public Function<QueryCustomerRequest, QueryCustomerResponse> queryCustomerByPhone() {
        return request -> {
            log.info("Function Calling - 查询客户: phone={}", request.getPhone());

            QueryCustomerResponse response = new QueryCustomerResponse();

            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                response.setFound(false);
                response.setMessage("请输入手机号");
                return response;
            }

            Customer customer = customerService.lambdaQuery()
                    .eq(Customer::getPhone, request.getPhone())
                    .one();

            if (customer == null) {
                response.setFound(false);
                response.setMessage("未找到该客户信息");
                return response;
            }

            response.setFound(true);
            response.setCustomerId(customer.getId());
            response.setName(customer.getName());
            response.setPhone(customer.getPhone());
            response.setEducation(customer.getEducation());
            response.setInterest(customer.getInterest());
            response.setSource(customer.getSource());
            return response;
        };
    }

    /**
     * 查询某手机号的所有预约记录
     */
    @Bean
    @Description("查询某手机号的所有预约记录。参数：phone(手机号)")
    public Function<ListReservationsRequest, ListReservationsResponse> listReservationsByPhone() {
        return request -> {
            log.info("Function Calling - 查询客户预约: phone={}", request.getPhone());

            ListReservationsResponse response = new ListReservationsResponse();

            if (request.getPhone() == null || request.getPhone().isEmpty()) {
                response.setReservations(List.of());
                response.setTotal(0);
                response.setMessage("请输入手机号");
                return response;
            }

            List<Reservation> reservations = reservationService.lambdaQuery()
                    .eq(Reservation::getPhone, request.getPhone())
                    .orderByDesc(Reservation::getCreateTime)
                    .list();

            if (reservations.isEmpty()) {
                response.setReservations(List.of());
                response.setTotal(0);
                response.setMessage("该手机号没有预约记录");
                return response;
            }

            List<ReservationItem> items = new ArrayList<>();
            for (Reservation r : reservations) {
                ReservationItem item = new ReservationItem();
                item.setReservationId(r.getId());
                item.setCustomerName(r.getCustomerName());
                item.setPhone(r.getPhone());

                Course course = r.getCourseId() != null ? courseService.getById(r.getCourseId()) : null;
                Campus campus = r.getCampusId() != null ? campusService.getById(r.getCampusId()) : null;

                item.setCourseName(course != null ? course.getName() : "未知");
                item.setCampusName(campus != null ? campus.getName() : "未知");
                item.setAppointmentTime(r.getAppointmentTime() != null ? r.getAppointmentTime().toString() : null);

                String statusText;
                switch (r.getStatus()) {
                    case 0: statusText = "待确认"; break;
                    case 1: statusText = "已确认"; break;
                    case 2: statusText = "已完成"; break;
                    case 3: statusText = "已取消"; break;
                    default: statusText = "未知"; break;
                }
                item.setStatus(statusText);
                item.setRemark(r.getRemark());
                items.add(item);
            }

            response.setReservations(items);
            response.setTotal(items.size());
            response.setMessage("共找到" + items.size() + "条预约记录");
            return response;
        };
    }

    // ========== 请求/响应类 ==========

    @Data
    public static class QueryCustomerRequest {
        private String phone;
    }

    @Data
    public static class QueryCustomerResponse {
        private boolean found;
        private String message;
        private Long customerId;
        private String name;
        private String phone;
        private String education;
        private String interest;
        private String source;
    }

    @Data
    public static class ListReservationsRequest {
        private String phone;
    }

    @Data
    public static class ListReservationsResponse {
        private List<ReservationItem> reservations;
        private int total;
        private String message;
    }

    @Data
    public static class ReservationItem {
        private Long reservationId;
        private String customerName;
        private String phone;
        private String courseName;
        private String campusName;
        private String appointmentTime;
        private String status;
        private String remark;
    }
}
