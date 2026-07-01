package com.aicustomer.service;

import com.aicustomer.entity.Customer;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 客户服务接口
 * 
 * 功能：定义客户相关的业务方法
 * 
 * 继承IService，已内置以下方法：
 * - save: 新增客户
 * - updateById: 更新客户
 * - removeById: 删除客户
 * - getById: 根据ID查询
 * - list: 查询列表
 * - page: 分页查询
 */
public interface CustomerService extends IService<Customer> {

    /**
     * 保存客户信息
     * 
     * 功能：根据手机号判断客户是否已存在
     *        - 已存在：更新客户信息
     *        - 不存在：新增客户
     * 
     * @param customer 客户信息
     * @return 保存后的客户记录
     */
    Customer saveCustomer(Customer customer);
}
