package com.aicustomer.service.impl;

import com.aicustomer.entity.Customer;
import com.aicustomer.mapper.CustomerMapper;
import com.aicustomer.service.CustomerService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 客户服务实现类
 * 
 * 功能：实现CustomerService接口定义的客户相关业务方法
 */
@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {

    /**
     * 保存客户信息（新增或更新）
     * 
     * 实现逻辑：
     * 1. 根据手机号查询是否已存在该客户
     * 2. 已存在：更新客户的各项信息
     * 3. 不存在：新增客户记录
     * 
     * @param customer 客户信息
     * @return 保存后的客户记录
     */
    @Override
    public Customer saveCustomer(Customer customer) {
        // 根据手机号查询是否已存在
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Customer::getPhone, customer.getPhone());
        Customer existing = getOne(wrapper);
        
        if (existing != null) {
            // 已存在，更新信息
            existing.setName(customer.getName());
            existing.setEmail(customer.getEmail());
            existing.setAge(customer.getAge());
            existing.setEducation(customer.getEducation());
            existing.setInterest(customer.getInterest());
            updateById(existing);
            return existing;
        } else {
            // 不存在，新增记录
            save(customer);
            return customer;
        }
    }
}
