package com.aicustomer.mapper;

import com.aicustomer.entity.Campus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 校区Mapper接口
 * 
 * 功能：提供校区数据的CRUD操作
 * 
 * 继承BaseMapper，已内置以下方法：
 * - insert: 新增校区
 * - deleteById: 根据ID删除校区
 * - updateById: 根据ID更新校区
 * - selectById: 根据ID查询校区
 * - selectList: 查询校区列表
 * - selectPage: 分页查询校区
 */
@Mapper
public interface CampusMapper extends BaseMapper<Campus> {
}
