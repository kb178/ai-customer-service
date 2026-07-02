package com.aicustomer.service.impl;

import com.aicustomer.config.BizConstants;
import com.aicustomer.entity.Reservation;
import com.aicustomer.mapper.ReservationMapper;
import com.aicustomer.service.ReservationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 预约服务实现类
 * 
 * 功能：实现ReservationService接口定义的预约相关业务方法
 */
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements ReservationService {

    /**
     * 创建预约
     * 
     * 实现逻辑：
     * 1. 设置预约状态为待确认（0）
     * 2. 保存预约记录到数据库
     * 
     * @param reservation 预约信息
     * @return 创建成功的预约记录
     */
    @Override
    public Reservation createReservation(Reservation reservation) {
        // 设置初始状态为待确认
        reservation.setStatus(BizConstants.STATUS_PENDING);
        // 保存到数据库
        save(reservation);
        return reservation;
    }
}
