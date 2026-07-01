package com.aicustomer.service;

/**
 * 对话服务接口
 * 
 * 功能：定义智能客服对话的核心方法
 * 
 * 主要职责：
 * - 处理用户消息
 * - 调用AI模型生成回复
 * - 管理会话上下文
 */
public interface ChatService {

    /**
     * 处理对话消息
     * 
     * 功能：接收用户消息，结合上下文调用AI生成回复
     * 
     * @param sessionId 会话ID（用于保持对话上下文）
     * @param message 用户消息内容
     * @return AI生成的回复内容
     */
    String chat(String sessionId, String message);
}
