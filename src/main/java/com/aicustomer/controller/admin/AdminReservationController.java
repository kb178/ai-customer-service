package com.aicustomer.controller.admin;

import com.aicustomer.config.BizConstants;
import com.aicustomer.entity.Reservation;
import com.aicustomer.entity.ReservationLog;
import com.aicustomer.service.ReservationLogService;
import com.aicustomer.service.ReservationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reservation")
@RequiredArgsConstructor
public class AdminReservationController {

    private final ReservationService reservationService;
    private final ReservationLogService reservationLogService;

    @GetMapping("/list")
    public AdminResponse<PageResult<Reservation>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<Reservation> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(phone), Reservation::getPhone, phone)
               .eq(status != null, Reservation::getStatus, status)
               .ge(StringUtils.hasText(startDate), Reservation::getCreateTime, 
                   StringUtils.hasText(startDate) ? LocalDate.parse(startDate).atStartOfDay() : null)
               .le(StringUtils.hasText(endDate), Reservation::getCreateTime, 
                   StringUtils.hasText(endDate) ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null)
               .orderByDesc(Reservation::getCreateTime);
        Page<Reservation> result = reservationService.page(pageParam, wrapper);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public AdminResponse<Reservation> detail(@PathVariable Long id) {
        Reservation reservation = reservationService.getById(id);
        if (reservation == null) {
            return AdminResponse.error(404, "预约不存在");
        }
        return AdminResponse.ok(reservation);
    }

    @PutMapping("/{id}/confirm")
    public AdminResponse<Void> confirm(@PathVariable Long id) {
        Reservation reservation = reservationService.getById(id);
        if (reservation == null) {
            return AdminResponse.error(404, "预约不存在");
        }
        int oldStatus = reservation.getStatus();
        reservation.setStatus(BizConstants.STATUS_CONFIRMED);
        reservationService.updateById(reservation);

        reservationLogService.addLog(id, oldStatus, BizConstants.STATUS_CONFIRMED, "admin", "管理员确认预约");
        return AdminResponse.ok();
    }

    @PutMapping("/{id}/cancel")
    public AdminResponse<Void> cancel(@PathVariable Long id) {
        Reservation reservation = reservationService.getById(id);
        if (reservation == null) {
            return AdminResponse.error(404, "预约不存在");
        }
        int oldStatus = reservation.getStatus();
        reservation.setStatus(BizConstants.STATUS_CANCELLED);
        reservationService.updateById(reservation);

        reservationLogService.addLog(id, oldStatus, BizConstants.STATUS_CANCELLED, "admin", "管理员取消预约");
        return AdminResponse.ok();
    }

    @PutMapping("/{id}/complete")
    public AdminResponse<Void> complete(@PathVariable Long id) {
        Reservation reservation = reservationService.getById(id);
        if (reservation == null) {
            return AdminResponse.error(404, "预约不存在");
        }
        int oldStatus = reservation.getStatus();
        reservation.setStatus(BizConstants.STATUS_COMPLETED);
        reservationService.updateById(reservation);

        reservationLogService.addLog(id, oldStatus, BizConstants.STATUS_COMPLETED, "admin", "管理员标记完成");
        return AdminResponse.ok();
    }
}
