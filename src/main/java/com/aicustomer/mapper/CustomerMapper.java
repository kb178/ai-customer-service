package com.aicustomer.mapper;

import com.aicustomer.entity.Customer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户Mapper接口
 * 
 * 功能：提供客户数据的CRUD操作
 * 
 * 继承BaseMapper，已内置以下方法：
 * - insert: 新增客户
 * - deleteById: 根据ID删除客户
 * - updateById: 根据ID更新客户
 * - selectById: 根据ID查询客户
 * - selectList: 查询客户列表
 * - selectPage: 分页查询客户
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
