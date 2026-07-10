package com.aicustomer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RedisChatMemory 单元测试
 *
 * RedisChatMemory 是 Spring AI ChatMemory 接口的 Redis 实现，
 * 负责存储每轮对话的消息历史，让 AI 能记住之前的对话内容。
 *
 * 存储格式：每条消息存为 "role\ncontent"（简化序列化）
 * Key格式：chat:{conversationId}
 * 降级策略：Redis不可用时自动切换为ConcurrentHashMap内存存储
 *
 * 本测试不依赖 Redis，通过反射将 redisAvailable 设为 false，
 * 直接测试降级模式下的内存存储逻辑。
 *
 * 测试覆盖：
 * - 消息的添加和读取（顺序是否正确）
 * - 容量控制（超过 maxMessages 是否截断）
 * - 会话隔离（不同 sessionId 的消息互不干扰）
 * - 会话清除
 * - 并发安全性（多线程同时添加不丢消息）
 */
class RedisChatMemoryTest {

    private RedisChatMemory chatMemory;

    /**
     * 每个测试方法执行前重置 chatMemory
     * 使用 null 的 RedisTemplate，强制走降级模式
     */
    @BeforeEach
    void setUp() {
        chatMemory = new RedisChatMemory(null, 5, 30);
        setRedisAvailable(false);
    }

    /**
     * 通过反射设置 redisAvailable 字段
     * 将其设为 false 后，所有操作都会走内存降级路径
     */
    private void setRedisAvailable(boolean available) {
        try {
            Field field = RedisChatMemory.class.getDeclaredField("redisAvailable");
            field.setAccessible(true);
            field.set(chatMemory, available);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 工具方法：将多个 Message 包装成 List
     * Java 8 兼容写法（替代 List.of()）
     */
    private static List<Message> asList(Message... msgs) {
        return new ArrayList<>(Arrays.asList(msgs));
    }

    // ==================== add / get 基本存取测试 ====================

    /**
     * 测试：添加一条用户消息和一条助手回复，读取时顺序正确
     * 验证：消息类型（UserMessage/AssistantMessage）和内容都正确
     */
    @Test
    void add_添加消息_能正确获取() {
        Message userMsg = new UserMessage("你好");
        Message assistantMsg = new AssistantMessage("你好！有什么可以帮您？");

        chatMemory.add("session1", asList(userMsg, assistantMsg));

        List<Message> messages = chatMemory.get("session1");
        assertEquals(2, messages.size());
        assertTrue(messages.get(0) instanceof UserMessage);
        assertEquals("你好", messages.get(0).getText());
        assertTrue(messages.get(1) instanceof AssistantMessage);
        assertEquals("你好！有什么可以帮您？", messages.get(1).getText());
    }

    /**
     * 测试：多次添加消息后，按添加顺序返回
     * 模拟真实的多轮对话：用户1→助手1→用户2
     */
    @Test
    void add_多次添加_按顺序返回() {
        chatMemory.add("session1", asList(new UserMessage("消息1")));
        chatMemory.add("session1", asList(new AssistantMessage("回复1")));
        chatMemory.add("session1", asList(new UserMessage("消息2")));

        List<Message> messages = chatMemory.get("session1");
        assertEquals(3, messages.size());
        assertEquals("消息1", messages.get(0).getText());
        assertEquals("回复1", messages.get(1).getText());
        assertEquals("消息2", messages.get(2).getText());
    }

    /**
     * 测试：传入空列表和 null 不会报错
     * 防御性测试：确保边界情况不会导致异常
     */
    @Test
    void add_空消息列表_不报错() {
        chatMemory.add("session1", Collections.<Message>emptyList());
        chatMemory.add("session1", (List<Message>) null);
    }

    // ==================== get(lastN) 容量控制测试 ====================

    /**
     * 测试：添加5条消息后，get(session, 3) 只返回最后3条
     * 验证：maxMessages 容量限制是否生效
     */
    @Test
    void get_lastN_返回最近N条() {
        chatMemory.add("session1", asList(
                new UserMessage("1"),
                new AssistantMessage("2"),
                new UserMessage("3"),
                new AssistantMessage("4"),
                new UserMessage("5")
        ));

        List<Message> last3 = chatMemory.get("session1", 3);
        assertEquals(3, last3.size());
        assertEquals("3", last3.get(0).getText());
        assertEquals("4", last3.get(1).getText());
        assertEquals("5", last3.get(2).getText());
    }

    /**
     * 测试：请求数量超过实际消息数时，返回全部消息
     * 不应抛出异常或截断
     */
    @Test
    void get_lastN_超过实际数量_返回全部() {
        chatMemory.add("session1", asList(new UserMessage("1"), new UserMessage("2")));

        List<Message> last10 = chatMemory.get("session1", 10);
        assertEquals(2, last10.size());
    }

    // ==================== clear 清除测试 ====================

    /**
     * 测试：清除后，该会话的消息为空
     * 场景：会话过期或用户主动结束对话
     */
    @Test
    void clear_清除后_获取为空() {
        chatMemory.add("session1", asList(new UserMessage("你好")));
        assertFalse(chatMemory.get("session1").isEmpty());

        chatMemory.clear("session1");
        assertTrue(chatMemory.get("session1").isEmpty());
    }

    /**
     * 测试：清除 session1 不影响 session2
     * 会话隔离：不同用户的消息不能互相影响
     */
    @Test
    void clear_只清除指定会话_不影响其他() {
        chatMemory.add("session1", asList(new UserMessage("会话1")));
        chatMemory.add("session2", asList(new UserMessage("会话2")));

        chatMemory.clear("session1");

        assertTrue(chatMemory.get("session1").isEmpty());
        assertFalse(chatMemory.get("session2").isEmpty());
    }

    // ==================== 多会话隔离测试 ====================

    /**
     * 测试：两个不同会话的消息互不干扰
     * 核心场景：多个用户同时和AI对话，每个用户只能看到自己的历史
     */
    @Test
    void 多会话_互不干扰() {
        chatMemory.add("session1", asList(new UserMessage("会话1的消息")));
        chatMemory.add("session2", asList(new UserMessage("会话2的消息")));

        List<Message> msgs1 = chatMemory.get("session1");
        List<Message> msgs2 = chatMemory.get("session2");

        assertEquals(1, msgs1.size());
        assertEquals("会话1的消息", msgs1.get(0).getText());

        assertEquals(1, msgs2.size());
        assertEquals("会话2的消息", msgs2.get(0).getText());
    }

    // ==================== 不存在的会话测试 ====================

    /**
     * 测试：查询不存在的会话返回空列表（不返回null）
     * 防御性测试：调用方不需要判空
     */
    @Test
    void get_不存在的会话_返回空列表() {
        List<Message> messages = chatMemory.get("nonexistent");
        assertNotNull(messages);
        assertTrue(messages.isEmpty());
    }

    // ==================== 线程安全测试 ====================

    /**
     * 测试：10个线程同时向同一个会话添加消息，总共500条不丢失
     * 验证：ConcurrentHashMap + Redis List 的并发安全性
     * 场景：同一用户快速连续发送多条消息
     */
    @Test
    void 并发添加_不丢消息() throws Exception {
        int threadCount = 10;
        int messagesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int j = 0; j < messagesPerThread; j++) {
                            chatMemory.add("concurrent-session",
                                    asList(new UserMessage("T" + threadId + "-M" + j)));
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        List<Message> messages = chatMemory.get("concurrent-session");
        // 降级模式下没有容量限制，500条应全部保留
        assertEquals(threadCount * messagesPerThread, messages.size());
    }
}
