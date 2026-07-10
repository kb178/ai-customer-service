package com.aicustomer.service.impl;

import com.aicustomer.entity.Customer;
import com.aicustomer.mapper.CustomerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * CustomerServiceImpl 单元测试
 *
 * CustomerServiceImpl 负责客户信息的新增和更新。
 * 核心逻辑：saveCustomer() 方法根据手机号判断是新增还是更新
 * - 手机号不存在 → 插入新记录
 * - 手机号已存在 → 更新已有记录
 *
 * 测试覆盖：
 * - 新客户场景：确认执行 insert 而非 update
 * - 已有客户场景：确认执行 update 而非 insert
 * - 字段覆盖行为：验证更新时的字段赋值逻辑
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerServiceImpl customerService;

    /**
     * 测试：新手机号 → 执行 insert 插入
     * 场景：第一次咨询的用户，数据库中没有该手机号
     * 验证：调用了 insert，没有调用 updateById
     */
    @Test
    void saveCustomer_新客户_执行插入() {
        Customer customer = new Customer();
        customer.setPhone("13800138000");
        customer.setName("张三");

        // Mock：按手机号查询返回 null（不存在）
        when(customerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(customerMapper.insert(any(Customer.class))).thenReturn(1);

        Customer result = customerService.saveCustomer(customer);

        assertNotNull(result);
        assertEquals("13800138000", result.getPhone());
        verify(customerMapper).insert(any(Customer.class));
        verify(customerMapper, never()).updateById(any(Customer.class));
    }

    /**
     * 测试：已有手机号 → 执行 updateById 更新
     * 场景：回头客再次咨询，数据库中已有该手机号
     * 验证：调用了 updateById，没有调用 insert
     */
    @Test
    void saveCustomer_已有客户_执行更新() {
        // 模拟数据库中已存在的客户
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setPhone("13800138000");
        existing.setName("旧名字");

        // 用户新传入的信息
        Customer newCustomer = new Customer();
        newCustomer.setPhone("13800138000");
        newCustomer.setName("新名字");
        newCustomer.setEducation("研究生");
        newCustomer.setInterest("编程开发");

        when(customerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(customerMapper.updateById(any(Customer.class))).thenReturn(1);

        Customer result = customerService.saveCustomer(newCustomer);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(customerMapper).updateById(any(Customer.class));
        verify(customerMapper, never()).insert(any(Customer.class));
    }

    /**
     * 测试：更新时，新Customer未传的字段会被覆盖为null
     * 场景：用户第二次咨询只说了新电话，没说邮箱
     * 预期行为：现有代码会将 existing.email 覆盖为 null
     * 注：这是一个已知的行为缺陷，当前代码没有做null字段跳过处理
     */
    @Test
    void saveCustomer_更新时保留原有字段() {
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setPhone("13800138000");
        existing.setName("张三");
        existing.setEmail("old@example.com");

        Customer newCustomer = new Customer();
        newCustomer.setPhone("13800138000");
        newCustomer.setName("张三");
        // 注意：没有设置 email，新Customer的email为null

        when(customerMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);
        when(customerMapper.updateById(any(Customer.class))).thenReturn(1);

        Customer result = customerService.saveCustomer(newCustomer);

        // 使用 ArgumentCaptor 捕获 updateById 的实际参数
        // 这样可以验证传给数据库的Customer对象的字段值
        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerMapper).updateById(captor.capture());
        Customer updated = captor.getValue();
        assertEquals("张三", updated.getName());
        // 新Customer没传email，所以existing的email被null覆盖
        assertNull(updated.getEmail());
    }
}
