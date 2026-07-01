package com.aicustomer.service;

import com.aicustomer.entity.ReservationLog;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface ReservationLogService extends IService<ReservationLog> {
    void addLog(Long reservationId, Integer oldStatus, Integer newStatus, String operator, String remark);
    List<ReservationLog> getLogsByReservationId(Long reservationId);
}
