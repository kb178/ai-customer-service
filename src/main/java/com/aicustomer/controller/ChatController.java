package com.aicustomer.controller;

import com.aicustomer.service.ChatService;
import com.aicustomer.service.FunctionCallingChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 智能客服对话控制器
 *
 * 功能说明：
 * - 提供REST API接口供前端调用
 * - 支持两种模式：指令解析模式、Function Calling模式
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    /** 指令解析模式的对话服务 */
    private final ChatService chatService;

    /** Function Calling模式的对话服务 */
    @Qualifier("functionCallingChatService")
    private final FunctionCallingChatService functionCallingChatService;

    /**
     * 发送消息接口
     */
    @PostMapping("/send")
    public ChatResponse sendMessage(@Valid @RequestBody ChatRequest request) {
        String sessionId = request.getSessionId() != null
                ? request.getSessionId()
                : UUID.randomUUID().toString();

        String reply;
        if ("function".equals(request.getMode())) {
            reply = functionCallingChatService.chat(sessionId, request.getMessage());
        } else {
            reply = chatService.chat(sessionId, request.getMessage());
        }

        return new ChatResponse(sessionId, reply, request.getMode());
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
