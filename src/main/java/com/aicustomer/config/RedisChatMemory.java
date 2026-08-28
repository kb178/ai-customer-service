package com.aicustomer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Redis的Spring AI ChatMemory实现
 *
 * 存储格式：每条消息存为 "role\ncontent"（简化序列化，避免Message接口反序列化问题）
 * Key格式：chat:{conversationId}
 * 过期时间：与SessionContext一致（30分钟）
 *
 * 降级策略：Redis不可用时自动切换为内存存储
 */
@Slf4j
public class RedisChatMemory implements ChatMemory {

    private static final String KEY_PREFIX = "chat:";

    private final RedisTemplate<String, String> redisTemplate;
    private final int maxMessages;
    private final Duration timeout;

    /** 降级：内存存储 */
    private final Map<String, List<Message>> fallbackStore = new ConcurrentHashMap<>();

    /** Redis是否可用 */
    private volatile boolean redisAvailable = true;
    private int failCount = 0;
    private static final int FAIL_THRESHOLD = 3;

    public RedisChatMemory(RedisTemplate<String, String> redisTemplate, int maxMessages, int timeoutMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxMessages = maxMessages;
        this.timeout = Duration.ofMinutes(timeoutMinutes);
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) return;

        if (redisAvailable) {
            try {
                String key = KEY_PREFIX + conversationId;
                for (Message msg : messages) {
                    String role = getRole(msg);
                    String content = msg.getText();
                    // 用分隔符存储：role + \n + content
                    String stored = role + "\n" + (content != null ? content : "");
                    redisTemplate.opsForList().rightPush(key, stored);
                }
                // 保留最近maxMessages条
                Long size = redisTemplate.opsForList().size(key);
                if (size != null && size > maxMessages) {
                    redisTemplate.opsForList().trim(key, size - maxMessages, -1);
                }
                // 续期
                redisTemplate.expire(key, timeout);
                failCount = 0;
                return;
            } catch (Exception e) {
                handleFailure(e);
            }
        }

        // 降级
        fallbackStore.computeIfAbsent(conversationId, k -> new ArrayList<>()).addAll(messages);
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> allMessages = get(conversationId);
        if (allMessages.size() <= lastN) {
            return allMessages;
        }
        return allMessages.subList(allMessages.size() - lastN, allMessages.size());
    }


    public List<Message> get(String conversationId) {
        if (redisAvailable) {
            try {
                String key = KEY_PREFIX + conversationId;
                List<String> storedList = redisTemplate.opsForList().range(key, 0, -1);
                failCount = 0;
                if (storedList == null) return new ArrayList<>();

                List<Message> messages = new ArrayList<>();
                for (String stored : storedList) {
                    Message msg = parseMessage(stored);
                    if (msg != null) {
                        messages.add(msg);
                    }
                }
                return messages;
            } catch (Exception e) {
                handleFailure(e);
            }
        }

        // 降级
        return fallbackStore.getOrDefault(conversationId, new ArrayList<>());
    }

    @Override
    public void clear(String conversationId) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(KEY_PREFIX + conversationId);
                failCount = 0;
            } catch (Exception e) {
                handleFailure(e);
            }
        }
        fallbackStore.remove(conversationId);
    }

    /**
     * 从存储字符串解析出Message对象
     * 格式："role\ncontent"
     */
    private Message parseMessage(String stored) {
        if (stored == null || stored.isEmpty()) return null;
        int newlineIdx = stored.indexOf('\n');
        if (newlineIdx < 0) return null;

        String role = stored.substring(0, newlineIdx);
        String content = stored.substring(newlineIdx + 1);

        switch (role) {
            case "user": return new UserMessage(content);
            case "assistant": return new AssistantMessage(content);
            case "system": return new SystemMessage(content);
            default: return new UserMessage(content);
        }
    }

    /**
     * 获取Message的角色标识
     */
    private String getRole(Message message) {
        if (message instanceof UserMessage) return "user";
        if (message instanceof AssistantMessage) return "assistant";
        if (message instanceof SystemMessage) return "system";
        return "user";
    }

    private void handleFailure(Exception e) {
        failCount++;
        log.warn("Redis ChatMemory操作失败，失败次数: {} - {}", failCount, e.getMessage());
        if (failCount >= FAIL_THRESHOLD) {
            redisAvailable = false;
            log.warn("Redis ChatMemory连续失败{}次，已切换到内存降级模式", FAIL_THRESHOLD);
        }
    }
}
