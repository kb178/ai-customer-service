# 核心业务功能详解

本文档详细说明 AI 智能客服系统的核心业务功能实现，不包含管理后台部分。

---

## 目录

1. [系统架构总览](#1-系统架构总览)
2. [对话核心流程（chat方法）](#2-对话核心流程chat方法)
3. [Function Calling 机制](#3-function-calling-机制)
4. [14个业务函数详解](#4-14个业务函数详解)
5. [会话管理与上下文](#5-会话管理与上下文)
6. [自定义注解系统](#6-自定义注解系统)
7. [预约确认机制](#7-预约确认机制)
8. [对话记忆（ChatMemory）](#8-对话记忆chatmemory)
9. [系统提示词设计](#9-系统提示词设计)
10. [关键设计模式](#10-关键设计模式)

---

## 1. 系统架构总览

### 请求处理链路

```
用户消息
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  ChatController                                             │
│  POST /api/chat/send                                        │
│  接收 { message, sessionId }                                │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  FunctionCallingChatServiceImpl.chat()                      │
│                                                             │
│  ① 加载 SessionContext（Redis）                             │
│  ② 检查 pending 确认（修改/取消预约）                        │
│  ③ 提取用户信息（注解 + 正则）                               │
│  ④ 构建动态系统提示词                                        │
│  ⑤ 设置 ThreadLocal                                         │
│  ⑥ 调用 AI（Function Calling）                              │
│  ⑦ 后处理响应                                               │
│  ⑧ 持久化上下文                                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│  Spring AI ChatClient                                       │
│                                                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ System Prompt │  │ ChatMemory   │  │ 14个Functions│      │
│  │ (动态组装)    │  │ (Redis记忆)  │  │ (业务函数)   │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                          │                                  │
│                          ▼                                  │
│                   DeepSeek API                              │
│                   (LLM 推理 + 决策)                         │
└─────────────────────────────────────────────────────────────┘
```

### 核心组件关系

```
ChatController
    │
    ▼
FunctionCallingChatServiceImpl ◄─── SessionContextService（会话存储）
    │                                   │
    ├── IntentMatcher（意图识别）        ├── Redis（主存储）
    │   ├── @Intent（意图匹配）          └── ConcurrentHashMap（降级）
    │   └── @EntityExtract（实体提取）
    │
    ├── SessionContextHolder（ThreadLocal桥接）
    │
    └── ChatClient（Spring AI）
            │
            ├── ChatMemory（RedisChatMemory）
            │
            └── 14个 Function Beans
                ├── CourseFunctions（3个）
                ├── CampusFunctions（4个）
                ├── ReservationFunctions（4个）
                ├── CustomerFunctions（2个）
                └── LeaveMessageFunctions（1个）
```

---

## 2. 对话核心流程（chat方法）

`FunctionCallingChatServiceImpl.chat()` 是整个系统的入口，每次用户发消息都会执行以下步骤：

### 流程步骤

```
chat(sessionId, message)
    │
    ├─ Step 1: 加载会话上下文
    │   └─ sessionContextService.getOrCreate(sessionId)
    │   └─ 从 Redis 读取 SessionContext，不存在则新建
    │
    ├─ Step 2: 保存用户消息到历史
    │   └─ context.addMessage("用户", message)
    │
    ├─ Step 3: 检查待确认的修改预约
    │   └─ if (pendingUpdate != null && 确认消息)
    │       └─ executePendingUpdate() → 直接修改数据库 → 返回结果
    │
    ├─ Step 4: 检查待确认的取消预约
    │   └─ if (pendingCancelReason != null && 确认消息)
    │       └─ executePendingCancel() → 直接取消 → 返回结果
    │
    ├─ Step 5: 清除过期的 pending（用户拒绝）
    │   └─ if (pending != null && 非确认消息)
    │       └─ 清除 pending，继续正常对话
    │
    ├─ Step 6: 提取用户信息
    │   └─ extractInfoFromMessage(message, context)
    │       ├── 注解驱动：兴趣(interest)、学历(education)
    │       ├── 数据库驱动：课程(course)、校区(campus)
    │       ├── 正则提取：手机号(phone)、姓名(name)
    │       └─ 有手机号 → 保存客户信息到 customer 表
    │
    ├─ Step 7: 持久化上下文
    │   └─ sessionContextService.save(sessionId, context)
    │
    ├─ Step 8: 构建动态系统提示词
    │   └─ buildSystemMessage(context)
    │       ├── 基础 SYSTEM_PROMPT（角色定义 + 规则 + 函数列表）
    │       ├── 已知用户信息摘要（姓名、电话、学历、兴趣...）
    │       └─ 当前预约状态（有预约ID / 暂无预约）
    │
    ├─ Step 9: 设置 ThreadLocal
    │   └─ SessionContextHolder.setSessionId(sessionId)
    │
    ├─ Step 10: 调用 AI（Function Calling）
    │   └─ chatClient.prompt()
    │       .system(systemMessage)      ← 系统提示词
    │       .user(message)              ← 用户消息
    │       .advisors(chatMemory)       ← 对话记忆
    │       .call().content()           ← 同步调用，AI 可能调用函数
    │
    ├─ Step 11: 清除 ThreadLocal
    │   └─ SessionContextHolder.clear()（finally 中）
    │
    ├─ Step 12: 后处理响应
    │   └─ processResponse(responseText, context)
    │       ├── 响应包含"已取消" → 清除 reservationId
    │       └─ 响应包含"预约成功" → 自动关联最新预约ID
    │
    ├─ Step 13: 保存助手回复
    │   └─ context.addMessage("助手", response)
    │   └─ sessionContextService.save()
    │   └─ conversationLogService.saveLog()（写入对话日志）
    │
    └─ Step 14: 返回响应给用户
```

### 关键设计点

| 设计点 | 说明 |
|--------|------|
| **pending 优先检查** | 在调用 AI 之前先检查确认消息，避免不必要的 AI 调用 |
| **信息自动提取** | 用户消息经过注解 + 正则双重提取，无需 AI 参与 |
| **动态系统提示词** | 每次调用 AI 前注入已知信息，AI 不会重复询问 |
| **ThreadLocal 桥接** | 让单例 Function Bean 能访问当前用户的 SessionContext |
| **后处理响应** | 从 AI 响应中提取业务状态（预约ID、取消状态） |

---

## 3. Function Calling 机制

### 什么是 Function Calling

Function Calling 是 Spring AI 的核心能力：**AI 不只是聊天，还能决定调用哪个函数、传什么参数**。

```
传统方式：用户消息 → AI 回复文字 → 代码解析文字 → 执行业务
Function Calling：用户消息 → AI 决定调用函数 → 直接执行业务 → 返回结果
```

### 工作原理

```
┌─────────────────────────────────────────────────────────────┐
│                    启动时（自动完成）                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ① @Bean 注册函数到 Spring 容器                              │
│  ② @Description 提供函数描述                                 │
│  ③ DTO 类提供参数结构（反射扫描字段名和类型）                  │
│  ④ Spring AI 自动生成 JSON Schema                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    运行时（每次对话）                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ① .defaultFunctions("name1", "name2", ...) 注册到 ChatClient│
│  ② AI 收到：系统提示词 + JSON Schema + 用户消息 + 对话历史     │
│  ③ AI 推理：该调用哪个函数？传什么参数？                       │
│  ④ AI 返回：{ "function": "xxx", "arguments": {...} }       │
│  ⑤ Spring AI 反序列化为 DTO 对象，调用函数                    │
│  ⑥ 函数返回结果，Spring AI 发回 AI 生成最终回复               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 函数注册方式

```java
// 1. 定义函数 Bean
@Bean
@Description("搜索课程信息。参数：keyword(关键词)、categoryId(分类ID)")
public Function<SearchCoursesRequest, SearchCoursesResponse> searchCourses() {
    return request -> {
        // 业务逻辑
        return response;
    };
}

// 2. 注册到 ChatClient
ChatClient.builder(chatModel)
    .defaultFunctions("searchCourses", "getCategories", ...)
    .build();

// 3. AI 自动识别并调用
chatClient.prompt().user("我想学Python").call().content();
// AI 可能自动调用 searchCourses(keyword="Python")
```

### 参数来源

AI 的参数来自**三个渠道**：

| 渠道 | 示例 |
|------|------|
| **用户直接说了** | "帮我预约课程2" → courseId=2 |
| **从对话历史推断** | 用户之前说了手机号 → 用之前的手机号 |
| **先调查询函数获取** | 用户说"帮我改预约" → 先查 queryReservation 拿到ID |

---

## 4. 14个业务函数详解

### 4.1 课程相关（3个）

#### searchCourses — 搜索课程

```java
@Description("搜索课程信息。参数：keyword(关键词，可选)、categoryId(分类ID，可选)")
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| keyword | String | 可选 | 搜索关键词，如"Python"、"Java" |
| categoryId | Long | 可选 | 分类ID筛选 |

**AI 调用场景：**
- 用户说"有什么课程？" → searchCourses()（无参数，返回全部）
- 用户说"我想学Python" → searchCourses(keyword="Python")
- 用户说"设计类课程有哪些" → searchCourses(categoryId=设计分类的ID)

#### getCategories — 获取课程分类

```java
@Description("获取所有课程分类列表")
```

无参数，返回所有课程分类。

**AI 调用场景：** 用户说"课程分几类？"或 AI 需要展示分类列表时。

#### getCourseSchedules — 获取课程时间表

```java
@Description("获取指定校区课程的时间安排。参数：campusId(校区ID)、courseId(课程ID)")
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| campusId | Long | 必填 | 校区ID |
| courseId | Long | 必填 | 课程ID |

**AI 调用场景：** 用户问"中关村校区的Python课什么时候上？"

---

### 4.2 校区相关（4个）

#### getProvinces — 获取省份列表

```java
@Description("获取有校区的省份列表")
```

无参数，返回有校区的省份。

#### getCities — 获取城市列表

```java
@Description("获取指定省份下有校区的城市列表。参数：provinceId(省份ID)")
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| provinceId | Long | 必填 | 省份ID |

#### getCampuses — 获取校区列表

```java
@Description("获取校区列表。参数：provinceId(可选)、cityId(可选)、courseId(可选，筛选开设该课程的校区)")
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| provinceId | Long | 可选 | 按省份筛选 |
| cityId | Long | 可选 | 按城市筛选 |
| courseId | Long | 可选 | 筛选开设指定课程的校区 |

**AI 调用场景：**
- 用户说"我在上海，有校区吗？" → getCampuses(cityId=上海的ID)
- 用户说"哪里有Python课？" → getCampuses(courseId=Python的ID)

#### getCampusCourses — 获取校区开设的课程

```java
@Description("获取指定校区开设的课程列表。参数：campusId(校区ID)")
```

**AI 调用场景：** 用户问"中关村校区有什么课？"

---

### 4.3 预约相关（4个）

#### createReservation — 创建预约

```java
@Description("创建课程预约。参数：customerName(姓名)、phone(电话)、courseId(课程ID)、campusId(校区ID)、scheduleId(时间段ID，可选)")
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| customerName | String | 必填 | 客户姓名 |
| phone | String | 必填 | 联系电话 |
| courseId | Long | 必填 | 课程ID |
| campusId | Long | 必填 | 校区ID |
| scheduleId | Long | 可选 | 时间段ID |

**内置校验：**
- 课程是否存在
- 校区是否存在
- 该校区是否开设此课程（不开设则推荐其他校区）
- 课程是否满员
- 时间段是否满员（如指定）

**自动操作：**
- 创建预约记录
- 记录预约日志
- 保存客户信息
- 更新校区课程学员数

#### updateReservation — 修改预约（带确认机制）

```java
@Description("修改已有预约。参数：reservationId(预约ID)、customerName(可选)、phone(可选)、courseId(可选)、campusId(可选)")
```

**确认机制：**
1. 第一次调用 → 暂存修改数据到 `pendingUpdate`，返回确认提示
2. 用户回复"确认" → `executePendingUpdate()` 直接执行修改
3. 用户回复其他 → 清除 pending，取消操作

详见 [第7节：预约确认机制](#7-预约确认机制)。

#### cancelReservation — 取消预约（带确认机制）

```java
@Description("取消已有预约。参数：reservationId(预约ID)、reason(取消原因，可选)")
```

**确认机制：** 同 updateReservation，暂存到 `pendingCancelReason`。

#### queryReservation — 查询预约

```java
@Description("查询预约信息。参数：reservationId(预约ID，可选)、phone(手机号，可选)")
```

**支持两种查询方式：** 按预约ID或按手机号（返回最近一条）。

**返回信息：** 预约ID、姓名、电话、课程、校区、状态、预约时间、备注。

---

### 4.4 客户相关（2个）

#### queryCustomerByPhone — 查询客户

```java
@Description("根据手机号查询客户信息。参数：phone(手机号)")
```

**AI 调用场景：** 用户提供了手机号时，AI 自动调用识别是否为回头客。

**返回信息：** 客户姓名、电话、学历、兴趣、来源、创建时间。

#### listReservationsByPhone — 查询客户预约列表

```java
@Description("查询某手机号的所有预约记录。参数：phone(手机号)")
```

**AI 调用场景：** 用户说"我有哪些预约？"、"查一下我的预约"。

---

### 4.5 留言相关（1个）

#### leaveMessage — 记录留言

```java
@Description("记录学员留言。参数：sessionId、customerName(可选)、customerPhone(可选)、message、category(可选)")
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 必填 | 会话ID |
| customerName | String | 可选 | 客户姓名 |
| customerPhone | String | 可选 | 客户电话 |
| message | String | 必填 | 留言内容 |
| category | String | 可选 | 分类（退款咨询/课程问题/投诉建议） |

**AI 调用场景：** 用户问退款政策、投诉、AI 无法回答的问题。

---

### 函数分类总览

```
14个 Function Calling 函数
│
├── 课程查询（3个）
│   ├── searchCourses      → 搜索课程
│   ├── getCategories      → 获取分类
│   └── getCourseSchedules → 获取时间表
│
├── 校区查询（4个）
│   ├── getProvinces       → 获取省份
│   ├── getCities          → 获取城市
│   ├── getCampuses        → 获取校区
│   └── getCampusCourses   → 校区课程
│
├── 预约管理（4个）⭐ 核心业务
│   ├── createReservation  → 创建预约
│   ├── updateReservation  → 修改预约（带确认）
│   ├── cancelReservation  → 取消预约（带确认）
│   └── queryReservation   → 查询预约
│
├── 客户管理（2个）
│   ├── queryCustomerByPhone    → 查询客户
│   └── listReservationsByPhone → 客户预约列表
│
└── 留言管理（1个）
    └── leaveMessage → 记录留言
```

---

## 5. 会话管理与上下文

### SessionContext 结构

```java
public class SessionContext {
    // 基础信息
    private String sessionId;           // 会话ID
    private LocalDateTime createTime;   // 创建时间
    private LocalDateTime lastActiveTime; // 最后活跃时间

    // 用户信息（从对话中自动提取）
    private String customerName;        // 客户姓名
    private String phone;               // 联系电话
    private String email;               // 邮箱
    private Integer age;                // 年龄
    private String education;           // 学历（大一、研究生...）
    private String interest;            // 兴趣（编程开发、UI设计...）

    // 业务状态
    private Long selectedCourseId;      // 已选课程ID
    private Long selectedCourseName;    // 已选课程名称
    private Long selectedCampusId;      // 已选校区ID
    private Long selectedCampusName;    // 已选校区名称
    private Long reservationId;         // 当前预约ID

    // 确认机制
    private Map<String, Object> pendingUpdate;    // 待确认的修改
    private String pendingCancelReason;           // 待确认的取消

    // 对话历史
    private List<String> conversationHistory;     // 最近20条消息
}
```

### 存储策略

```
SessionContext 存储
│
├── 主存储：Redis
│   ├── Key: "session:{sessionId}"
│   ├── Value: SessionContext 的 JSON 序列化
│   ├── TTL: 30分钟（每次有新消息续期）
│   └── 自动过期清理
│
└── 降级存储：ConcurrentHashMap
    ├── Redis 连续失败3次后自动切换
    ├── 内存存储，重启丢失
    └── Redis 恢复后自动切回
```

### SessionContextService 核心方法

```java
// 获取或创建会话
SessionContext getOrCreate(String sessionId)

// 保存会话
void save(String sessionId, SessionContext context)

// 删除会话
void remove(String sessionId)

// 检查会话是否存在
boolean exists(String sessionId)
```

### SessionContextHolder（ThreadLocal 桥接）

```java
// 问题：Function Bean 是单例，无法访问当前用户的 SessionContext
// 解决：用 ThreadLocal 传递 sessionId

// chat() 中：
SessionContextHolder.setSessionId(sessionId);  // 调用 AI 前设置
try {
    chatClient.prompt().call();  // 函数在这里执行（同一线程）
} finally {
    SessionContextHolder.clear();  // 清除防内存泄漏
}

// Function Bean 中：
SessionContext context = sessionContextHolder.getCurrentContext();
// 通过 ThreadLocal 中的 sessionId 从 Redis 获取上下文
```

---

## 6. 自定义注解系统

### 设计目的

在 AI 调用之前，**快速提取用户消息中的结构化信息**，减少 AI 调用次数。

### 三个注解

#### @Intent — 意图匹配

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Intent {
    String name();          // 意图名称
    String[] keywords();    // 触发关键词
}
```

**使用方式：** 在 IntentMatcher 中定义空方法作为注解载体。

```java
@Intent(name = "CONFIRM", keywords = {"确认", "确定", "没问题", "好的", "可以", "ok"})
public void confirmIntent() {}

@Intent(name = "CANCEL", keywords = {"取消", "不要了", "算了", "不去了"})
public void cancelIntent() {}
```

**调用：** `intentMatcher.matchIntent("好的没问题", "CONFIRM")` → `true`

#### @EntityExtract — 实体提取

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityExtract {
    String field();                          // 提取的字段名
    KeywordMapping[] mappings();             // 关键词→值映射
}
```

**使用方式：**

```java
@EntityExtract(field = "interest", mappings = {
    @KeywordMapping(keywords = {"编程", "java", "python"}, value = "编程开发"),
    @KeywordMapping(keywords = {"设计", "ui", "ux"},       value = "UI/UX设计"),
    @KeywordMapping(keywords = {"数据", "ai", "分析"},      value = "数据分析")
})
public void extractInterest() {}
```

#### @KeywordMapping — 关键词映射（嵌套注解）

```java
public @interface KeywordMapping {
    String[] keywords();    // 触发关键词
    String value();         // 目标值
}
```

### IntentMatcher 工作机制

```
IntentMatcher（@Component）
│
├── 启动时（@PostConstruct）
│   ├── scanAnnotations()
│   │   ├── 反射扫描 @Intent → 存入 intentKeywords Map
│   │   └── 反射扫描 @EntityExtract → 存入 entityMappings Map
│   │
│   └── loadFromDatabase()
│       ├── 从课程表加载课程关键词 → courseKeywordMap
│       └── 从校区表加载校区关键词 → campusKeywordMap
│
└── 运行时
    ├── matchIntent(message, "CONFIRM") → boolean
    │   └── 检查消息是否包含意图关键词
    │
    └── extractEntities(message) → Map<String, String>
        ├── 注解驱动：interest、education
        └── 数据库驱动：course、campus
```

### 混合匹配策略

```
实体提取策略
│
├── 固定分类（注解驱动）
│   ├── interest（兴趣）：编程开发 / UI设计 / 数据分析
│   └── education（学历）：大一 / 大二 / 研究生 / 零基础
│
└── 动态数据（数据库驱动）
    ├── course（课程）：启动时从数据库加载所有课程名
    └── campus（校区）：启动时从数据库加载所有校区名
    └── 新增课程/校区后自动生效，无需改代码
```

### 匹配优先级

```
用户消息: "我是大一新生，想学java，去中关村校区"
    │
    ▼
① @EntityExtract 匹配
   "大一" → education = "大一"
   "java" → interest = "编程开发"
    │
    ▼
② 数据库关键词匹配（长词优先）
   "java" → course = "Java全栈开发"
   "中关村" → campus = "中关村校区"
    │
    ▼
③ 结果: { education: "大一", interest: "编程开发", course: "Java全栈开发", campus: "中关村校区" }
```

---

## 7. 预约确认机制

### 设计目的

修改和取消预约是**高风险操作**，需要用户二次确认后才执行。

### 技术实现

```
┌─────────────────────────────────────────────────────────────┐
│                    确认机制流程                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户: "把课程改成Python"                                    │
│      │                                                      │
│      ▼                                                      │
│  LLM 调用 updateReservation(courseId=2)                     │
│      │                                                      │
│      ▼                                                      │
│  函数检查：无 pendingUpdate                                  │
│      │                                                      │
│      ▼                                                      │
│  暂存到 context.setPendingUpdate({courseId: 2})              │
│  返回 "您确认要修改吗？课程改为：Python"                      │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  用户: "确认"                                                │
│      │                                                      │
│      ▼                                                      │
│  chat() 检查：pendingUpdate != null && 确认消息              │
│      │                                                      │
│      ▼                                                      │
│  executePendingUpdate() 直接执行（不经过 LLM）               │
│  从 Redis 读取 pendingData → 修改数据库 → 清除 pending       │
│  返回 "预约修改成功！课程：Python"                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 涉及的字段

```java
// SessionContext 中的两个暂存字段
private Map<String, Object> pendingUpdate;     // 修改预约：暂存 {courseId, campusId, customerName, phone}
private String pendingCancelReason;            // 取消预约：暂存取消原因
```

### 三种状态转换

```
状态机：
    │
    ├─ 无 pending（正常状态）
    │   └─ 用户说"改课程" → 暂存 → 进入"待确认"状态
    │
    ├─ 待确认状态
    │   ├─ 用户说"确认" → 执行操作 → 回到"无 pending"
    │   └─ 用户说其他   → 清除 pending → 回到"无 pending"
    │
    └─ 执行中（executePendingUpdate/Cancel）
        └─ 成功/失败 → 清除 pending → 回到"无 pending"
```

### ThreadLocal 桥接

```
问题：updateReservation() 是单例 Bean，无法直接访问 SessionContext

解决：
┌──────────────────────────────────────────────────────────────┐
│  chat() 方法                     Function Bean               │
│  ┌─────────────────┐            ┌─────────────────┐         │
│  │ setSessionId()  │ ──Thread── │ getCurrent      │         │
│  │    (before)     │   Local    │ Context()       │         │
│  └─────────────────┘            └─────────────────┘         │
│         │                              │                    │
│         ▼                              ▼                    │
│  ┌─────────────────┐            ┌─────────────────┐         │
│  │ call AI         │            │ 暂存 pending    │         │
│  │ (触发函数)       │            │ 返回确认提示     │         │
│  └─────────────────┘            └─────────────────┘         │
│         │                                                    │
│         ▼                                                    │
│  ┌─────────────────┐                                         │
│  │ clear()         │  ← finally 中清除                       │
│  └─────────────────┘                                         │
└──────────────────────────────────────────────────────────────┘
```

---

## 8. 对话记忆（ChatMemory）

### 实现方式

```java
// RedisChatMemory 实现 ChatMemory 接口
public class RedisChatMemory implements ChatMemory {
    // 存储对话消息到 Redis List
    void add(String conversationId, List<Message> messages)

    // 获取最近 N 条消息
    List<Message> get(String conversationId, int lastN)

    // 清除对话记录
    void clear(String conversationId)
}
```

### 消息类型

```java
// Spring AI 的三种消息类型
UserMessage       // 用户消息
AssistantMessage  // AI 回复
SystemMessage     // 系统提示词
```

### 存储结构

```
Redis Key: "chat:memory:{sessionId}"
Redis Type: List
Max Size: 20条（滑动窗口）
TTL: 与 SessionContext 同步

序列化格式: "{role}\n{content}"
示例: "USER\n我想学Python"
```

### 降级策略

```
Redis 可用 → 正常存储
Redis 不可用（连续3次失败）→ 降级到 ConcurrentHashMap
Redis 恢复 → 自动切回
```

### 与 MessageChatMemoryAdvisor 配合

```java
// 注册 Advisor
ChatClient.builder(chatModel)
    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
    .build();

// 调用时指定 conversationId
chatClient.prompt()
    .advisors(a -> a.param("chatMemoryConversationId", sessionId))
    .call();

// Advisor 自动：
// ① 从 Redis 加载该 sessionId 的对话历史
// ② 注入到消息列表中
// ③ AI 回复后自动保存
```

---

## 9. 系统提示词设计

### 基础 SYSTEM_PROMPT 结构

```
系统提示词
│
├── 角色定义
│   └── "你是课程咨询顾问"
│
├── 最高优先级规则
│   └── "必须调用函数，不能只回复文字"
│
├── 函数调用映射表
│   ├── 创建预约 → createReservation
│   ├── 取消预约 → queryReservation → cancelReservation
│   ├── 修改预约 → queryReservation → updateReservation
│   └── ...
│
├── 核心规则
│   ├── 不要询问已知信息
│   ├── 不要重复确认
│   └── 直接使用已知信息
│
├── 客户识别规则
│   └── 有手机号 → queryCustomerByPhone → 个性化问候
│
├── 留言规则
│   └── 退款/投诉/无法回答 → leaveMessage → "客服2小时内联系"
│
├── 主要任务
│   ├── 识别回头客
│   ├── 了解用户需求
│   ├── 推荐课程
│   ├── 引导预约
│   ├── 收集联系方式
│   └── 无法回答时建议留言
│
├── 可用函数列表（14个）
│   ├── 客户相关：queryCustomerByPhone, listReservationsByPhone
│   ├── 留言相关：leaveMessage
│   ├── 地区相关：getProvinces, getCities, getCampuses
│   ├── 课程相关：getCategories, searchCourses, getCampusCourses, getCourseSchedules
│   └── 预约相关：createReservation, updateReservation, cancelReservation, queryReservation
│
├── 交互流程（9种标准流程）
│   ├── 手机号 → 客户识别
│   ├── 课程咨询 → 分类 → 搜索
│   ├── 校区查询 → 省份 → 城市 → 校区
│   └── ...
│
└── 运营提示
    ├── 校区课程差异提醒
    ├── 课程容量限制
    └── 退款/投诉不要猜测
```

### 动态组装

```java
private String buildSystemMessage(SessionContext context) {
    StringBuilder sb = new StringBuilder(SYSTEM_PROMPT);

    // 追加已知用户信息
    sb.append(context.getKnownInfoSummary());
    // 输出示例：
    // 已知用户信息：
    // - 姓名: 张三
    // - 电话: 13800138000
    // - 学历: 大一
    // - 兴趣: 编程开发
    // - 已选课程: Java全栈开发
    // - 已选校区: 中关村校区

    // 追加预约状态
    if (context.getReservationId() != null) {
        sb.append("用户已有预约（ID: ").append(context.getReservationId()).append("）");
    } else {
        sb.append("用户暂无预约");
    }

    return sb.toString();
}
```

**效果：** AI 每次调用都能看到用户的完整信息，不会重复询问。

---

## 10. 关键设计模式

### 10.1 注解驱动的预处理

```
用户消息 → IntentMatcher（注解扫描）→ 提取实体 → 存入 SessionContext
                                                         │
                                                         ▼
                                                    AI 调用时已知信息
```

**优势：** 兴趣、学历、课程、校区等信息在 AI 调用前就提取好了，减少 AI 的工作量。

### 10.2 ThreadLocal 桥接模式

```
单例 Bean 需要访问会话状态 → ThreadLocal 传递 sessionId
```

**适用场景：** Function Calling 的函数 Bean 是单例，但需要访问当前用户的 SessionContext。

### 10.3 暂存确认模式

```
高风险操作 → 暂存到 SessionContext → 用户确认 → 执行
```

**适用场景：** 修改预约、取消预约等需要二次确认的操作。

### 10.4 优雅降级模式

```
Redis 可用 → Redis 存储
Redis 不可用 → ConcurrentHashMap 降级
Redis 恢复 → 自动切回
```

**应用于：** SessionContextService 和 RedisChatMemory。

### 10.5 动态系统提示词

```
静态规则（SYSTEM_PROMPT）+ 动态状态（SessionContext）→ 完整提示词
```

**效果：** AI 始终知道用户的完整上下文，提供个性化服务。

### 10.6 多层信息提取

```
用户消息
    │
    ├─ 第1层：注解提取（IntentMatcher）
    │   ├── 意图：确认/取消
    │   └── 实体：兴趣/学历/课程/校区
    │
    ├─ 第2层：正则提取
    │   ├── 手机号：1[3-9]\\d{9}
    │   └── 姓名：我叫/我是/我姓 + 中文名
    │
    └─ 第3层：AI 提取（Function Calling）
        └── AI 从自然语言中理解复杂意图并调用函数
```

---

## 附录：业务流程图

### 完整预约流程

```
用户: "我想学Python"
    │
    ├─ IntentMatcher 提取: interest="编程开发"
    │
    ▼
AI 调用 searchCourses(keyword="Python")
    │
    ▼
返回课程列表
    │
    ▼
用户: "在中关村校区有吗？"
    │
    ├─ IntentMatcher 提取: campus="中关村校区"
    │
    ▼
AI 调用 getCampuses(courseId=Python的ID)
    │
    ▼
返回校区列表（中关村有）
    │
    ▼
用户: "帮我预约"
    │
    ├─ SessionContext 已有: 姓名、电话、课程、校区
    │
    ▼
AI 调用 createReservation(customerName, phone, courseId, campusId)
    │
    ├─ 校验：课程存在 ✓ 校区存在 ✓ 校区有此课程 ✓ 未满员 ✓
    │
    ▼
创建成功 → 返回"预约成功！"
    │
    ▼
processResponse() 自动关联预约ID到 SessionContext
```

### 回头客识别流程

```
用户: "13800138000"
    │
    ├─ 正则提取手机号 → context.setPhone("13800138000")
    │
    ▼
AI 调用 queryCustomerByPhone(phone="13800138000")
    │
    ├─ 查到客户：张三，之前咨询过Java课程
    │
    ▼
AI: "您好，张三同学，又见面了！上次您咨询的Java课程，现在想继续了解吗？"
```

### 留言转人工流程

```
用户: "我要退款！"
    │
    ▼
AI 判断：退款问题超出处理范围
    │
    ▼
AI: "请问您的姓名和电话是？"
    │
    ▼
用户: "张三，13800138000"
    │
    ▼
AI 调用 leaveMessage(sessionId, customerName="张三", phone="13800138000",
                      message="要求退款", category="退款咨询")
    │
    ▼
AI: "已为您记录，客服会在2小时内联系您。"
```
