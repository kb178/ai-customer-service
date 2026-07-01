package com.aicustomer.service;

/**
 * Function Calling模式的对话服务接口
 */
public interface FunctionCallingChatService {

    /**
     * 处理对话消息（Function Calling模式）
     *
     * @param sessionId 会话ID
     * @param message 用户消息
     * @return AI回复内容
     */
    String chat(String sessionId, String message);
}
