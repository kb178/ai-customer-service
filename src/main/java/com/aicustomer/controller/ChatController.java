package com.aicustomer.controller;

import com.aicustomer.service.FunctionCallingChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 智能客服对话控制器
 *
 * 功能说明：
 * - 提供REST API接口供前端调用
 * - 使用Function Calling模式
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    /** Function Calling模式的对话服务 */
    private final FunctionCallingChatService functionCallingChatService;

    /**
     * 发送消息接口
     */
    @PostMapping("/send")
    public ChatResponse sendMessage(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String reply = functionCallingChatService.chat(sessionId, request.getMessage());

        return new ChatResponse(sessionId, reply, "function");
    }

    /**
     * 获取会话信息接口
     */
    @GetMapping("/session/{sessionId}")
    public Map<String, Object> getSessionInfo(@PathVariable String sessionId) {
        return Map.of(
                "sessionId", sessionId,
                "status", "active"
        );
    }
}
