package com.aicustomer.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient配置类
 *
 * 功能：配置Spring AI的ChatClient，启用对话记忆功能
 */
@Configuration
public class ChatClientConfig {

    /**
     * 配置ChatMemory（对话记忆）
     *
     * 使用InMemoryChatMemory将对话历史存储在内存中
     * 生产环境可替换为RedisChatMemory或JdbcChatMemory
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
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
