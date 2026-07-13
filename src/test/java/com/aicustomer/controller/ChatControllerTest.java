package com.aicustomer.controller;

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
 * - POST /api/chat/send：发送消息，使用Function Calling模式
 * - GET /api/chat/session/{sessionId}：获取会话信息
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private FunctionCallingChatService functionCallingChatService;

    @InjectMocks
    private ChatController chatController;

    // ==================== sendMessage 接口测试 ====================

    /**
     * 测试：发送消息调用FunctionCallingChatService
     */
    @Test
    void sendMessage_调用functionCallingChatService() {
        ChatRequest request = new ChatRequest();
        request.setMessage("查询课程");

        when(functionCallingChatService.chat(anyString(), eq("查询课程"))).thenReturn("为您找到以下课程...");

        ChatResponse response = chatController.sendMessage(request);

        assertNotNull(response);
        assertEquals("为您找到以下课程...", response.getReply());
        assertEquals("function", response.getMode());
        verify(functionCallingChatService).chat(anyString(), eq("查询课程"));
    }

    /**
     * 测试：不传 sessionId 时，自动生成 UUID
     */
    @Test
    void sendMessage_不传sessionId_自动生成UUID() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setSessionId(null);

        when(functionCallingChatService.chat(anyString(), eq("你好"))).thenReturn("回复");

        ChatResponse response = chatController.sendMessage(request);

        assertNotNull(response.getSessionId());
        assertFalse(response.getSessionId().isEmpty());
        assertTrue(response.getSessionId().matches("[0-9a-f-]{36}"));
    }

    /**
     * 测试：传入 sessionId 时，原样使用
     */
    @Test
    void sendMessage_传入sessionId_使用传入的值() {
        ChatRequest request = new ChatRequest();
        request.setMessage("你好");
        request.setSessionId("my-session-123");

        when(functionCallingChatService.chat(eq("my-session-123"), eq("你好"))).thenReturn("回复");

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
