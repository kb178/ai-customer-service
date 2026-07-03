package com.aicustomer.config;

import com.aicustomer.entity.SessionContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话上下文存储服务
 *
 * 主存储：Redis（支持分布式、重启不丢失）
 * 降级存储：ConcurrentHashMap（Redis不可用时自动切换）
 *
 * 会话过期策略：
 * - 每次有新消息时重置过期时间（30分钟）
 * - 无操作30分钟后自动过期清理
 */
@Slf4j
@Component
public class SessionContextService {

    private static final String REDIS_KEY_PREFIX = "session:";

    private final RedisTemplate<String, Object> redisTemplate;

    private final ObjectMapper objectMapper;

    /** 降级：内存存储 */
    private final Map<String, SessionContext> fallbackStore = new ConcurrentHashMap<>();

    /** Redis是否可用的标志 */
    private volatile boolean redisAvailable = true;

    /** 连续失败计数，超过阈值后切换到降级模式 */
    private int failCount = 0;
    private static final int FAIL_THRESHOLD = 3;

    @Value("${session.timeout-minutes:30}")
    private int timeoutMinutes;

    public SessionContextService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 获取会话上下文，不存在则创建新的
     */
    public SessionContext getOrCreate(String sessionId) {
        // 尝试从Redis读取
        if (redisAvailable) {
            try {
                Object raw = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + sessionId);
                if (raw != null) {
                    failCount = 0;
                    SessionContext context;
                    if (raw instanceof SessionContext sc) {
                        context = sc;
                    } else if (raw instanceof String json) {
                        context = objectMapper.readValue(json, SessionContext.class);
                    } else {
                        context = objectMapper.readValue(raw.toString(), SessionContext.class);
                    }
                    // 续期
                    renewExpiration(sessionId);
                    return context;
                }
                // Redis中不存在，创建新的并保存
                SessionContext context = new SessionContext();
                context.setSessionId(sessionId);
                save(sessionId, context);
                return context;
            } catch (Exception e) {
                handleRedisFailure("读取会话失败", e);
            }
        }

        // Redis不可用或失败，使用降级存储
        return fallbackStore.computeIfAbsent(sessionId, k -> {
            SessionContext ctx = new SessionContext();
            ctx.setSessionId(k);
            return ctx;
        });
    }

    /**
     * 保存会话上下文到Redis
     */
    public void save(String sessionId, SessionContext context) {
        context.touch();

        if (redisAvailable) {
            try {
                String json = objectMapper.writeValueAsString(context);
                redisTemplate.opsForValue().set(
                        REDIS_KEY_PREFIX + sessionId,
                        json,
                        Duration.ofMinutes(timeoutMinutes)
                );
                failCount = 0;
                // 同时保存到降级存储，确保Redis恢复后数据一致
                fallbackStore.put(sessionId, context);
                return;
            } catch (Exception e) {
                handleRedisFailure("保存会话失败", e);
            }
        }

        // 降级：直接写内存
        fallbackStore.put(sessionId, context);
    }

    /**
     * 删除会话
     */
    public void remove(String sessionId) {
        if (redisAvailable) {
            try {
                redisTemplate.delete(REDIS_KEY_PREFIX + sessionId);
                failCount = 0;
            } catch (Exception e) {
                handleRedisFailure("删除会话失败", e);
            }
        }
        fallbackStore.remove(sessionId);
    }

    /**
     * 检查会话是否存在
     */
    public boolean exists(String sessionId) {
        if (redisAvailable) {
            try {
                Boolean exists = redisTemplate.hasKey(REDIS_KEY_PREFIX + sessionId);
                failCount = 0;
                return Boolean.TRUE.equals(exists);
            } catch (Exception e) {
                handleRedisFailure("检查会话存在性失败", e);
            }
        }
        return fallbackStore.containsKey(sessionId);
    }

    /**
     * 续期会话（重置过期时间）
     */
    private void renewExpiration(String sessionId) {
        try {
            redisTemplate.expire(REDIS_KEY_PREFIX + sessionId, Duration.ofMinutes(timeoutMinutes));
        } catch (Exception e) {
            log.warn("续期会话失败: {}", e.getMessage());
        }
    }

    /**
     * 处理Redis故障：记录日志，达到阈值后切换到降级模式
     */
    private void handleRedisFailure(String message, Exception e) {
        failCount++;
        log.warn("{} - Redis故障，当前失败次数: {} - {}", message, failCount, e.getMessage());

        if (failCount >= FAIL_THRESHOLD) {
            redisAvailable = false;
            log.warn("Redis连续失败{}次，已切换到内存降级模式", FAIL_THRESHOLD);
        }
    }

    /**
     * 获取当前存储模式（用于监控）
     */
    public String getStorageMode() {
        return redisAvailable ? "redis" : "fallback-memory";
    }

    /**
     * 获取降级存储中的会话数量（用于监控）
     */
    public int getFallbackStoreSize() {
        return fallbackStore.size();
    }
}
