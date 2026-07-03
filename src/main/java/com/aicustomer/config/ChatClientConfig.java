package com.aicustomer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * ChatClient配置类
 *
 * 功能：配置Spring AI的ChatClient，启用对话记忆功能
 * - 对话历史存储在Redis中，支持重启不丢失
 * - Redis不可用时自动降级为内存存储
 */
@Configuration
public class ChatClientConfig {

    /**
     * 配置ChatMemory（对话记忆）
     *
     * 使用Redis存储对话历史，30分钟过期
     * Redis不可用时自动降级为内存存储
     */
    @Bean
    public ChatMemory chatMemory(@Qualifier("stringRedisTemplate") RedisTemplate<String, String> redisTemplate,
                                  @Value("${session.timeout-minutes:30}") int timeoutMinutes) {
        return new RedisChatMemory(redisTemplate, 20, timeoutMinutes);
    }

    /**
     * 配置ChatClient
     *
     * @param chatModel AI对话模型
     * @param chatMemory 对话记忆
     */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .build();
    }
}
