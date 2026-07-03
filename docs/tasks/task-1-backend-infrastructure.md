# 任务1：后端基础设施

## 目标

为管理后台搭建基础设施：统一响应格式、简单登录认证、请求拦截器、对话记录持久化。

## 前置条件

- Spring Boot 3.4.5 项目已可运行
- MySQL 数据库 `ai_customer` 已启动
- Redis 已启动（localhost:6379）

## 任务清单

### 1.1 统一响应封装

新建目录 `src/main/java/com/aicustomer/controller/admin/`，在其中创建以下两个类：

**AdminResponse.java** — 管理后台统一响应格式：

```java
package com.aicustomer.controller.admin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminResponse<T> {
    private int code;
    private String message;
    private T data;

    public static <T> AdminResponse<T> ok(T data) {
        return new AdminResponse<>(200, "success", data);
    }

    public static <T> AdminResponse<T> ok() {
        return new AdminResponse<>(200, "success", null);
    }

    public static <T> AdminResponse<T> error(int code, String message) {
        return new AdminResponse<>(code, message, null);
    }
}
```

**PageResult.java** — 分页响应：

```java
package com.aicustomer.controller.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
}
```

### 1.2 简单登录认证

**LoginRequest.java**（放在 `controller/admin` 包）— 登录请求 DTO：

```java
package com.aicustomer.controller.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

**AdminLoginController.java**（放在 `controller/admin` 包）— 登录接口：

```java
package com.aicustomer.controller.admin;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AdminLoginController {

    @PostMapping("/login")
    public AdminResponse<Void> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        if ("admin".equals(request.getUsername()) && "123456".equals(request.getPassword())) {
            session.setAttribute("adminUser", request.getUsername());
            return AdminResponse.ok();
        }
        return AdminResponse.error(401, "用户名或密码错误");
    }
}
```

**AdminInterceptor.java**（放在 `config` 包）— 认证拦截器：

```java
package com.aicustomer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getSession().getAttribute("adminUser") != null) {
            return true;
        }
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
            Map.of("code", 401, "message", "未登录，请先登录", "data", (Object) null)
        ));
        return false;
    }
}
```

**WebMvcConfig.java**（放在 `config` 包）— MVC 配置：

```java
package com.aicustomer.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
```

### 1.3 对话记录持久化

**新建文件** `src/main/resources/schema-admin.sql`，内容如下：

```sql
-- 管理后台新增表
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `conversation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `customer_phone` VARCHAR(20) DEFAULT NULL COMMENT '客户手机号',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_customer_phone` (`customer_phone`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话记录表';
```

执行此 SQL 创建表：`mysql -u root -p ai_customer < src/main/resources/schema-admin.sql`

**ConversationLog.java**（放在 `entity` 包）— 实体类：

```java
package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("conversation_log")
public class ConversationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 客户手机号 */
    private String customerPhone;

    /** 角色: user/assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

**ConversationLogMapper.java**（放在 `mapper` 包）：

```java
package com.aicustomer.mapper;

import com.aicustomer.entity.ConversationLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationLogMapper extends BaseMapper<ConversationLog> {
}
```

**ConversationLogService.java**（放在 `service` 包）：

```java
package com.aicustomer.service;

import com.aicustomer.entity.ConversationLog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ConversationLogService extends IService<ConversationLog> {

    void saveLog(String sessionId, String customerPhone, String role, String content);
}
```

**ConversationLogServiceImpl.java**（放在 `service/impl` 包）：

```java
package com.aicustomer.service.impl;

import com.aicustomer.entity.ConversationLog;
import com.aicustomer.mapper.ConversationLogMapper;
import com.aicustomer.service.ConversationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ConversationLogServiceImpl extends ServiceImpl<ConversationLogMapper, ConversationLog> implements ConversationLogService {

    @Override
    public void saveLog(String sessionId, String customerPhone, String role, String content) {
        ConversationLog log = new ConversationLog();
        log.setSessionId(sessionId);
        log.setCustomerPhone(customerPhone);
        log.setRole(role);
        log.setContent(content);
        save(log);
    }
}
```

**修改 FunctionCallingChatServiceImpl.java**：

步骤1：在类的字段区域（第42行 `sessionContextService` 之后）注入新 Service：
```java
private final ConversationLogService conversationLogService;
```

步骤2：在 `chat` 方法中共有 3 个 return 点，需要在每个 return 之前添加对话记录写入：

**return 点1（第148-149行）** — pendingUpdate 确认后的 early return：
在 `sessionContextService.save(sessionId, context);` 之后、`return result;` 之前添加：
```java
// 写入对话记录
String phone1 = context.getPhone();
conversationLogService.saveLog(sessionId, phone1, "user", message);
conversationLogService.saveLog(sessionId, phone1, "assistant", result);
```

**return 点2（第154-155行）** — pendingCancel 确认后的 early return：
在 `sessionContextService.save(sessionId, context);` 之后、`return result;` 之前添加：
```java
// 写入对话记录
String phone2 = context.getPhone();
conversationLogService.saveLog(sessionId, phone2, "user", message);
conversationLogService.saveLog(sessionId, phone2, "assistant", result);
```

**return 点3（第199-201行）** — 主流程 return：
在 `sessionContextService.save(sessionId, context);` 之后、`return processedResponse;` 之前添加：
```java
// 写入对话记录
String phone3 = context.getPhone();
conversationLogService.saveLog(sessionId, phone3, "user", message);
conversationLogService.saveLog(sessionId, phone3, "assistant", processedResponse);
```

## 验证方式

1. 执行 `schema-admin.sql` 创建 conversation_log 表
2. 启动应用 `mvn spring-boot:run`
3. 测试登录：`curl -X POST http://localhost:8082/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"123456\"}" -c cookie.txt`
4. 测试拦截器（无 cookie）：`curl http://localhost:8082/api/admin/course/list` → 应返回 `{"code":401,"message":"未登录，请先登录","data":null}`
5. 测试拦截器（有 cookie）：`curl http://localhost:8082/api/admin/course/list -b cookie.txt` → 应返回课程数据（T3完成后）
6. 测试对话记录：通过聊天接口发一条消息，然后查数据库 `SELECT * FROM conversation_log`

## 注意事项

- `AdminInterceptor` 中返回 401 时必须设置 `response.setContentType("application/json;charset=UTF-8")`
- `WebMvcConfig` 不需要额外排除 `/api/auth/login`，因为拦截器只拦截 `/api/admin/**`
- `FunctionCallingChatServiceImpl` 中获取 customerPhone 时要做 null 检查
- `AdminLoginController` 和 `LoginRequest` 放在 `controller/admin` 包下，方便后续管理 Controller 统一管理
