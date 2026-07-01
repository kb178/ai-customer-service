package com.aicustomer.service;

import com.aicustomer.entity.Reservation;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 预约服务接口
 * 
 * 功能：定义预约相关的业务方法
 * 
 * 继承IService，已内置以下方法：
 * - save: 新增预约
 * - updateById: 更新预约
 * - removeById: 删除预约
 * - getById: 根据ID查询
 * - list: 查询列表
 * - page: 分页查询
 */
public interface ReservationService extends IService<Reservation> {

    /**
     * 创建预约
     * 
     * 功能：新建一条预约记录，默认状态为待确认
     * 
     * @param reservation 预约信息
     * @return 创建成功的预约记录（包含生成的ID）
     */
    Reservation createReservation(Reservation reservation);
}
