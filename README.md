# AI智能客服系统

基于Spring Boot 3 + Spring AI + DeepSeek的智能客服系统，使用Function Calling技术实现AI自动调用后端函数完成业务操作。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.4.5 | 基础框架 |
| Spring AI | 1.0.0-M6 | AI能力集成 |
| DeepSeek | - | 大语言模型 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存/会话存储 |
| MyBatis Plus | 3.5.7 | ORM框架 |
| Lombok | - | 代码简化 |
| JDK | 17+ | Java运行环境 |

## 功能特性

### AI智能对话（Function Calling模式）
- 自动识别用户意图，调用对应函数执行业务操作
- 支持课程咨询、校区查询、预约管理等场景
- 自动识别回头客，提供个性化服务
- 智能引导用户完成预约流程
- 支持留言功能，复杂问题自动转人工

### 核心业务功能
- **课程管理**：课程分类、课程搜索、校区课程查询
- **校区管理**：省份/城市/校区三级联动查询
- **预约管理**：创建/修改/取消预约
- **客户管理**：客户信息自动收集与识别
- **留言管理**：用户留言记录与处理
- **FAQ管理**：常见问题维护

### 管理后台
- 数据统计仪表盘
- 课程/校区/预约/客户管理
- 对话记录查看
- 留言处理
- 系统提示词配置

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 2. 创建数据库
```sql
CREATE DATABASE ai_customer DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
然后执行 `src/main/resources/schema.sql` 创建表结构

### 3. 配置环境变量
```bash
# 设置DeepSeek API Key
export DEEPSEEK_API_KEY=your_api_key_here
```

或修改 `application.yml` 中的数据库和Redis连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_customer
    username: root
    password: password
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 启动项目
```bash
mvn spring-boot:run
```

### 5. 访问应用
- **用户端聊天界面**: http://localhost:8082/chat.html
- **管理后台**: http://localhost:8082/admin/
- **API文档**: http://localhost:8082/api/chat/send

## API接口

### 发送消息
```
POST /api/chat/send
Content-Type: application/json

{
  "message": "你好，我想了解课程",
  "sessionId": "可选，不传自动生成"
}
```

**响应示例：**
```json
{
  "sessionId": "session_xxxxx",
  "reply": "您好！我是黑牛课程顾问...",
  "mode": "function"
}
```

### 获取会话信息
```
GET /api/chat/session/{sessionId}
```

## 项目结构

```
src/main/java/com/aicustomer/
├── AiCustomerServiceApplication.java    # 启动类
├── annotation/                          # 自定义注解
│   ├── Intent.java                      # 意图注解
│   ├── IntentMatcher.java               # 意图匹配器
│   ├── EntityExtract.java               # 实体提取注解
│   └── KeywordMapping.java              # 关键词映射注解
├── config/                              # 配置类
│   ├── BizConstants.java                # 业务常量
│   ├── ChatClientConfig.java            # ChatClient配置
│   ├── MybatisPlusConfig.java           # MyBatis Plus配置
│   ├── RedisConfig.java                 # Redis配置
│   ├── RedisChatMemory.java             # Redis对话记忆
│   ├── SessionContextService.java       # 会话上下文服务
│   └── WebMvcConfig.java                # Web配置
├── controller/                          # 控制器
│   ├── ChatController.java              # 对话接口
│   ├── ChatRequest.java                 # 请求DTO
│   ├── ChatResponse.java                # 响应DTO
│   └── admin/                           # 管理后台接口
│       ├── AdminLoginController.java
│       ├── AdminCourseController.java
│       ├── AdminCampusController.java
│       ├── AdminReservationController.java
│       ├── AdminCustomerController.java
│       ├── AdminConversationController.java
│       ├── AdminLeaveMessageController.java
│       ├── AdminFaqController.java
│       ├── AdminPromptController.java
│       └── AdminStatisticsController.java
├── entity/                              # 实体类
│   ├── Course.java                      # 课程
│   ├── CourseCategory.java              # 课程分类
│   ├── CourseSchedule.java              # 课程时间表
│   ├── Campus.java                      # 校区
│   ├── CampusCourse.java                # 校区课程关联
│   ├── Province.java                    # 省份
│   ├── City.java                        # 城市
│   ├── Reservation.java                 # 预约
│   ├── ReservationLog.java              # 预约日志
│   ├── Customer.java                    # 客户
│   ├── ConversationLog.java             # 对话日志
│   ├── LeaveMessage.java                # 留言
│   ├── Faq.java                         # 常见问题
│   ├── SystemPrompt.java                # 系统提示词
│   └── SessionContext.java              # 会话上下文
├── function/                            # Function Calling函数
│   ├── CourseFunctions.java             # 课程相关函数
│   ├── CampusFunctions.java             # 校区相关函数
│   ├── ReservationFunctions.java        # 预约相关函数
│   ├── CustomerFunctions.java           # 客户相关函数
│   └── LeaveMessageFunctions.java       # 留言相关函数
├── mapper/                              # MyBatis Mapper
│   ├── CourseMapper.java
│   ├── CourseCategoryMapper.java
│   ├── CourseScheduleMapper.java
│   ├── CampusMapper.java
│   ├── CampusCourseMapper.java
│   ├── ProvinceMapper.java
│   ├── CityMapper.java
│   ├── ReservationMapper.java
│   ├── ReservationLogMapper.java
│   ├── CustomerMapper.java
│   ├── ConversationLogMapper.java
│   ├── LeaveMessageMapper.java
│   ├── FaqMapper.java
│   └── SystemPromptMapper.java
├── service/                             # 服务接口
│   ├── FunctionCallingChatService.java  # 对话服务接口
│   ├── CourseService.java
│   ├── CourseCategoryService.java
│   ├── CourseScheduleService.java
│   ├── CampusService.java
│   ├── CampusCourseService.java
│   ├── ProvinceService.java
│   ├── CityService.java
│   ├── ReservationService.java
│   ├── ReservationLogService.java
│   ├── CustomerService.java
│   ├── ConversationLogService.java
│   ├── LeaveMessageService.java
│   ├── FaqService.java
│   ├── SystemPromptService.java
│   └── StatisticsService.java
└── service/impl/                        # 服务实现
    ├── FunctionCallingChatServiceImpl.java  # 核心对话实现
    ├── CourseServiceImpl.java
    ├── CourseCategoryServiceImpl.java
    ├── CourseScheduleServiceImpl.java
    ├── CampusServiceImpl.java
    ├── CampusCourseServiceImpl.java
    ├── ProvinceServiceImpl.java
    ├── CityServiceImpl.java
    ├── ReservationServiceImpl.java
    ├── ReservationLogServiceImpl.java
    ├── CustomerServiceImpl.java
    ├── ConversationLogServiceImpl.java
    ├── LeaveMessageServiceImpl.java
    ├── FaqServiceImpl.java
    ├── SystemPromptServiceImpl.java
    └── StatisticsServiceImpl.java
```

## Function Calling函数说明

| 函数名 | 说明 | 参数 |
|--------|------|------|
| searchCourses | 搜索课程 | keyword, categoryId |
| getCategories | 获取课程分类 | 无 |
| getCampusCourses | 获取校区课程 | campusId |
| getCourseSchedules | 获取课程时间表 | campusId, courseId |
| getProvinces | 获取省份列表 | 无 |
| getCities | 获取城市列表 | provinceId |
| getCampuses | 获取校区列表 | provinceId, cityId, courseId |
| createReservation | 创建预约 | customerName, phone, courseId, campusId, scheduleId |
| updateReservation | 修改预约 | reservationId, courseId, campusId |
| cancelReservation | 取消预约 | reservationId, reason |
| queryReservation | 查询预约 | reservationId, phone |
| queryCustomerByPhone | 查询客户 | phone |
| listReservationsByPhone | 查询客户预约 | phone |
| leaveMessage | 记录留言 | sessionId, customerName, customerPhone, message, category |

## 数据库设计

### 核心表
- `course` - 课程表
- `course_category` - 课程分类表
- `course_schedule` - 课程时间表
- `campus` - 校区表
- `campus_course` - 校区课程关联表
- `province` - 省份表
- `city` - 城市表
- `reservation` - 预约表
- `reservation_log` - 预约日志表
- `customer` - 客户表
- `conversation_log` - 对话日志表
- `leave_message` - 留言表
- `faq` - 常见问题表
- `system_prompt` - 系统提示词表

## 配置说明

### 应用配置 (application.yml)
```yaml
server:
  port: 8082

spring:
  data:
    redis:
      host: localhost
      port: 6379
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7

session:
  timeout-minutes: 30
```

## 核心业务功能详细文档

> 📖 完整的核心业务功能详解请查看 [CORE-FEATURES.md](./CORE-FEATURES.md)

涵盖以下内容：
- 对话核心流程（chat方法14步详解）
- Function Calling 机制原理
- 14个业务函数的参数、调用场景、内置校验
- 会话管理与 SessionContext 结构
- 自定义注解系统（@Intent、@EntityExtract）
- 预约确认机制（暂存→确认→执行）
- 对话记忆（Redis ChatMemory）
- 系统提示词设计
- 关键设计模式

## 开发说明

### 添加新的Function
1. 在 `function/` 目录下创建函数类
2. 使用 `@Bean` 注册函数
3. 在 `FunctionCallingChatServiceImpl` 中添加函数名到 `defaultFunctions` 列表

### 自定义系统提示词
系统提示词定义了AI的角色和行为规则，位于 `FunctionCallingChatServiceImpl.SYSTEM_PROMPT`。
可通过管理后台的"提示词配置"功能进行修改。
