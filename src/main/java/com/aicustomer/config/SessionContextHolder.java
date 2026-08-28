package com.aicustomer.config;

import com.aicustomer.entity.SessionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 会话上下文持有者（ThreadLocal）
 *
 * 作用：让 Function Calling 的函数 bean 能访问当前用户的 SessionContext
 * 原理：chatClient.prompt().call() 是同步调用，函数执行在同一线程，所以 ThreadLocal 安全
 *
 * 使用方式：
 * 1. chat() 方法调用 AI 前设置 sessionId
 * 2. 函数 bean 通过 getCurrentContext() 获取当前用户的上下文
 * 3. chat() 方法结束后在 finally 中清除
 */
@Component
@RequiredArgsConstructor
public class SessionContextHolder {

    private static final ThreadLocal<String> SESSION_ID = new ThreadLocal<>();

    private final SessionContextService sessionContextService;

    /**
     * 设置当前线程的 sessionId
     */
    public static void setSessionId(String sessionId) {
        SESSION_ID.set(sessionId);
    }

    /**
     * 获取当前线程的 sessionId
     */
    public static String getSessionId() {
        return SESSION_ID.get();
    }

    /**
     * 清除当前线程的 sessionId（必须在 finally 中调用，防止内存泄漏）
     */
    public static void clear() {
        SESSION_ID.remove();
    }

    /**
     * 获取当前用户的 SessionContext
     */
    public SessionContext getCurrentContext() {
        String sid = getSessionId();
        if (sid == null) return null;
        return sessionContextService.getOrCreate(sid);
    }

    /**
     * 保存当前用户的 SessionContext
     */
    public void saveCurrentContext(SessionContext context) {
        String sid = getSessionId();
        if (sid != null && context != null) {
            sessionContextService.save(sid, context);
        }
    }
}
