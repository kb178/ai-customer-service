package com.aicustomer.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 *
 * 配置序列化方式，确保：
 * 1. 中文字符串正常显示（UTF-8）
 * 2. Java对象序列化为可读的JSON（而非JDK二进制）
 * 3. Key使用String序列化
 */
@Configuration
public class RedisConfig {

    /**
     * 配置RedisTemplate（用于存储Java对象如SessionContext）
     *
     * Key序列化：StringRedisSerializer（可读）
     * Value序列化：Jackson2JsonRedisSerializer（JSON格式，可读）
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        Jackson2JsonRedisSerializer<Object> jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    // 不要自定义 StringRedisTemplate，直接用 Spring Boot 自动配置的
    // Spring Boot 的 StringRedisTemplate 默认使用 StringRedisSerializer（UTF-8），中文正常显示

    /**
     * 创建Jackson JSON序列化器
     * - 中文不转义
     * - 时间类型正确处理
     * - 所有属性可见
     */
    private Jackson2JsonRedisSerializer<Object> createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 所有属性可见
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 类型信息写入JSON，反序列化时可还原具体类型
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 时间模块
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    }
}
