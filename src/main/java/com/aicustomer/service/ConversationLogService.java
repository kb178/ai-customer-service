package com.aicustomer.service;

import com.aicustomer.entity.ConversationLog;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDateTime;
import java.util.Map;

public interface ConversationLogService extends IService<ConversationLog> {

    void saveLog(String sessionId, String customerPhone, String role, String content);

    IPage<Map<String, Object>> selectSessionList(IPage<Map<String, Object>> page, String phone, String sessionId, LocalDateTime startDate, LocalDateTime endDate);
}
