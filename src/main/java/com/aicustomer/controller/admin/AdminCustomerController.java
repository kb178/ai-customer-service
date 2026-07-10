package com.aicustomer.controller.admin;

import com.aicustomer.entity.Customer;
import com.aicustomer.entity.Reservation;
import com.aicustomer.service.CustomerService;
import com.aicustomer.service.ReservationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/customer")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerService customerService;
    private final ReservationService reservationService;

    @GetMapping("/list")
    public AdminResponse<PageResult<Customer>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<Customer> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(name), Customer::getName, name)
               .like(StringUtils.hasText(phone), Customer::getPhone, phone)
               .orderByDesc(Customer::getCreateTime);
        Page<Customer> result = customerService.page(pageParam, wrapper);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public AdminResponse<Customer> detail(@PathVariable Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null) {
            return AdminResponse.error(404, "客户不存在");
        }
        return AdminResponse.ok(customer);
    }

    @GetMapping("/{id}/reservations")
    public AdminResponse<List<Reservation>> reservations(@PathVariable Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null) {
            return AdminResponse.error(404, "客户不存在");
        }
        List<Reservation> reservations = reservationService.lambdaQuery()
                .eq(Reservation::getPhone, customer.getPhone())
                .orderByDesc(Reservation::getCreateTime)
                .list();
        return AdminResponse.ok(reservations);
    }
}
