package com.aicustomer.controller.admin;

import com.aicustomer.entity.ConversationLog;
import com.aicustomer.service.ConversationLogService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/conversation")
@RequiredArgsConstructor
public class AdminConversationController {

    private final ConversationLogService conversationLogService;

    @GetMapping("/list")
    public AdminResponse<PageResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        if (page < 1) page = 1;
        if (size < 1) size = 10;
        if (size > 100) size = 100;

        LocalDateTime start = StringUtils.hasText(startDate) ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime end = StringUtils.hasText(endDate) ? LocalDate.parse(endDate).atTime(23, 59, 59) : null;

        IPage<Map<String, Object>> pageParam = new Page<>(page, size);
        IPage<Map<String, Object>> result = conversationLogService.selectSessionList(pageParam, phone, sessionId, start, end);
        return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/session/{sessionId}")
    public AdminResponse<List<ConversationLog>> sessionDetail(@PathVariable String sessionId) {
        List<ConversationLog> logs = conversationLogService.lambdaQuery()
                .eq(ConversationLog::getSessionId, sessionId)
                .orderByAsc(ConversationLog::getCreateTime)
                .list();
        return AdminResponse.ok(logs);
    }
}
