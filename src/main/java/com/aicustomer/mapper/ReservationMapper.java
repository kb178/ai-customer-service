package com.aicustomer.mapper;

import com.aicustomer.entity.Reservation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 预约Mapper接口
 * 
 * 功能：提供预约数据的CRUD操作
 * 
 * 继承BaseMapper，已内置以下方法：
 * - insert: 新增预约
 * - deleteById: 根据ID删除预约
 * - updateById: 根据ID更新预约
 * - selectById: 根据ID查询预约
 * - selectList: 查询预约列表
 * - selectPage: 分页查询预约
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {
}
