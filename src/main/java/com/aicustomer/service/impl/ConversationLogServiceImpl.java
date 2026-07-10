package com.aicustomer.service.impl;

import com.aicustomer.entity.ConversationLog;
import com.aicustomer.mapper.ConversationLogMapper;
import com.aicustomer.service.ConversationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversationLogServiceImpl extends ServiceImpl<ConversationLogMapper, ConversationLog> implements ConversationLogService {

    @Override
    public void saveLog(String sessionId, String customerPhone, String role, String content) {
        ConversationLog log = new ConversationLog();
        log.setSessionId(sessionId);
        log.setCustomerPhone(customerPhone);
        log.setRole(role);
        log.setContent(content);
        save(log);
    }

    @Override
    public IPage<Map<String, Object>> selectSessionList(IPage<Map<String, Object>> page, String phone, String sessionId, LocalDateTime startDate, LocalDateTime endDate) {
        LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(phone), ConversationLog::getCustomerPhone, phone)
               .eq(StringUtils.hasText(sessionId), ConversationLog::getSessionId, sessionId)
               .ge(startDate != null, ConversationLog::getCreateTime, startDate)
               .le(endDate != null, ConversationLog::getCreateTime, endDate);

        // 查所有符合条件的记录，Java Stream 聚合
        List<ConversationLog> all = list(wrapper);
        Map<String, List<ConversationLog>> grouped = all.stream()
                .collect(Collectors.groupingBy(ConversationLog::getSessionId));

        List<Map<String, Object>> records = grouped.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("sessionId", entry.getKey());
                    map.put("customerPhone", entry.getValue().get(0).getCustomerPhone());
                    map.put("messageCount", entry.getValue().size());
                    map.put("firstTime", entry.getValue().stream().map(ConversationLog::getCreateTime).min(LocalDateTime::compareTo).orElse(null));
                    map.put("lastTime", entry.getValue().stream().map(ConversationLog::getCreateTime).max(LocalDateTime::compareTo).orElse(null));
                    return map;
                })
                .sorted((a, b) -> {
                    LocalDateTime t1 = (LocalDateTime) b.get("lastTime");
                    LocalDateTime t2 = (LocalDateTime) a.get("lastTime");
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return t1.compareTo(t2);
                })
                .collect(Collectors.toList());

        // 手动分页
        int total = records.size();
        int from = (int) ((page.getCurrent() - 1) * page.getSize());
        int to = Math.min(from + (int) page.getSize(), records.size());
        List<Map<String, Object>> pageRecords = from < total ? records.subList(from, to) : Collections.emptyList();

        return new Page<Map<String, Object>>(page.getCurrent(), page.getSize(), total).setRecords(pageRecords);
    }
}
