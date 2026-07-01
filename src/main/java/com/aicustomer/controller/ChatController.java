package com.aicustomer.controller;

import com.aicustomer.service.ChatService;
import com.aicustomer.service.FunctionCallingChatService;
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
     *
     * 请求参数：
     * - message: 用户消息内容
     * - sessionId: 会话ID（可选）
     * - mode: 对话模式（可选，instruction-指令解析模式，function-Function Calling模式，默认instruction）
     *
     * 返回：
     * - sessionId: 会话ID
     * - reply: AI回复内容
     * - mode: 使用的模式
     */
    @PostMapping("/send")
    public Map<String, String> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());
        String mode = request.getOrDefault("mode", "instruction");

        String reply;
        if ("function".equals(mode)) {
            reply = functionCallingChatService.chat(sessionId, message);
        } else {
            reply = chatService.chat(sessionId, message);
        }

        return Map.of(
                "sessionId", sessionId,
                "reply", reply,
                "mode", mode
        );
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
