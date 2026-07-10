package com.aicustomer.controller.admin;

import com.aicustomer.entity.LeaveMessage;
import com.aicustomer.service.LeaveMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/leave-message")
@RequiredArgsConstructor
public class AdminLeaveMessageController {

    private final LeaveMessageService leaveMessageService;

    @GetMapping("/list")
    public AdminResponse<PageResult<LeaveMessage>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        Page<LeaveMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<LeaveMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, LeaveMessage::getStatus, status)
               .orderByDesc(LeaveMessage::getCreateTime);
        Page<LeaveMessage> result = leaveMessageService.page(pageParam, wrapper);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/{id}")
    public AdminResponse<LeaveMessage> detail(@PathVariable Long id) {
        LeaveMessage msg = leaveMessageService.getById(id);
        if (msg == null) {
            return AdminResponse.error(404, "留言不存在");
        }
        return AdminResponse.ok(msg);
    }

    @PutMapping("/{id}/handle")
    public AdminResponse<Void> handle(@PathVariable Long id, @RequestBody HandleRequest request) {
        LeaveMessage msg = leaveMessageService.getById(id);
        if (msg == null) {
            return AdminResponse.error(404, "留言不存在");
        }
        msg.setHandler(request.getHandler());
        msg.setHandleRemark(request.getHandleRemark());
        msg.setStatus(request.getStatus());
        msg.setHandleTime(LocalDateTime.now());
        leaveMessageService.updateById(msg);
        return AdminResponse.ok();
    }
}
