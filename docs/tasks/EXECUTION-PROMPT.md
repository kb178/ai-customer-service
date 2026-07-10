# 后台管理系统开发执行 Prompt

## 角色

你是一个专业的全栈开发工程师，负责按照任务文档逐步实现 AI 智能客服系统的后台管理系统。你的工作方式是：**读任务 → 写代码 → 自检 → 修复 → 进入下一个任务**。

## 项目背景

- 项目：Spring Boot 3 + Spring AI + DeepSeek 的教育培训行业智能客服系统
- 位置：`F:\code\java开发\springboot3\ai-customer-service`
- 后端：Java 17, Spring Boot 3.4.5, MyBatis-Plus 3.5.7, MySQL（数据库名 `ai_customer`）, Redis
- 前端：Vue 3 + Element Plus + Vite（位于 `src/main/resources/admin-frontend/`）
- 后端端口：8082
- 前端端口：5173（Vite dev server）
- 现有 API：`POST /api/chat/send`（聊天接口，不能破坏）

## 目录结构

```
ai-customer-service/
├── src/main/java/com/aicustomer/          -- 后端 Java 代码
├── src/main/resources/
│   ├── static/index.html                  -- 现有聊天页面
│   ├── admin-frontend/                    -- 新建的管理后台前端
│   │   ├── package.json
│   │   ├── vite.config.js
│   │   ├── index.html
│   │   └── src/
│   │       ├── main.js
│   │       ├── App.vue
│   │       ├── router/index.js
│   │       ├── api/                       -- API 封装
│   │       ├── layout/AdminLayout.vue
│   │       └── views/                     -- 页面组件
│   ├── schema-complete.sql                -- 完整数据库脚本
│   ├── schema-admin.sql                   -- 管理后台新增表（需创建）
│   └── application.yml
├── docs/tasks/                            -- 任务文件
│   ├── task-1-backend-infrastructure.md
│   ├── task-2-backend-crud-apis.md
│   ├── task-3-backend-new-modules.md
│   ├── task-4-frontend-init.md
│   ├── task-5-frontend-pages.md
│   └── EXECUTION-PROMPT.md
└── pom.xml
```

## 任务文件位置

```
docs/tasks/
├── task-1-backend-infrastructure.md   -- 后端基础设施（统一响应、登录、拦截器、对话持久化）
├── task-2-backend-crud-apis.md        -- 后端 CRUD API（课程/校区/预约/客户/留言/对话）
├── task-3-backend-new-modules.md      -- 后端新模块（FAQ/提示词/统计，需新建表）
├── task-4-frontend-init.md            -- 前端项目初始化（Vite+Vue3+布局+登录）
├── task-5-frontend-pages.md           -- 前端管理页面（9个模块页面）
└── EXECUTION-PROMPT.md                -- 本文件（执行指南）
```

## 核心原则

1. **不破坏现有功能**：修改现有文件时，只改需要改的部分，不动其他代码
2. **先读后改**：修改任何文件前，必须先 read 确认当前内容
3. **每个任务独立验证**：完成一个任务后验证通过再开始下一个
4. **遇到问题记录但不卡住**：问题记在报告里，能修复就修复，不能修复标记为未解决继续下一个

## 执行流程

### 第一步：读取任务文件

```
1. 读取 docs/tasks/task-{N}.md 的全部内容
2. 理解任务目标、前置条件、任务清单、验证方式
3. 确认前置条件是否满足（检查相关文件是否存在）
4. 如果前置条件不满足 → 停止，告知用户缺少什么
```

### 第二步：逐步实现

按照任务文件中的"任务清单"逐项实现：

```
对于每个子任务（如 1.1、1.2、1.3）：
  1. 理解该子任务的要求
  2. 检查目标文件是否已存在
  3. 如果是"新建"：创建文件，写入代码
  4. 如果是"修改"：先读取现有文件，找到修改位置，精确修改
  5. 完成后立即进行单项自检
```

### 第三步：自检（每个子任务完成后）

```
自检清单：
  □ 文件是否创建在正确的路径？
  □ package 声明是否正确？
  □ import 是否完整？
  □ 注解是否正确（@Service, @Mapper, @RestController 等）？
  □ 字段类型是否与数据库一致？
  □ 方法签名是否与接口定义匹配？
  □ 是否有拼写错误？
  □ 代码风格是否与项目现有代码一致？
```

### 第四步：任务完成后全面自检

```
一个任务文件的所有子任务完成后：

1. 数据库检查（如涉及新表）：
   - 确认 schema-admin.sql 包含所有需要的建表语句
   - 执行 SQL 创建表：mysql -u root -p ai_customer < src/main/resources/schema-admin.sql
   - 确认表创建成功

2. 编译检查：
   - 后端任务：在项目根目录执行 mvn compile
   - 前端任务：在 src/main/resources/admin-frontend 目录执行 npm run build
   - 确认无编译/构建错误

3. 运行时检查：
   - 后端任务：执行 mvn spring-boot:run 启动应用
   - 检查启动日志是否有报错（特别关注 Bean 创建失败、SQL 异常）
   - 用 curl 测试相关 API（见下方测试命令模板）

4. 逻辑检查：
   - 对照任务文件中的"验证方式"逐项验证
   - 确认返回数据格式符合 AdminResponse<T> 规范
   - 确认分页接口返回 PageResult<T> 格式

5. 回归检查：
   - 确认现有聊天接口 POST /api/chat/send 仍然正常工作
   - 确认前端聊天页面 http://localhost:8082 仍然正常
```

### 第五步：修复问题

```
如果自检发现问题：
  1. 记录问题描述
  2. 定位问题原因（读报错日志、检查代码）
  3. 修复代码
  4. 重新自检
  5. 确认修复成功后继续

如果问题无法修复：
  1. 记录为"未解决问题"
  2. 继续下一个子任务
  3. 所有子任务完成后统一处理
```

### 第六步：进入下一个任务

```
确认当前任务完成且自检通过后：
  1. 输出完成报告（见下方模板）
  2. 读取下一个任务文件
  3. 重复第二步到第六步
```

## 任务执行顺序

```
task-1（后端基础设施）         ← 首先执行
  ↓ 前置条件
task-2（后端 CRUD API）       ← 依赖 task-1
task-3（后端新模块）          ← 依赖 task-1（与 task-2 可并行，但建议串行避免冲突）
  ↓ 后端完成
task-4（前端初始化）          ← 依赖 task-1（登录接口可用）
  ↓ 前置条件
task-5（前端管理页面）        ← 依赖 task-2, task-3, task-4
```

## 测试命令模板

### 后端 API 测试

```bash
# 1. 登录获取 session
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' \
  -c cookie.txt

# 2. 测试管理 API（带 cookie）
curl http://localhost:8082/api/admin/course/list -b cookie.txt

# 3. 测试管理 API（无 cookie，应返回 401）
curl http://localhost:8082/api/admin/course/list

# 4. 测试新增
curl -X POST http://localhost:8082/api/admin/course \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"name":"测试课程","price":9900,"duration":100}'

# 5. 测试修改
curl -X PUT http://localhost:8082/api/admin/course/1 \
  -H "Content-Type: application/json" \
  -b cookie.txt \
  -d '{"name":"修改后的课程名","price":8800}'

# 6. 测试删除
curl -X DELETE http://localhost:8082/api/admin/course/1 -b cookie.txt

# 7. 测试统计接口
curl http://localhost:8082/api/admin/statistics/overview -b cookie.txt
```

### 前端测试

```bash
# 1. 启动前端 dev server
cd src/main/resources/admin-frontend && npm run dev

# 2. 浏览器访问
#    http://localhost:5173 → 应跳转到登录页
#    输入 admin/123456 登录 → 应进入后台布局
#    点击各菜单项 → 应显示对应页面

# 3. 生产构建（可选）
cd src/main/resources/admin-frontend && npm run build
# 构建产物输出到 src/main/resources/static/admin/，Spring Boot 直接可访问
```

## 代码规范

### 后端

- 所有管理 Controller 放在 `controller/admin` 包
- 所有接口返回 `AdminResponse<T>` 格式
- 使用 `@RequiredArgsConstructor` + `final` 字段注入
- 使用 `LambdaQueryWrapper` 做类型安全查询
- 分页使用 MyBatis-Plus 的 `Page<T>`
- 删除使用逻辑删除（`@TableLogic`），不需要手动处理
- `StringUtils` 使用 `org.springframework.util.StringUtils`
- 日期参数格式：`yyyy-MM-dd`，用 `LocalDate.parse()` 解析

### 前端

- 使用 `<script setup>` 语法
- API 调用封装在 `src/main/resources/admin-frontend/src/api/` 目录
- 使用 `el-table` + `el-pagination` 做列表
- 弹窗表单使用 `el-dialog` + `el-form`
- 删除操作使用 `ElMessageBox.confirm` 二次确认
- 状态标签使用 `el-tag`，颜色映射：待确认(info)、已确认(primary)、已完成(success)、已取消(danger)
- 分页参数：page（从1开始）、size

## 自检报告模板

每个任务完成后，输出以下报告：

```
## 任务 {N} 完成报告

### 完成的子任务
- [x] 1.1 xxx
- [x] 1.2 xxx
- [ ] 1.3 xxx（未完成原因：xxx）

### 新建的文件
- src/main/java/com/aicustomer/controller/admin/XxxController.java
- src/main/java/com/aicustomer/entity/Xxx.java
- src/main/resources/admin-frontend/src/views/xxx/XxxList.vue

### 修改的文件
- src/main/java/com/aicustomer/service/impl/XxxServiceImpl.java（添加了 xxx 方法）

### 数据库变更
- schema-admin.sql 追加了 xxx 表

### 自检结果
- 数据库检查：✅ 通过 / ❌ 失败 / ⏭ 不涉及
- 编译检查：✅ 通过 / ❌ 失败（错误信息）
- 运行时检查：✅ 通过 / ❌ 失败（错误信息）
- 逻辑检查：✅ 通过 / ❌ 失败（哪个接口有问题）
- 回归检查：✅ 聊天接口正常 / ❌ 聊天接口异常

### 发现的问题及修复
- 问题1：xxx → 已修复
- 问题2：xxx → 未修复（原因：xxx）

### 状态：✅ 完成 / ⚠️ 部分完成 / ❌ 未完成
```

## 注意事项

1. **不要跳过任务**：按顺序执行，每个任务的前置条件必须满足
2. **不要跳过自检**：每个子任务完成后都要自检，不要等全部做完再检查
3. **修改现有文件时要先读取**：不要凭记忆修改，先 read 文件确认当前内容
4. **保持代码风格一致**：参考项目中已有的代码风格（注释、命名、格式）
5. **遇到问题不要卡住**：如果某个子任务有问题，记录后继续下一个，最后统一处理
6. **每次只处理一个任务文件**：完成一个再开始下一个，不要同时处理多个任务
7. **前端页面可以简化**：如果某个页面过于复杂，先实现核心功能（列表+分页+增删改），细节后续优化
8. **注意数据库编码**：所有新表使用 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`
9. **注意 Session 过期**：测试 API 时如果返回 401，可能需要重新登录获取新 cookie
10. **确认后端启动成功后再测试 API**：观察控制台日志，看到 "Started AiCustomerServiceApplication" 后再测试
11. **前端项目位于 resources 下**：`src/main/resources/admin-frontend/`，不要在项目根目录创建
12. **前端构建产物**：`npm run build` 输出到 `src/main/resources/static/admin/`，Spring Boot 可直接访问
