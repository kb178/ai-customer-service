# AI智能客服系统

基于Spring Boot 3 + Spring AI + DeepSeek的智能客服系统。

## 技术栈

- Spring Boot 3.4.5
- Spring AI 1.0.0-M6
- DeepSeek大模型
- MySQL 8.0
- MyBatis Plus 3.5.7
- Lombok

## 功能特性

### AI智能对话
- 了解用户兴趣、学历等信息
- 根据用户需求推荐课程
- 引导用户预约试听
- 引导用户留下联系方式

### 数据库操作
- 查询课程信息
- 查询校区信息
- 新增预约单
- 保存客户信息

## 快速开始

### 1. 环境要求
- JDK 17+
- Maven 3.8+
- MySQL 8.0+

### 2. 创建数据库
执行 `src/main/resources/schema.sql` 创建数据库和表

### 3. 配置DeepSeek API Key
设置环境变量 `DEEPSEEK_API_KEY` 或修改 `application.yml`

### 4. 启动项目
```bash
mvn spring-boot:run
```

### 5. 测试对话
```bash
curl -X POST http://localhost:8080/api/chat/send \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，我想了解一下课程"}'
```

## 项目结构

```
src/main/java/com/aicustomer/
├── AiCustomerServiceApplication.java  # 启动类
├── config/
│   └── MybatisPlusConfig.java         # MyBatis Plus配置
├── controller/
│   └── ChatController.java            # 对话接口
├── entity/
│   ├── Course.java                    # 课程实体
│   ├── Campus.java                    # 校区实体
│   ├── Reservation.java               # 预约实体
│   └── Customer.java                  # 客户实体
├── mapper/
│   ├── CourseMapper.java
│   ├── CampusMapper.java
│   ├── ReservationMapper.java
│   └── CustomerMapper.java
└── service/
    ├── ChatService.java               # 对话服务接口
    ├── CourseService.java             # 课程服务接口
    ├── CampusService.java             # 校区服务接口
    ├── ReservationService.java        # 预约服务接口
    ├── CustomerService.java           # 客户服务接口
    └── impl/
        ├── ChatServiceImpl.java       # 对话服务实现
        ├── CourseServiceImpl.java
        ├── CampusServiceImpl.java
        ├── ReservationServiceImpl.java
        └── CustomerServiceImpl.java
```
