package com.aicustomer.controller;

import com.aicustomer.service.ChatService;
import com.aicustomer.service.FunctionCallingChatService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ChatController 单元测试
 *
 * ChatController 是对外的 REST API 入口，提供：
 * - POST /api/chat/send：发送消息，支持 instruction 和 function 两种模式
 * - GET /api/chat/session/{sessionId}：获取会话信息
 *
 * 测试覆盖：
 * - 模式路由：不同 mode 值是否调用了正确的 Service
 * - sessionId 处理：不传自动生成UUID，传了原样使用
 * - 响应格式：返回的 ChatResponse 包含必要字段
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock(name = "functionCallingChatService")
    private FunctionCallingChatService functionCallingChatService;

    @InjectMocks
    private ChatController chatController;

    // ==================== sendMessage 接口测试 ====================

    /**
     * 测试：mode=instruction 时，调用 ChatService（指令解析模式）
     * 验证：chatService.chat() 被调用，functionCallingChatService 未被调用
     */
    @Test
    void sendMessage_默认模式_调用chatService() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setMode("instruction");

        when(chatService.chat(anyString(), eq("你好"))).thenReturn("你好！有什么可以帮您？");

        ChatResponse response = chatController.sendMessage(request);

        assertNotNull(response);
        assertNotNull(response.getSessionId());
        assertEquals("你好！有什么可以帮您？", response.getReply());
        assertEquals("instruction", response.getMode());
        verify(chatService).chat(anyString(), eq("你好"));
        verify(functionCallingChatService, never()).chat(anyString(), anyString());
    }

    /**
     * 测试：mode=function 时，调用 FunctionCallingChatService（Function Calling模式）
     * 两种模式使用不同的AI调用策略
     */
    @Test
    void sendMessage_function模式_调用functionCallingChatService() {
        ChatRequest request = new ChatRequest();
        request.setMessage("查询课程");
        request.setMode("function");

        when(functionCallingChatService.chat(anyString(), eq("查询课程"))).thenReturn("为您找到以下课程...");

        ChatResponse response = chatController.sendMessage(request);

        assertNotNull(response);
        assertEquals("为您找到以下课程...", response.getReply());
        assertEquals("function", response.getMode());
        verify(functionCallingChatService).chat(anyString(), eq("查询课程"));
        verify(chatService, never()).chat(anyString(), anyString());
    }

    /**
     * 测试：不传 sessionId 时，自动生成 UUID
     * 新用户第一次对话时不需要传sessionId
     */
    @Test
    void sendMessage_不传sessionId_自动生成UUID() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setSessionId(null);

        when(chatService.chat(anyString(), eq("你好"))).thenReturn("回复");

        ChatResponse response = chatController.sendMessage(request);

        assertNotNull(response.getSessionId());
        assertFalse(response.getSessionId().isEmpty());
        // 验证生成的是合法的UUID格式（36位，含4个短横线）
        assertTrue(response.getSessionId().matches("[0-9a-f-]{36}"));
    }

    /**
     * 测试：传入 sessionId 时，原样使用
     * 前端维护会话状态时，会回传之前的 sessionId
     */
    @Test
    void sendMessage_传入sessionId_使用传入的值() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setSessionId("my-session-123");

        when(chatService.chat(eq("my-session-123"), eq("你好"))).thenReturn("回复");

        ChatResponse response = chatController.sendMessage(request);

        assertEquals("my-session-123", response.getSessionId());
    }

    // ==================== getSessionInfo 接口测试 ====================

    /**
     * 测试：获取会话信息返回 sessionId 和 active 状态
     */
    @Test
    void getSessionInfo_返回会话状态() {
        Map<String, Object> info = chatController.getSessionInfo("test-session");

        assertEquals("test-session", info.get("sessionId"));
        assertEquals("active", info.get("status"));
    }
}
