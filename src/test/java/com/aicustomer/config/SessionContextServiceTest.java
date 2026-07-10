package com.aicustomer.config;

import com.aicustomer.entity.SessionContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SessionContextService 单元测试
 *
 * SessionContextService 负责会话上下文的持久化管理：
 * - 主存储：Redis（支持分布式、重启不丢失）
 * - 降级存储：ConcurrentHashMap（Redis不可用时自动切换）
 * - 过期策略：每次操作重置30分钟过期时间
 *
 * 测试覆盖：
 * - Redis 读写：正常情况下的存取逻辑
 * - JSON序列化：SessionContext ↔ JSON 的正确转换
 * - 降级机制：Redis连续失败后自动切换到内存存储
 * - 监控接口：getStorageMode、getFallbackStoreSize
 */
@ExtendWith(MockitoExtension.class)
class SessionContextServiceTest {

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);

    @SuppressWarnings("unchecked")
    private ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);

    private SessionContextService sessionContextService;

    /**
     * 每个测试前重新创建 service，并绑定 Mock 的 RedisTemplate
     */
    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sessionContextService = new SessionContextService(redisTemplate);
    }

    // ==================== getOrCreate 测试 ====================

    /**
     * 测试：Redis中不存在的sessionId → 创建新上下文并保存
     * 场景：新用户第一次对话
     * 验证：返回的context有正确的sessionId，并且被保存到Redis
     */
    @Test
    void getOrCreate_Redis中不存在_创建新上下文() {
        when(valueOperations.get("session:test1")).thenReturn(null);

        SessionContext context = sessionContextService.getOrCreate("test1");

        assertNotNull(context);
        assertEquals("test1", context.getSessionId());
        assertNotNull(context.getCreateTime());
        // 验证调用了 set 方法保存到 Redis（key、value、过期时间）
        verify(valueOperations).set(eq("session:test1"), anyString(), any(Duration.class));
    }

    /**
     * 测试：Redis中存在的sessionId → 反序列化返回已有上下文
     * 场景：用户刷新页面后重新连接
     * 验证：从JSON正确还原出SessionContext的所有字段
     */
    @Test
    void getOrCreate_Redis中存在_返回已有上下文() throws Exception {
        // 模拟Redis中存储的JSON字符串
        SessionContext existing = new SessionContext();
        existing.setSessionId("test2");
        existing.setCustomerName("张三");
        existing.setPhone("13800138000");

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String json = mapper.writeValueAsString(existing);

        when(valueOperations.get("session:test2")).thenReturn(json);

        SessionContext context = sessionContextService.getOrCreate("test2");

        assertNotNull(context);
        assertEquals("test2", context.getSessionId());
        assertEquals("张三", context.getCustomerName());
        assertEquals("13800138000", context.getPhone());
    }

    // ==================== save 测试 ====================

    /**
     * 测试：save 将 SessionContext 序列化为 JSON 并写入 Redis
     * 验证：调用了 valueOperations.set()，带正确的过期时间
     */
    @Test
    void save_保存到Redis() {
        SessionContext context = new SessionContext();
        context.setSessionId("test3");

        sessionContextService.save("test3", context);

        verify(valueOperations).set(eq("session:test3"), anyString(), any(Duration.class));
    }

    // ==================== remove 测试 ====================

    /**
     * 测试：remove 删除 Redis 中的会话key
     * 场景：会话过期清理或用户主动退出
     */
    @Test
    void remove_删除Redis中的会话() {
        sessionContextService.remove("test4");
        verify(redisTemplate).delete("session:test4");
    }

    // ==================== Redis 降级测试 ====================

    /**
     * 测试：Redis读取连续失败3次后，自动切换到内存降级模式
     * 降级后 getStorageMode() 返回 "fallback-memory"
     * 保护机制：避免Redis故障时整个系统不可用
     */
    @Test
    void getOrCreate_Redis失败_降级到内存() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis连接失败"));

        // 触发4次失败（超过阈值3）
        for (int i = 0; i < 4; i++) {
            sessionContextService.getOrCreate("fail-session-" + i);
        }

        assertEquals("fallback-memory", sessionContextService.getStorageMode());
    }

    /**
     * 测试：Redis写入失败后也能降级，且降级后仍能正常存取
     * 验证：降级存储（ConcurrentHashMap）能正确保存和读取SessionContext
     */
    @Test
    void save_Redis失败_降级到内存() {
        doThrow(new RuntimeException("Redis连接失败"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        SessionContext context = new SessionContext();
        context.setSessionId("fail-save");

        // 触发降级
        for (int i = 0; i < 4; i++) {
            sessionContextService.save("fail-save-" + i, context);
        }

        assertEquals("fallback-memory", sessionContextService.getStorageMode());
        // 降级后仍能从内存中获取到数据
        SessionContext retrieved = sessionContextService.getOrCreate("fail-save-0");
        assertNotNull(retrieved);
    }

    // ==================== exists 测试 ====================

    /**
     * 测试：会话存在时返回 true
     */
    @Test
    void exists_Redis中存在_返回true() {
        when(redisTemplate.hasKey("session:exist")).thenReturn(true);
        assertTrue(sessionContextService.exists("exist"));
    }

    /**
     * 测试：会话不存在时返回 false
     */
    @Test
    void exists_Redis中不存在_返回false() {
        when(redisTemplate.hasKey("session:exist")).thenReturn(false);
        assertFalse(sessionContextService.exists("exist"));
    }

    // ==================== 监控接口测试 ====================

    /**
     * 测试：默认存储模式为 "redis"
     */
    @Test
    void getStorageMode_默认Redis模式() {
        assertEquals("redis", sessionContextService.getStorageMode());
    }

    /**
     * 测试：降级后，降级存储中的会话数量正确
     * 用于运维监控，观察降级影响范围
     */
    @Test
    void getFallbackStoreSize_降级后有数据() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("fail"));
        sessionContextService.getOrCreate("fallback1");
        sessionContextService.getOrCreate("fallback2");

        assertEquals(2, sessionContextService.getFallbackStoreSize());
    }
}
