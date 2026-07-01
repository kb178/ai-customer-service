package com.aicustomer.service.impl;

import com.aicustomer.entity.ReservationLog;
import com.aicustomer.mapper.ReservationLogMapper;
import com.aicustomer.service.ReservationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ReservationLogServiceImpl extends ServiceImpl<ReservationLogMapper, ReservationLog> implements ReservationLogService {

    @Override
    public void addLog(Long reservationId, Integer oldStatus, Integer newStatus, String operator, String remark) {
        ReservationLog log = new ReservationLog();
        log.setReservationId(reservationId);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setOperator(operator);
        log.setRemark(remark);
        save(log);
    }

    @Override
    public List<ReservationLog> getLogsByReservationId(Long reservationId) {
        LambdaQueryWrapper<ReservationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReservationLog::getReservationId, reservationId)
               .orderByDesc(ReservationLog::getCreateTime);
        return list(wrapper);
    }
}
