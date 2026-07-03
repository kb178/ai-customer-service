# AI智能客服系统 - 架构设计文档

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| v1.0 | 2026-07-02 | 初稿 |

---

## 一、系统总览

### 1.1 系统定位

基于 Spring Boot 3 + Spring AI + DeepSeek 的教育培训行业智能客服系统。用户通过 Web 聊天窗口与 AI 对话，AI 通过 Function Calling 直接操作业务数据库，完成课程咨询、预约管理等操作。

### 1.2 核心架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户浏览器                                │
│                  static/index.html (Chat UI)                    │
└──────────────────────────────┬──────────────────────────────────┘
                               │ HTTP POST /api/chat/send
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│                     Spring Boot 3 (port:8082)                   │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                     Controller 层                           │  │
│  │          ChatController (/api/chat/send)                   │  │
│  │          ChatRequest → ChatResponse                        │  │
│  └──────────────────────────┬─────────────────────────────────┘  │
│                             │                                    │
│  ┌──────────────────────────▼─────────────────────────────────┐  │
│  │                      Service 层                             │  │
│  │  ┌─────────────────────┐  ┌────────────────────────────┐  │  │
│  │  │ FunctionCallingChat │  │     ChatService            │  │  │
│  │  │     Service         │  │     (指令解析模式)          │  │  │
│  │  │  (主推，Function     │  │  (备用，逐步淘汰)          │  │  │
│  │  │   Calling模式)      │  │                            │  │  │
│  │  └────────┬────────────┘  └────────────────────────────┘  │  │
│  │           │                                                 │  │
│  │           │ 使用                                            │  │
│  │           ▼                                                 │  │
│  │  ┌─────────────────────────────────────────────────────┐  │  │
│  │  │   IntentMatcher (意图匹配器，仅Function Calling使用) │  │  │
│  │  │  - 注解驱动：@Intent @EntityExtract @KeywordMapping │  │  │
│  │  │  - 动态加载：课程/校区关键词从数据库读取             │  │  │
│  │  └─────────────────────────────────────────────────────┘  │  │
│  └────────────────────────────────────────────────────────────┘  │
│                             │                                    │
│  ┌──────────────────────────▼─────────────────────────────────┐  │
│  │                  Function Calling 层                        │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐   │  │
│  │  │ Reservation  │ │   Course     │ │     Campus       │   │  │
│  │  │  Functions   │ │  Functions   │ │    Functions     │   │  │
│  │  │ (4个函数)     │ │ (3个函数)     │ │   (4个函数)       │   │  │
│  │  └──────┬───────┘ └──────┬───────┘ └────────┬─────────┘   │  │
│  └─────────┼────────────────┼──────────────────┼─────────────┘  │
│            │                │                  │                 │
│  ┌─────────▼────────────────▼──────────────────▼─────────────┐  │
│  │                    Service 层 (业务服务)                    │  │
│  │  ReservationService  CourseService  CampusService          │  │
│  │  CustomerService     CourseScheduleService                 │  │
│  │  CampusCourseService  ReservationLogService                │  │
│  └─────────────────────────────┬──────────────────────────────┘  │
│                                │                                 │
│  ┌─────────────────────────────▼──────────────────────────────┐  │
│  │               MyBatis-Plus (ORM + 分页)                     │  │
│  └─────────────────────────────┬──────────────────────────────┘  │
│                                │                                 │
└────────────────────────────────┼─────────────────────────────────┘
                                 │ JDBC
                 ┌───────────────▼───────────────┐
                 │         MySQL 8.0              │
                 │    ai_customer database        │
                 │    (10张表 + 4张新增表)         │
                 └───────────────────────────────┘

                 ┌───────────────────────────────┐
                 │     DeepSeek API (远程)        │
                 │   https://api.deepseek.com     │
                 │   spring-ai-openai-starter     │
                 └───────────────────────────────┘
```

### 1.3 技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| 框架 | Spring Boot | 3.4.5 | Web容器、自动配置 |
| AI | Spring AI | 1.0.0-M6 | ChatClient + Function Calling |
| AI模型 | DeepSeek | deepseek-chat | 大语言模型推理 |
| ORM | MyBatis-Plus | 3.5.7 | 数据库操作、分页、逻辑删除 |
| 数据库 | MySQL | 8.0+ | 业务数据存储 |
| 缓存 | Redis | 6.0+ | 会话持久化（Phase 1） |
| 前端 | HTML/CSS/JS | - | 聊天界面（单文件SPA） |
| 构建 | Maven | 3.6+ | 依赖管理、打包 |
| 语言 | Java | 17 | 后端开发 |

---

## 二、分层架构

### 2.1 包结构

```
com.aicustomer
├── AiCustomerServiceApplication.java    # 启动类
│
├── config/                              # 配置层
│   ├── ChatClientConfig.java            # Spring AI ChatClient + ChatMemory 配置
│   ├── MybatisPlusConfig.java           # MyBatis-Plus 分页 + 自动填充
│   └── BizConstants.java                # 业务常量（状态码、ID等）
│
├── controller/                          # 控制层（REST API）
│   ├── ChatController.java              # 对话接口 /api/chat/*
│   ├── ChatRequest.java                 # 请求DTO（@Valid校验）
│   └── ChatResponse.java                # 响应DTO
│
├── service/                             # 服务接口层
│   ├── ChatService.java                 # 指令解析模式接口
│   ├── FunctionCallingChatService.java  # Function Calling模式接口
│   ├── CourseService.java               # 课程业务
│   ├── CampusService.java               # 校区业务
│   ├── ReservationService.java          # 预约业务
│   ├── CustomerService.java             # 客户业务
│   ├── CourseScheduleService.java       # 课程时间段
│   ├── CampusCourseService.java         # 校区课程关联
│   ├── CourseCategoryService.java       # 课程分类
│   ├── ProvinceService.java             # 省份
│   ├── CityService.java                 # 城市
│   └── ReservationLogService.java       # 预约日志
│
├── service/impl/                        # 服务实现层
│   ├── FunctionCallingChatServiceImpl.java  # ★ 核心：Function Calling对话实现
│   ├── ChatServiceImpl.java                 # 指令解析对话实现（逐步淘汰）
│   ├── CourseServiceImpl.java
│   ├── CampusServiceImpl.java
│   ├── ReservationServiceImpl.java
│   ├── CustomerServiceImpl.java
│   └── ... (其他实现类)
│
├── function/                            # Function Calling 函数定义
│   ├── ReservationFunctions.java        # 预约CRUD（4个函数）
│   ├── CourseFunctions.java             # 课程查询（3个函数）
│   └── CampusFunctions.java             # 校区/地区查询（4个函数）
│
├── entity/                              # 实体类
│   ├── Course.java                      # 课程
│   ├── Campus.java                      # 校区
│   ├── Reservation.java                 # 预约
│   ├── Customer.java                    # 客户
│   ├── CourseSchedule.java              # 课程时间段
│   ├── CampusCourse.java                # 校区课程关联
│   ├── CourseCategory.java              # 课程分类
│   ├── Province.java                    # 省份
│   ├── City.java                        # 城市
│   ├── ReservationLog.java              # 预约日志
│   └── SessionContext.java              # 会话上下文（非持久化）
│
├── mapper/                              # MyBatis-Plus Mapper 接口
│   ├── CourseMapper.java
│   ├── CampusMapper.java
│   ├── ReservationMapper.java
│   ├── CustomerMapper.java
│   └── ... (其他Mapper)
│
├── annotation/                          # 自定义注解 + 意图匹配
│   ├── Intent.java                      # 意图注解
│   ├── EntityExtract.java               # 实体提取注解
│   ├── KeywordMapping.java              # 关键词映射注解
│   └── IntentMatcher.java               # 意图匹配器（核心组件）
│
└── exception/                           # 异常处理
    └── GlobalExceptionHandler.java      # 全局异常处理器
```

### 2.2 各层职责

| 层级 | 职责 | 依赖方向 |
|------|------|----------|
| Controller | 接收HTTP请求，参数校验，路由分发 | → Service |
| Service Interface | 定义业务契约 | - |
| Service Impl | 业务逻辑编排 | → Mapper |
| Function | Spring AI Function Calling 函数，AI可直接调用 | → Service |
| Entity | 数据模型，对应数据库表 | - |
| Mapper | 数据库访问，MyBatis-Plus CRUD | → Database |
| IntentMatcher | 意图识别 + 实体提取（仅Function Calling模式使用） | → Service (查课程/校区) |

> 注意依赖方向：Function → Service → Mapper，不可反向依赖。

---

## 三、核心流程：一次完整的对话

### 3.1 Function Calling 模式（主推）

```
用户输入: "我想学Python，在上海有校区吗"
         │
         ▼
┌─ ChatController.sendMessage() ─────────────────────────────────┐
│  1. 路由判断: mode="function" → FunctionCallingChatService     │
└──────────────────────────────┬─────────────────────────────────┘
                               ▼
┌─ FunctionCallingChatServiceImpl.chat() ────────────────────────┐
│                                                                 │
│  2. 获取/创建 SessionContext                                    │
│     sessionContexts.computeIfAbsent(sessionId)                  │
│                                                                 │
│  3. 检查待确认操作（预约修改/取消）                               │
│     → 无待确认操作，继续                                         │
│                                                                 │
│  4. IntentMatcher 实体提取                                      │
│     extractEntities("我想学Python，在上海有校区吗")              │
│     → interest="编程开发", course="Python数据分析"               │
│     → lookupCourse("Python数据分析") → Course{id=2}            │
│                                                                 │
│  5. 更新 SessionContext                                         │
│     context.setInterest("编程开发")                             │
│     context.setSelectedCourseId(2)                              │
│     context.setSelectedCourseName("Python数据分析")             │
│                                                                 │
│  6. 构建系统提示词（含已知信息）                                  │
│     buildSystemMessage(context)                                 │
│     → "已知用户信息：\n- 兴趣: 编程开发\n- 已选课程: Python..." │
│                                                                 │
│  7. Spring AI ChatClient 调用 DeepSeek                         │
│     chatClient.prompt()                                         │
│       .system(systemMessage)                                    │
│       .user(message)                                            │
│       .defaultFunctions(11个函数)                               │
│       .call()                                                   │
│     → DeepSeek 判断需要调用 getCampuses(cityId=13)             │
│                                                                 │
│  8. Function Calling 执行链                                     │
│     DeepSeek → Spring AI → CampusFunctions.getCampuses()       │
│     → 查询 campus WHERE city_id=13 AND status=1                │
│     → 返回校区列表给 DeepSeek                                   │
│                                                                 │
│  9. DeepSeek 生成最终回复                                       │
│     "上海有2个校区开设Python课程：                               │
│      1. 上海徐汇校区 - 漕溪北路88号                              │
│      2. 上海浦东校区 - 陆家嘴金融中心"                           │
│                                                                 │
│  10. 后处理响应                                                  │
│      processResponse() → 检查是否有预约操作                      │
│                                                                 │
│  11. 保存到对话历史                                              │
│      context.addMessage("助手", responseText)                   │
└──────────────────────────────┬──────────────────────────────────┘
                               ▼
         ChatResponse(sessionId, reply, "function")
         → 前端渲染消息气泡
```

### 3.2 Function Calling 调用链路

```
DeepSeek API (远程推理)
    │
    │ 决定调用函数 + 参数
    ▼
Spring AI ChatClient (框架层)
    │
    │ 解析函数名 + 反射调用
    ▼
Spring Bean (函数注册)
    │
    │ @Bean 注册的 Function<Request, Response>
    ▼
ReservationFunctions / CourseFunctions / CampusFunctions
    │
    │ 业务逻辑 + 数据校验
    ▼
Service 层 (ReservationService, CourseService, ...)
    │
    │ MyBatis-Plus CRUD
    ▼
MySQL 数据库
    │
    │ 返回查询结果
    ▲
    │ 序列化为 JSON 返回给 DeepSeek
    │
DeepSeek 基于函数结果生成自然语言回复
```

### 3.3 指令解析模式（备用，逐步淘汰）

```
用户输入
    │
    ▼
ChatServiceImpl.chat()
    │
    ├─ 正则提取用户信息
    ├─ 构建系统提示词（含课程/校区列表）
    ├─ ChatClient 调用 AI
    │
    ▼
AI 回复中包含特殊指令
    │
    ├─ "SEARCH_COURSES:Python" → courseService.searchCourses()
    ├─ "SEARCH_CAMPUS" → campusService.getAllCampuses()
    ├─ "CREATE_RESERVATION:{json}" → reservationService.createReservation()
    ├─ "UPDATE_RESERVATION:{json}" → 预览修改 → 用户确认 → 保存
    ├─ "CANCEL_RESERVATION:{json}" → 预览取消 → 用户确认 → 保存
    └─ "SAVE_CUSTOMER:{json}" → customerService.saveCustomer()
    │
    ▼
processAiResponse() 解析并执行指令
    │
    ▼
返回处理后的自然语言回复
```

**两种模式对比**：

| 维度 | Function Calling 模式 | 指令解析模式 |
|------|----------------------|-------------|
| 实现方式 | Spring AI 原生支持 | 人工解析 AI 回复文本 |
| 灵活性 | AI 自主决定调哪个函数 | 需要在提示词中定义指令格式 |
| 维护成本 | 低（新增函数注册即可） | 高（需同步修改提示词+解析逻辑） |
| 可靠性 | 高（框架保证参数校验） | 中（正则解析易出错） |
| 状态 | **主推** | 备用，逐步淘汰 |

---

## 四、Function Calling 函数设计

### 4.1 函数注册机制

每个函数通过 `@Bean` + `@Description` 注册为 Spring Bean：

```java
@Configuration
public class ReservationFunctions {
    @Bean
    @Description("创建课程预约。参数：customerName、phone、courseId、campusId")
    public Function<CreateReservationRequest, CreateReservationResponse> createReservation() {
        return request -> { ... };
    }
}
```

Spring AI 自动扫描所有 `Function<Req, Resp>` 类型的 Bean，将其注册到 ChatClient：

```java
chatClient.defaultFunctions(
    "createReservation", "updateReservation", "cancelReservation", "queryReservation",
    "searchCourses", "getCategories", "getCampusCourses", "getCourseSchedules",
    "getProvinces", "getCities", "getCampuses"
);
```

### 4.2 完整函数清单

| 模块 | 函数名 | 入参 | 出参 | 业务逻辑 |
|------|--------|------|------|----------|
| 预约 | `createReservation` | customerName, phone, courseId, campusId, scheduleId? | success, message, reservationId | 校验课程/校区存在 → 校验校区是否开设该课程 → 校验容量 → 创建预约 → 记录日志 → 更新学员数 |
| 预约 | `updateReservation` | reservationId, customerName?, phone?, courseId?, campusId? | success, message, customerName, phone, courseName, campusName | 校验预约存在 → 更新字段 → 记录日志 |
| 预约 | `cancelReservation` | reservationId, reason | success, message | 校验预约存在 → 更新状态为已取消 → 记录日志 → 释放容量 |
| 预约 | `queryReservation` | reservationId?, phone? | found, reservationId, customerName, ...status, appointmentTime | 按ID或手机号查询 → 关联课程/校区名称 |
| 课程 | `searchCourses` | keyword?, categoryId? | courses[], total | 按关键词 + 分类过滤 |
| 课程 | `getCategories` | 无 | categories[], total | 查询所有分类 |
| 课程 | `getCourseSchedules` | campusId, courseId | found, schedules[], total | 查校区课程关联 → 查时间段 |
| 校区 | `getProvinces` | 无 | provinces[], total | 只返回有校区的省份 |
| 校区 | `getCities` | provinceId | cities[], total | 查该省份下有校区的城市 |
| 校区 | `getCampuses` | provinceId?, cityId?, courseId? | campuses[], total | 多维度筛选：按省/市/课程 |
| 校区 | `getCampusCourses` | campusId | campusCourses[], total | 查校区开设的所有课程 |

### 4.3 函数参数/返回值设计

函数的 Request/Response 类定义在各自 Functions 类内部（静态内部类）：

```java
// 请求类 — 字段名即为 AI 可传入的参数名
@Data
public static class CreateReservationRequest {
    private String customerName;  // AI从对话中提取
    private String phone;
    private Long courseId;        // AI根据课程名查找ID后传入
    private Long campusId;
    private Long scheduleId;      // 可选
}

// 响应类 — 字段内容返回给AI，AI据此生成回复
@Data
public static class CreateReservationResponse {
    private boolean success;
    private String message;
    private Long reservationId;
    private String customerName;
    private String phone;
    private String courseName;
    private String campusName;
}
```

**关键设计原则**：
- 函数内部做完整的业务校验（课程是否存在、校区是否开设、容量是否满）
- 返回结果包含足够的信息，让 AI 能生成有意义的回复
- 失败时返回具体错误信息，AI 会据此告知用户

---

## 五、意图识别与实体提取

### 5.1 IntentMatcher 架构

```
IntentMatcher
├── 注解驱动（启动时扫描，固定不变）
│   ├── @Intent → 意图关键词表
│   │   ├── CONFIRM: ["确认","确定","好的","可以","ok",...]
│   │   └── CANCEL:  ["取消","不要了","算了","不去了"]
│   │
│   └── @EntityExtract → 实体关键词表
│       ├── interest: {"编程","java","python",...} → "编程开发"
│       └── education: {"大一","大二","研究生",...} → 对应值
│
└── 动态加载（启动时从数据库读取，数据变更后重启生效）
    ├── courseKeywordMap: "python数据分析" → Course{id=2}
    ├── campusKeywordMap: "中关村" → Campus{id=1}
    └── ... (课程名/别名/描述词/校区名/简称)
```

### 5.2 匹配策略

```java
// 意图匹配：关键词包含
matchIntent("确认一下", "CONFIRM") → true  // "确认" 在 "确认一下" 中

// 实体提取：最长匹配优先（按字符数）
matchCourse("我想学Python数据分析")
  → 候选: "python数据分析"(8字符), "python"(6字符), "数据"(2字符)
  → 返回: "python数据分析" → Course{id=2}

// 校区匹配：同理
matchCampus("中关村校区怎么样")
  → 候选: "中关村校区"(5字符), "中关村"(3字符)
  → 返回: "中关村校区" → Campus{id=1}
```

### 5.3 信息提取流水线

在 `FunctionCallingChatServiceImpl.extractInfoFromMessage()` 中执行：

```
用户消息: "我叫张三，电话13800138000，我想学Python，在北京"
    │
    ├─ IntentMatcher.extractEntities()
    │   → interest="编程开发"
    │   → course="Python数据分析"
    │
    ├─ 正则提取姓名: "我叫张三" → customerName="张三"
    │
    ├─ 正则提取电话: "13800138000" → phone="13800138000"
    │
    ├─ lookupCourse("Python数据分析") → courseId=2
    │
    ├─ 校区: 消息中无校区关键词 → 未匹配
    │
    └─ 更新 SessionContext:
        context.customerName = "张三"
        context.phone = "13800138000"
        context.interest = "编程开发"
        context.selectedCourseId = 2
        context.selectedCourseName = "Python数据分析"
```

---

## 六、会话管理

### 6.1 当前实现（内存存储）

```
ConcurrentHashMap<String, SessionContext> sessionContexts
    │
    ├── key: sessionId (UUID)
    └── value: SessionContext
            ├── sessionId
            ├── customerName, phone, education, interest
            ├── selectedCourseId, selectedCourseName
            ├── selectedCampusId, selectedCampusName
            ├── reservationId
            ├── pendingUpdate (待确认修改)
            ├── pendingCancelReason (待确认取消)
            └── conversationHistory (最近20条)
```

**双层存储**：
1. `SessionContext` — 应用内存（ConcurrentHashMap），存储业务上下文
2. `ChatMemory` — Spring AI InMemoryChatMemory，存储对话历史（供AI上下文）

### 6.2 会话生命周期

```
用户首次访问
    │
    ├─ 前端可传入 sessionId，不传则后端自动生成 (UUID)
    ├─ 发送消息 → 后端 computeIfAbsent(sessionId)
    ├─ SessionContext 初始化
    │
对话进行中
    │
    ├─ 每次消息更新 SessionContext
    ├─ ChatMemory 自动保存对话历史
    │
会话结束（Phase 1: Redis持久化后）
    │
    ├─ 30分钟无操作 → 自动过期清理
    └─ 过期前保存对话历史到 chat_message 表
```

### 6.3 SessionContext 数据流

```
┌─────────────┐     extractInfoFromMessage()     ┌─────────────────┐
│  用户消息     │ ──────────────────────────────→ │  SessionContext  │
│ "我叫张三..." │                                  │                 │
└─────────────┘                                  │  customerName:  │
                                                 │   "张三"         │
┌─────────────┐     buildSystemMessage()          │  phone:         │
│  System      │ ←────────────────────────────── │   "138001..."   │
│  Prompt      │  注入已知信息摘要                  │  interest:      │
│  (动态拼接)   │                                  │   "编程开发"     │
└──────┬──────┘                                  │  courseId: 2    │
       │                                         └─────────────────┘
       ▼
┌─────────────┐     processResponse()             ┌─────────────────┐
│  AI 回复      │ ──────────────────────────────→ │  SessionContext  │
│  (自然语言)   │  提取预约ID等                     │  reservationId  │
└─────────────┘                                  │   = 123         │
                                                 └─────────────────┘
```

---

## 七、数据库架构

### 7.1 ER关系图

```
province (1) ──── (N) city
    │                    │
    │                    │
campus ──────────────── campus_course ──── course
    │                         │
    │                         │
    │                    course_schedule
    │
reservation ──── reservation_log
    │
    └── phone ──── (逻辑关联) ──── customer.phone
    │
customer


新增表（Phase 1-3）:
chat_message ──── session_id (逻辑关联)
leave_message ──── session_id (逻辑关联)
faq (独立)
```

### 7.2 表结构概览

| 表名 | 记录量级 | 索引策略 | 说明 |
|------|---------|----------|------|
| province | 34 | PK | 省份数据，基本不变 |
| city | 53 | PK, idx_province_id | 城市数据，基本不变 |
| course_category | 6 | PK | 课程分类 |
| course | ~50 | PK, idx_category_id | 课程信息 |
| campus | ~20 | PK, idx_province_id, idx_city_id | 校区信息 |
| campus_course | ~100 | PK, uk_campus_course, idx_campus_id, idx_course_id | 校区-课程关联 |
| course_schedule | ~600 | PK, idx_campus_course_id | 课程时间段 |
| customer | 增长 | PK | 客户信息 |
| reservation | 增长 | PK | 预约记录 |
| reservation_log | 增长 | PK, idx_reservation_id | 预约变更日志 |

### 7.3 关键查询场景

| 场景 | SQL路径 | 频率 |
|------|---------|------|
| 搜索课程 | course WHERE name LIKE / category_id = ? | 高 |
| 查校区课程 | campus_course WHERE campus_id = ? AND course_id = ? | 高 |
| 查时间段 | course_schedule WHERE campus_course_id = ? AND status = 1 | 中 |
| 创建预约 | INSERT reservation | 中 |
| 查预约 | reservation WHERE phone = ? ORDER BY create_time DESC LIMIT 1 | 中 |
| 查校区 | campus WHERE city_id = ? AND status = 1 | 中 |
| 查有校区的省份 | SELECT DISTINCT province_id FROM campus | 低（启动时加载） |

---

## 八、API接口设计

### 8.1 当前已有接口

| 方法 | 路径 | 说明 | 入参 | 出参 |
|------|------|------|------|------|
| POST | `/api/chat/send` | 发送对话消息 | ChatRequest{sessionId?, message, mode} | ChatResponse{sessionId, reply, mode} |
| GET | `/api/chat/session/{sessionId}` | 获取会话信息 | path: sessionId | {sessionId, status} |

### 8.2 待开发接口（按需求文档）

#### Phase 1: 核心闭环

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/customer/phone/{phone}` | 根据手机号查询客户 |
| GET | `/api/reservation/phone/{phone}` | 查询手机号的所有预约 |
| POST | `/api/leave-message` | 提交留言 |

#### Phase 2: 体验提升

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/chat/send` (SSE) | 流式对话响应 |
| GET | `/api/chat/history/{sessionId}` | 查询对话历史 |
| GET | `/api/chat/history` | 按条件分页查询对话 |

#### Phase 3: 管理后台

| 方法 | 路径 | 说明 |
|------|------|------|
| GET/POST/PUT/DELETE | `/api/admin/course/**` | 课程CRUD |
| GET/POST/PUT/DELETE | `/api/admin/campus/**` | 校区CRUD |
| GET/PUT | `/api/admin/reservation/**` | 预约管理 |
| GET | `/api/admin/customer/**` | 客户列表 |
| GET/PUT | `/api/admin/prompt` | 系统提示词管理 |
| GET | `/api/admin/statistics` | 预约统计数据 |
| GET/POST/PUT/DELETE | `/api/admin/faq/**` | FAQ管理 |
| GET/PUT | `/api/admin/leave-message/**` | 留言管理 |

#### Phase 4: 安全

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 管理员登录 |
| POST | `/api/auth/refresh` | 刷新Token |

### 8.3 统一响应格式

**当前格式**（对话接口直接返回 ChatResponse）：

```json
{
  "sessionId": "uuid",
  "reply": "AI回复内容",
  "mode": "function"
}
```

**异常响应**（GlobalExceptionHandler 统一处理）：

```json
{
  "error": "参数错误",
  "message": "请求的数据不存在或服务异常，请稍后重试"
}
```

> 后续管理后台接口建议统一为 `{code, message, data}` 格式，与现有对话接口并存。

---

## 九、前端架构

### 9.1 当前实现

单文件SPA（`static/index.html`，730行），内嵌 CSS + JS：

```
┌─ index.html ────────────────────────────────────┐
│                                                   │
│  ┌─ HTML结构 ──────────────────────────────────┐ │
│  │  侧边栏: 模式切换 + 快捷回复                 │ │
│  │  聊天区: 消息气泡 + 头像 + 时间戳             │ │
│  │  输入区: 文本框 + 发送按钮                    │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  ┌─ CSS样式 ──────────────────────────────────┐  │
│  │  响应式布局、气泡样式、动画效果               │  │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  ┌─ JavaScript ───────────────────────────────┐  │
│  │  sendMessage() → POST /api/chat/send        │  │
│  │  renderMessage() → 消息气泡渲染              │  │
│  │  renderTable() → 表格数据渲染                │  │
│  │  模式切换、快捷回复、输入动画                 │  │
│  └─────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────┘
```

### 9.2 待开发：管理后台前端

| Phase | 页面 | 技术选型建议 |
|-------|------|-------------|
| Phase 3 | 课程管理 | Vue 3 + Element Plus |
| Phase 3 | 校区管理 | Vue 3 + Element Plus |
| Phase 3 | 预约管理 | Vue 3 + Element Plus |
| Phase 3 | 客户管理 | Vue 3 + Element Plus |
| Phase 3 | 对话记录 | Vue 3 + Element Plus |
| Phase 3 | 留言管理 | Vue 3 + Element Plus |
| Phase 3 | FAQ管理 | Vue 3 + Element Plus |
| Phase 3 | 提示词管理 | Vue 3 + Element Plus + Monaco Editor |
| Phase 4 | 登录页 | Vue 3 + Element Plus |

---

## 十、关键设计决策

### 10.1 为什么选 Function Calling 而不是指令解析？

| 对比项 | Function Calling | 指令解析 |
|--------|-----------------|----------|
| 参数校验 | Spring AI + JSON Schema 自动校验 | 人工正则解析，易出错 |
| 新增函数 | 写 `@Bean` + `@Description` 即可 | 需改提示词 + 解析逻辑 + 前端 |
| AI决策 | AI自主决定调哪个函数、传什么参数 | AI按固定格式输出指令，格式错误即失败 |
| 可维护性 | 函数定义与业务逻辑合一 | 提示词与执行逻辑分离，易不同步 |
| 可观测性 | 函数调用有日志，参数明确 | 指令字符串难以追踪 |

### 10.2 为什么 SessionContext 存内存而不是数据库？

**当前决策**：ConcurrentHashMap 内存存储

**原因**：
1. 单机部署，不需要分布式会话
2. 会话上下文是临时的、非关键数据
3. 重启丢失可接受（用户可重新开始对话）
4. 避免引入 Redis 的运维成本

**Phase 1 升级路径**：Redis 存储，支持会话持久化和重启恢复

### 10.3 为什么 IntentMatcher 混用注解和数据库？

| 数据类型 | 加载方式 | 原因 |
|----------|---------|------|
| 意图（CONFIRM/CANCEL） | @Intent 注解 | 关键词固定，代码即配置 |
| 兴趣/学历分类 | @EntityExtract 注解 | 分类固定，不会频繁变化 |
| 课程列表 | 数据库动态加载 | 课程会增删改，需从数据库读 |
| 校区列表 | 数据库动态加载 | 校区会增删改，需从数据库读 |

### 10.4 System Prompt 设计策略

```
System Prompt = 基础提示词 + 已知信息 + 预约状态 + 可用函数说明
```

- **基础提示词**：定义AI角色、核心规则、交互流程（静态，存在代码常量中）
- **已知信息**：从 SessionContext 动态拼接（"已知用户信息：姓名张三、电话..."）
- **预约状态**：是否有待修改/取消的预约
- **可用函数说明**：每个函数的用途和参数说明（帮助AI决定调哪个函数）

**关键约束**：
- 每次请求都完整拼接 System Prompt（不复用）
- 已知信息注入后，AI不再重复询问
- 函数说明帮助AI理解何时调用哪个函数

---

## 十一、安全设计

### 11.1 当前安全措施

| 措施 | 实现 | 状态 |
|------|------|------|
| SQL注入防护 | MyBatis-Plus 参数化查询 | ✅ 已实现 |
| API参数校验 | @Valid + Jakarta Validation | ✅ 已实现 |
| 全局异常处理 | GlobalExceptionHandler | ✅ 已实现 |
| API Key安全 | 环境变量 DEEPSEEK_API_KEY | ✅ 已实现 |
| 错误信息脱敏 | 异常处理器不暴露堆栈 | ✅ 已实现 |

### 11.2 待实现（Phase 4）

| 措施 | 说明 |
|------|------|
| JWT鉴权 | 管理后台接口需登录 |
| API限流 | 单用户10次/分钟，全局500次/分钟 |
| XSS防护 | 用户输入HTML转义 |
| 日志脱敏 | 手机号显示为 138****8000 |
| CORS配置 | 限制允许的跨域来源 |

---

## 十二、部署架构

### 12.1 单机部署（当前）

```
┌──────────────────────────────────┐
│           单台服务器               │
│                                   │
│  ┌─────────────────────────────┐ │
│  │   Spring Boot (port:8082)   │ │
│  └──────────┬──────────────────┘ │
│             │                     │
│  ┌──────────▼──────────────────┐ │
│  │   MySQL (port:3306)         │ │
│  │   ai_customer database      │ │
│  └─────────────────────────────┘ │
│                                   │
│  ┌─────────────────────────────┐ │
│  │   Nginx (port:80)           │ │
│  │   静态文件 + 反向代理        │ │
│  └─────────────────────────────┘ │
└──────────────────────────────────┘
```

### 12.2 生产部署（Phase 1+）

```
┌─────────────┐     ┌─────────────┐
│   Nginx      │────→│ Spring Boot │ ×2 (负载均衡)
│   (port:80)  │     │ (port:8082) │
└─────────────┘     └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │  MySQL    │ │  Redis   │ │ DeepSeek │
        │ (主从)    │ │ (缓存)   │ │  (远程)  │
        └──────────┘ └──────────┘ └──────────┘
```

### 12.3 环境配置

| 环境 | 端口 | 数据库 | Redis | 说明 |
|------|------|--------|-------|------|
| dev | 8082 | localhost:3306/ai_customer | 无 | 本地开发 |
| test | 8082 | 测试服务器MySQL | 测试Redis | 集成测试 |
| prod | 8082 | 生产MySQL | 生产Redis | 正式环境 |

---

## 十三、监控与可观测性

### 13.1 当前日志

```java
// Function Calling函数调用日志
log.info("Function Calling - 创建预约: name={}, phone={}", ...);
log.info("Function Calling - 搜索课程: keyword={}", ...);
log.info("Function Calling AI响应: {}", responseText);
```

### 13.2 待建设（Phase 4）

| 指标 | 采集方式 | 告警阈值 |
|------|---------|---------|
| AI响应时间 | AOP切面 + Micrometer | P99 > 10s |
| Function Calling成功率 | 日志统计 | 成功率 < 95% |
| API QPS | Prometheus | > 500次/分钟 |
| 异常率 | GlobalExceptionHandler | > 1% |
| 数据库连接池 | HikariCP Metrics | 活跃连接 > 80% |

---

## 十四、已知技术债务

| 问题 | 位置 | 影响 | 修复方案 | 优先级 |
|------|------|------|---------|--------|
| pom.xml source/target=8 | pom.xml:115-116 | 项目无法编译 | 改为17 | P0 |
| ChatServiceImpl 硬编码课程/校区ID | ChatServiceImpl:419-447 | 新增课程/校区无法识别 | 改用IntentMatcher动态查询或合并到FunctionCallingChatService | P1 |
| ChatServiceImpl 与 FunctionCallingChatServiceImpl 功能重复 | service/impl/ | 维护两套代码，逻辑不同步 | 合并为一个实现，废弃指令解析模式 | P2 |
| InMemoryChatMemory | ChatClientConfig.java | 重启丢失对话历史 | Phase 1 改用 Redis | P1 |
| ConcurrentHashMap 会话存储 | FunctionCallingChatServiceImpl:44 | 重启丢失会话 | Phase 1 改用 Redis | P1 |
| ChatRequest.mode 默认值为 instruction | ChatRequest.java:22 | 默认走指令解析模式，非主推的Function Calling模式 | 改默认值为 "function" 或前端强制传入 | P2 |
| 无测试用例 | src/test/ | 无法保证代码质量 | 补充单元测试+集成测试 | P1 |
| Spring AI M6 版本 | pom.xml:22 | API可能变更 | 锁定版本，升级前评估 | P2 |

---

## 十五、扩展点

### 15.1 新增 Function Calling 函数

1. 在 `function/` 包下创建新的 Functions 类
2. 定义 Request/Response 静态内部类
3. 用 `@Bean` + `@Description` 注册函数
4. 在 `FunctionCallingChatServiceImpl.chat()` 的 `defaultFunctions()` 中添加函数名
5. 在 System Prompt 中添加函数使用说明

### 15.2 新增意图/实体类型

1. 在 `IntentMatcher` 中添加 `@Intent` 或 `@EntityExtract` 注解方法
2. 重启后自动生效

### 15.3 接入新AI模型

1. 修改 `application.yml` 中的 `spring.ai.openai.*` 配置
2. 更换对应的 Spring AI starter 依赖
3. Function Calling 接口不变（Spring AI 抽象层）

### 15.4 接入新渠道

1. 新增 Controller（如 `WechatController`）
2. 复用 `FunctionCallingChatService.chat(sessionId, message)`
3. 渠道无关的业务逻辑已解耦
