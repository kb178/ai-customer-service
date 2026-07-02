# AI智能客服系统 - 需求分析文档

| 版本 | 日期 | 变更说明 |
|------|------|----------|
| v1.0 | 2026-07-02 | 初稿 |
| v2.0 | 2026-07-02 | 全面修订：补充项目背景、数据字典、非功能性需求、状态机、安全合规、测试体系、风险解决方案 |

---

## 一、项目概述

### 1.1 项目背景

教育培训机构（编程/设计/数据类课程）需要一套智能客服系统，通过AI自动完成课程咨询、预约管理等高频重复工作，降低人工客服成本，提升响应速度。

### 1.2 业务目标

- 用户通过Web聊天窗口与AI对话，完成课程咨询、预约试听、修改/取消预约等操作
- AI通过Function Calling直接操作业务数据库，无需人工介入
- 运营人员通过管理后台管理课程、校区、预约数据，查看对话记录

### 1.3 使用人群

| 角色 | 说明 | 接入方式 |
|------|------|----------|
| 潜在学员 | 浏览课程、咨询、预约试听 | Web聊天窗口 |
| 已报名学员 | 查询/修改/取消预约 | Web聊天窗口 |
| 运营人员 | 管理课程/校区/预约数据、查看对话 | 管理后台 |
| 系统管理员 | 配置系统参数、查看监控 | 管理后台 |

### 1.4 明确不做的功能

- 不做人工客服坐席系统（仅做转接通知+上下文导出）
- 不做微信公众号/小程序接入（仅Web端）
- 不做支付/退款流程（预约仅试听，不涉及付费）
- 不做多语言支持（仅中文）
- 不做语音对话（仅文字）

---

## 二、已有功能盘点

### 2.1 Function Calling函数（11个）

| 模块 | 函数名 | 功能 | 入参 | 出参 |
|------|--------|------|------|------|
| 预约 | `createReservation` | 创建课程预约 | customerName, phone, courseId, campusId, scheduleId?, appointmentTime? | success, message, reservationId |
| 预约 | `updateReservation` | 修改已有预约 | reservationId, customerName?, phone?, courseId?, campusId? | success, message |
| 预约 | `cancelReservation` | 取消预约 | reservationId, reason | success, message |
| 预约 | `queryReservation` | 查询预约信息 | reservationId?, phone? | found, reservationId, customerName, phone, courseName, campusName, status, appointmentTime |
| 课程 | `searchCourses` | 搜索课程 | keyword?, categoryId? | courses, total |
| 课程 | `getCategories` | 获取课程分类 | 无 | categories, total |
| 课程 | `getCourseSchedules` | 获取课程时间段 | campusId, courseId | found, schedules, total |
| 校区 | `getProvinces` | 获取有校区的省份 | 无 | provinces, total |
| 校区 | `getCities` | 获取省份下的城市 | provinceId | cities, total |
| 校区 | `getCampuses` | 获取校区列表 | provinceId?, cityId?, courseId? | campuses, total |
| 校区 | `getCampusCourses` | 获取校区开设的课程 | campusId | campusCourses, total |

### 2.2 对话系统

- **两种对话模式**：指令解析模式（ChatServiceImpl）+ Function Calling模式（FunctionCallingChatServiceImpl）
- **多轮对话记忆**：SessionContext + Spring AI ChatMemory
- **意图识别**：注解驱动（IntentMatcher）+ 数据库动态加载课程/校区关键词
- **实体提取**：兴趣、学历（注解定义）、课程/校区（数据库动态匹配）
- **会话状态管理**：支持预约修改/取消的二次确认机制

### 2.3 前端界面

完整聊天UI，包含：消息气泡、头像、时间戳、模式切换（指令解析/Function Calling）、快捷回复、表格渲染、输入动画、响应式布局。

### 2.4 数据库（10张表）

| 表名 | 用途 | 初始数据 |
|------|------|----------|
| province | 省份 | 34个 |
| city | 城市 | 53个 |
| course_category | 课程分类 | 6个 |
| course | 课程 | 8门 |
| campus | 校区 | 10个 |
| campus_course | 校区课程关联 | 40条 |
| course_schedule | 课程时间段 | 自动生成 |
| customer | 客户信息 | 无 |
| reservation | 预约记录 | 无 |
| reservation_log | 预约状态变更日志 | 无 |

### 2.5 基础设施

全局异常处理、MyBatis-Plus配置（逻辑删除、自动填充）、业务常量管理（BizConstants）、API参数校验（@Valid）。

---

## 三、数据字典与状态枚举

### 3.1 统一术语

| 术语 | 定义 | 使用场景 |
|------|------|----------|
| 学员 | 已注册或咨询过的客户，对应customer表 | 数据库、后端代码 |
| 用户 | 当前正在与AI对话的人（可能是学员也可能是新访客） | 前端、对话场景 |
| 会话 | 一次完整的对话过程，以sessionId标识 | 全局 |
| 预约 | 学员预约试听课程的记录 | 业务层 |

### 3.2 预约状态枚举（reservation.status）

| 值 | 含义 | 触发条件 |
|----|------|----------|
| 0 | 待确认 | 创建预约时默认 |
| 1 | 已确认 | 管理员确认 |
| 2 | 已完成 | 试听结束后 |
| 3 | 已取消 | 学员或管理员取消 |

### 3.3 会话步骤枚举（SessionContext.currentStep）

| 值 | 含义 | 可执行操作 |
|----|------|-----------|
| CONSULT | 咨询阶段 | 课程查询、校区查询、FAQ问答 |
| SELECT_COURSE | 选课阶段 | 课程搜索、课程详情、分类查询 |
| SELECT_CAMPUS | 选校区阶段 | 校区筛选、校区课程查询 |
| SELECT_TIME | 选时间阶段 | 时间段查询 |
| FILL_INFO | 填信息阶段 | 姓名、手机号输入 |
| CONFIRM | 确认阶段 | 创建预约 |
| MODIFY | 修改阶段 | 修改已有预约 |
| CANCEL | 取消阶段 | 取消预约 |
| LEAVE_MESSAGE | 留言阶段 | 记录学员问题，提示客服跟进 |

### 3.4 客户来源枚举（customer.source）

| 值 | 含义 |
|----|------|
| web_chat | Web聊天窗口 |
| function_calling | Function Calling模式对话 |
| instruction | 指令解析模式对话 |
| admin | 管理员手动录入 |

### 3.5 留言状态枚举（leave_message.status）

| 值 | 含义 |
|----|------|
| 0 | 待处理 |
| 1 | 处理中 |
| 2 | 已解决 |
| 3 | 已忽略 |

---

## 四、非功能性需求

### 4.1 性能要求

| 指标 | 要求 |
|------|------|
| 单次AI响应时间 | ≤10秒（不含Function Calling执行） |
| Function Calling执行时间 | ≤3秒/次 |
| SSE首字节时间 | ≤2秒 |
| 并发会话数 | 支持100个并发会话 |
| API QPS | 单用户≤10次/分钟，全局≤500次/分钟 |

### 4.2 可用性要求

| 指标 | 要求 |
|------|------|
| 系统可用性 | ≥99.5%（月度） |
| Redis故障降级 | 自动降级为内存存储，不影响核心对话功能 |
| AI调用失败降级 | 返回友好提示"AI暂时繁忙，请稍后再试" |
| 数据库故障 | 返回系统繁忙提示，不暴露内部错误 |

### 4.3 兼容性要求

| 类别 | 要求 |
|------|------|
| 浏览器 | Chrome 90+、Edge 90+、Firefox 88+、Safari 14+ |
| 分辨率 | 最低1280×720，支持响应式 |
| Java版本 | 17 |
| MySQL版本 | 8.0+ |
| Redis版本 | 6.0+（如启用持久化） |

### 4.4 数据存储要求

| 数据类型 | 保留周期 | 说明 |
|----------|----------|------|
| 会话上下文 | 30分钟无操作过期 | Redis自动清理 |
| 对话消息 | 180天 | 超期自动归档到历史表 |
| 预约记录 | 永久 | 逻辑删除，不物理清除 |
| 客户信息 | 永久 | 逻辑删除，不物理清除 |
| 操作日志 | 365天 | 定期归档 |

---

## 五、缺失功能分析

### 5.1 第一梯队：核心业务闭环

#### 5.1.1 客户查询函数（CustomerFunctions）

**现状问题**：AI无法识别回头客，每次对话都是陌生人。

**需求描述**：
- 新增 `queryCustomerByPhone` 函数：根据手机号查询学员信息（姓名、兴趣、学历、来源）
- 新增 `listReservationsByPhone` 函数：查询某学员的所有预约记录列表
- AI在对话开始时自动识别已注册学员

**触发时机**：学员首次提供手机号时，AI调用 `queryCustomerByPhone` 查询，若存在则主动问候"您好，XX同学"。

**追问逻辑**：
- 手机号格式错误（非11位数字）→ 提示"请输入正确的11位手机号"
- 手机号不存在 → 提示"未找到该客户信息，请确认手机号是否正确"
- 查询成功 → 将客户信息写入SessionContext，后续对话自动使用

**重复校验规则**：
- 同一手机号多次咨询时，不重复创建customer记录
- 每次对话更新customer的最近咨询时间

**涉及模块**：
- 后端：新增 `CustomerFunctions.java`，注册到 `FunctionCallingChatServiceImpl`
- Service层：CustomerService补充查询方法
- 系统提示词：添加客户识别说明

**前置条件**：customer表已有数据。

**后置条件**：SessionContext中填充学员信息，AI后续对话可使用。

**验收标准**：
- 学员提供手机号后，AI能查询并记住学员信息
- 学员说"我有哪些预约"，AI能返回完整预约列表
- 手机号不存在时，AI提示"未找到该客户信息"而非报错
- 同一手机号不会重复创建customer记录

#### 5.1.2 留言功能

**现状问题**：AI无法处理的问题没有兜底方案，学员的问题可能被忽略。

**需求描述**：
- 新增 `leaveMessage` 函数：学员留言记录问题
- AI无法回答时，主动建议"我帮您记录下来，客服会在2小时内回复您"
- 管理后台可查看留言列表，运营人员手动跟进处理

**触发时机**：
- 学员主动说"留言"、"我有问题"、"帮我记一下"
- AI连续2次无法理解学员意图
- 涉及退款、特殊优惠等AI无权限处理的业务
- 学员表达不满（如"投诉"、"不满意"等关键词）

**留言表结构**：

```json
{
  "id": 1,
  "sessionId": "session_xxx",
  "customerName": "张三",
  "customerPhone": "138****8000",
  "message": "我想了解Python课程的退款政策",
  "category": "退款咨询",
  "status": 0,
  "createTime": "2026-07-02T10:30:00"
}
```

**管理后台处理流程**：
- 留言列表按时间倒序展示，支持按状态筛选
- 运营人员可标记留言为"处理中"、"已解决"、"已忽略"
- 已解决的留言可添加处理备注

**涉及模块**：
- 数据库：新增leave_message表
- 后端：新增LeaveMessageService + leaveMessage函数
- 管理后台：留言管理页面

**前置条件**：SessionContext已存在。

**后置条件**：留言记录写入leave_message表，学员收到确认提示。

**验收标准**：
- AI无法回答时，主动建议学员留言
- 学员说"留言"，系统记录问题并确认
- 管理后台能看到所有留言，状态可更新
- 留言后AI继续正常服务，不阻塞后续对话

### 5.2 第二梯队：体验提升

#### 5.2.1 流式响应（SSE）

**现状问题**：AI全量返回，回复长文本时用户等待时间长。

**需求描述**：
- 后端接口改为SSE（Server-Sent Events）流式输出
- 前端逐字/逐句显示AI回复
- Function Calling执行期间显示loading

**断线缓存规则**：
- 后端缓存最近5条AI回复（内存，按sessionId索引）
- 前端断线重连时，请求最近消息接口补全缺失内容
- 缓存消息24小时后自动清除

**消息不丢失规则**：
- SSE连接断开期间，AI回复存入chat_message表
- 重连后前端拉取缺失的消息记录
- Function Calling执行结果不受SSE连接状态影响

**涉及模块**：
- 后端：ChatController改为返回SseEmitter或Flux
- 前端：使用EventSource接收流式数据

**前置条件**：无。

**后置条件**：AI回复流式输出，前端逐字显示。

**验收标准**：
- AI回复逐字显示，无明显卡顿
- Function Calling执行时有loading提示
- 网络断开后自动重连，不丢失已显示内容
- 断线期间的AI回复可在重连后补全

#### 5.2.2 多轮引导状态机

**现状问题**：SessionContext有状态字段，但没有强制流程，AI可能跳步。

**需求描述**：
- 定义预约流程状态机，强制按步骤引导
- 支持中途切换话题、返回上一步、放弃预约

**状态流转图**：

```
CONSULT ──(说"报名/预约")──→ SELECT_COURSE
                                │
                    (选好课程)──→ SELECT_CAMPUS
                                │
                    (选好校区)──→ SELECT_TIME
                                │
                    (选好时间)──→ FILL_INFO
                                │
                    (填好信息)──→ CONFIRM
                                │
                    (确认)──────→ 完成（创建预约）

任意步骤 ──(说"返回上一步")──→ 上一步
任意步骤 ──(说"放弃/算了")──→ CONSULT
任意步骤 ──(说"留言/记录")──→ LEAVE_MESSAGE → CONSULT
MODIFY ──(修改完成)──→ CONSULT
CANCEL ──(取消完成)──→ CONSULT
```

**分支流程说明**：

| 场景 | 处理方式 |
|------|----------|
| 中途切换话题（如选课过程中问校区） | 暂存当前步骤，回答问题后返回继续 |
| 返回上一步 | currentStep回退，保留已填信息 |
| 放弃预约 | 清空预约相关信息，回到CONSULT |
| 提前填充信息（如选课时直接说了手机号） | 提前记录到SessionContext，到FILL_INFO步骤时自动使用 |
| 信息已齐全直接确认 | 跳过中间步骤，直接到CONFIRM |

**涉及模块**：
- 后端：SessionContext增加currentStep字段
- System Prompt：根据当前步骤动态调整AI行为
- IntentMatcher：增加步骤校验逻辑

**前置条件**：SessionContext已存在。

**后置条件**：currentStep更新为新步骤。

**验收标准**：
- 学员说"我想报名"，AI按顺序引导
- 不允许跳过必要步骤（如未选课程直接预约）
- 学员可随时说"返回上一步"回退
- 学员可随时说"放弃"退出预约流程
- 学员提前提供的信息会被记录，到对应步骤时自动使用

#### 5.2.3 对话记录持久化

**现状问题**：conversationHistory只存最近20条，且内存中，无法追溯。

**需求描述**：
- 对话消息存数据库，支持按会话ID、手机号、时间范围查询
- 支持分页查询，每页20条，默认按时间倒序

**涉及模块**：
- 数据库：新增chat_message表
- 后端：消息持久化逻辑+查询接口

**前置条件**：数据库已初始化。

**后置条件**：每条对话消息写入chat_message表。

**验收标准**：
- 所有对话消息持久化到数据库
- 能按条件分页查询历史对话
- 管理后台能展示完整对话时间线

### 5.3 第三梯队：运营支撑

#### 5.3.1 管理后台

**现状问题**：只有用户端聊天界面，管理员无法管理数据。

**需求描述**：
- 课程管理：增删改查课程信息
- 校区管理：增删改查校区信息
- 预约管理：查看/确认/取消预约
- 客户管理：查看学员列表、咨询记录
- 对话记录：按会话/手机号/时间查询历史对话
- 系统提示词管理：在线修改AI系统提示词

**提示词生效范围**：
- 修改后仅对新会话生效，已有会话继续使用旧提示词
- 支持查看当前生效的提示词版本

**提示词版本回滚**：
- 保留最近10个版本的历史提示词
- 支持一键回滚到指定版本

**对话导出格式与脱敏**：
- 支持导出为CSV/JSON格式
- 导出时手机号中间4位用*替换（138****8000）
- 姓名保留姓，名用*替换（张*）

**预约统计指标**：
- 今日/本周/本月预约数
- 预约转化率（咨询→预约）
- 热门课程TOP5
- 热门校区TOP5
- 预约状态分布（待确认/已确认/已完成/已取消）

**涉及模块**：
- 前端：管理后台页面（建议使用Vue Element Plus Admin）
- 后端：管理接口（课程/校区/预约/客户CRUD、提示词管理、统计接口）

**前置条件**：管理后台账号体系（本项目使用简单密码验证，不做复杂权限）。

**后置条件**：管理员可管理所有业务数据。

**验收标准**：
- 管理员能登录后台管理所有数据
- 能查看AI与学员的对话记录
- 能在线修改系统提示词，修改后新会话生效
- 能导出对话记录，手机号自动脱敏
- 能查看预约统计数据
- 删除操作有二次确认

#### 5.3.2 FAQ知识库

**现状问题**：AI靠系统提示词回答，没有结构化知识库。

**需求描述**：
- 常见问题（退款政策、请假规则、上课纪律等）走知识库匹配
- 支持运营人员在线管理FAQ
- 知识库优先匹配，匹配不到再走AI推理

**关键词匹配规则**：
- 精确匹配：问题关键词与FAQ问题完全一致，权重1.0
- 模糊匹配：问题包含FAQ关键词，权重0.5
- 同义词匹配：如"退钱"匹配"退款"，权重0.3
- 取权重最高的FAQ，权重相同时取分类更精确的

**多FAQ冲突处理**：
- 同时匹配到多个FAQ时，按权重排序取最高
- 权重差值<0.2时，合并多个FAQ答案一起返回
- 权重差值≥0.2时，只返回权重最高的

**多轮延伸交互逻辑**：
- FAQ匹配后，AI可追问"您是想了解退款的具体流程，还是退款时间？"
- 追问后继续匹配子FAQ
- 最多追问2轮，避免死循环

**涉及模块**：
- 数据库：新增faq表
- 后端：新增FaqService+queryFaq函数
- 管理后台：FAQ管理页面

**前置条件**：faq表已初始化。

**后置条件**：匹配到FAQ时直接返回答案，不调用AI。

**验收标准**：
- 常见问题秒级响应，不调用AI
- 运营人员能在线增删改FAQ
- FAQ匹配不到时，自动fallback到AI
- 多个FAQ冲突时，按权重合理处理

### 5.4 第四梯队：生产可用

#### 5.4.1 会话持久化

**现状问题**：ConcurrentHashMap存内存，服务重启后会话丢失。

**需求描述**：
- 会话数据（SessionContext）存Redis，设置过期时间
- 前端刷新页面后能恢复对话上下文

**会话自动续期规则**：
- 每次有新消息时，重置过期时间（30分钟）
- 无操作30分钟后自动过期清理

**Redis宕机降级行为**：
- 检测到Redis不可用时，自动切换为ConcurrentHashMap内存存储
- 降级时记录WARN日志
- Redis恢复后自动切换回来，已有内存会话不丢失

**分布式并发锁规则**：
- 同一会话的写操作加分布式锁（Redisson）
- 锁超时时间5秒，等待时间2秒
- 获取锁失败时返回"系统繁忙，请稍后重试"

**涉及模块**：
- 配置：引入Redis依赖
- 后端：SessionContext序列化存储，ChatMemory改用Redis实现

**前置条件**：Redis服务可用。

**后置条件**：会话数据存入Redis，支持过期清理。

**验收标准**：
- 服务重启后，进行中的对话不丢失
- 学员刷新页面后，能继续之前的对话
- 会话30分钟无操作自动清理
- Redis宕机时自动降级为内存存储，不影响核心对话

#### 5.4.2 鉴权与安全

**现状问题**：API无身份验证，无频率限制。

**需求描述**：
- 接口鉴权（JWT Token）
- API限流
- 敏感信息脱敏

**限流具体数值**：
- 单用户：10次/分钟（聊天接口）、30次/分钟（查询接口）
- 全局：500次/分钟
- 超限返回HTTP 429，提示"请求过于频繁，请稍后再试"

**Redis会话存储加密**：
- SessionContext序列化前AES加密
- 加密密钥从配置文件读取，不硬编码

**入参防注入**：
- 所有用户输入经过HTML转义
- SQL查询使用MyBatis-Plus参数化查询
- Function Calling参数做类型校验

**涉及模块**：
- 后端：新增JWT拦截器、限流过滤器
- 配置：限流规则、加密密钥

**前置条件**：无。

**后置条件**：未认证请求被拒绝，超限请求返回429。

**验收标准**：
- 未认证请求被拒绝（HTTP 401）
- 超限请求返回HTTP 429
- 日志中手机号脱敏显示（138****8000）
- 用户输入经过HTML转义，无XSS风险

#### 5.4.3 监控与告警

**需求描述**：
- AI调用耗时统计
- Function Calling成功率监控
- 异常自动告警

**涉及模块**：
- 后端：埋点日志
- 运维：接入监控系统

**前置条件**：监控系统可用（如Prometheus+Grafana）。

**后置条件**：关键指标可视化，异常自动通知。

**验收标准**：
- 能看到AI调用的平均耗时、成功率
- 异常时自动通知运维

#### 5.4.4 会话超时清理

**现状问题**：ConcurrentHashMap无清理机制，长时间会话占用内存。

**需求描述**：
- 会话30分钟无交互自动清理
- 清理前保存对话历史到数据库（如已开启持久化）

**涉及模块**：
- 后端：定时任务清理过期会话
- 配置：会话超时时间

**前置条件**：会话持久化已启用。

**后置条件**：过期会话被清理，资源释放。

**验收标准**：
- 超时会话自动释放资源
- 不影响正常使用的会话
- 清理前对话历史已保存到数据库

---

## 六、功能优先级与实施计划

### Phase 1：核心闭环（2周，9人天）

| 序号 | 功能 | 开发 | 测试 | 依赖 | 说明 |
|------|------|------|------|------|------|
| 1 | 会话持久化（Redis） | 3天 | 1天 | Redis环境 | 基础设施 |
| 2 | CustomerFunctions | 2天 | 0.5天 | 无 | 让AI有记忆 |
| 3 | listReservationsByPhone | 1天 | 0.5天 | 无 | 补全查询 |
| 4 | 留言功能 | 1.5天 | 0.5天 | 无 | 问题兜底 |
| 5 | 系统提示词优化 | 1天 | 0.5天 | 功能1、2、4 | 客户识别+留言触发 |

### Phase 2：体验提升（2周，7人天）

| 序号 | 功能 | 开发 | 测试 | 依赖 | 说明 |
|------|------|------|------|------|------|
| 6 | 流式响应（SSE） | 3天 | 1天 | 无 | 体验核心 |
| 7 | 多轮引导状态机 | 2天 | 1天 | 无 | 防止AI跳步 |
| 8 | 对话记录持久化 | 2天 | 0.5天 | 功能1 | 支持历史查询 |

### Phase 3：运营支撑（3周，11人天）

| 序号 | 功能 | 开发 | 测试 | 依赖 | 说明 |
|------|------|------|------|------|------|
| 9 | 管理后台后端接口 | 3天 | 1天 | 无 | CRUD+统计 |
| 10 | 管理后台前端 | 5天 | 2天 | 功能9 | 用现成模板 |
| 11 | FAQ知识库 | 3天 | 1天 | 无 | 秒级响应 |

### Phase 4：生产可用（1.5周，4人天）

| 序号 | 功能 | 开发 | 测试 | 依赖 | 说明 |
|------|------|------|------|------|------|
| 12 | 鉴权与限流 | 2天 | 1天 | 无 | 安全基础 |
| 13 | 监控与告警 | 2天 | 0.5天 | 监控系统 | 可观测性 |

### 工时汇总

| 阶段 | 开发 | 测试 | 合计 |
|------|------|------|------|
| Phase 1 | 9天 | 3.5天 | 12.5天 |
| Phase 2 | 7天 | 2.5天 | 9.5天 |
| Phase 3 | 11天 | 4天 | 15天 |
| Phase 4 | 4天 | 1.5天 | 5.5天 |
| **总计** | **31天** | **11.5天** | **42.5天** |

> 含测试约9周，加上联调、bug修复缓冲30%，实际约12周（3个月）。

---

## 七、数据库设计补充

### 7.1 chat_message表（新增）

```sql
CREATE TABLE chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    function_name VARCHAR(100) COMMENT '调用的函数名（如有）',
    function_result TEXT COMMENT '函数返回结果（如有）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';
```

### 7.2 faq表（新增）

```sql
CREATE TABLE faq (
    id BIGINT NOT NULL AUTO_INCREMENT,
    question VARCHAR(200) NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '答案',
    category VARCHAR(50) COMMENT '分类',
    keywords VARCHAR(500) COMMENT '关键词（逗号分隔）',
    weight DECIMAL(3,2) DEFAULT 1.00 COMMENT '权重（0-1）',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_category (category),
    FULLTEXT INDEX idx_question (question)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FAQ知识库表';
```

### 7.3 reservation_log表补充字段

```sql
ALTER TABLE reservation_log ADD COLUMN operator_name VARCHAR(50) COMMENT '操作人姓名';
ALTER TABLE reservation_log ADD COLUMN old_values TEXT COMMENT '变更前值（JSON）';
ALTER TABLE reservation_log ADD COLUMN new_values TEXT COMMENT '变更后值（JSON）';
```

### 7.4 leave_message表（新增）

```sql
CREATE TABLE leave_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    customer_name VARCHAR(50) COMMENT '学员姓名',
    customer_phone VARCHAR(20) COMMENT '联系电话',
    message TEXT NOT NULL COMMENT '留言内容',
    category VARCHAR(50) COMMENT '留言分类（退款咨询/课程问题/投诉建议等）',
    status TINYINT DEFAULT 0 COMMENT '状态：0待处理 1处理中 2已解决 3已忽略',
    handler VARCHAR(50) COMMENT '处理人',
    handle_remark TEXT COMMENT '处理备注',
    handle_time DATETIME COMMENT '处理时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言表';
```

### 7.5 历史数据归档策略

- chat_message表：超过180天的数据迁移到chat_message_archive表
- 使用定时任务（每月1日凌晨2点）执行归档
- 归档后原表数据物理删除

---

## 八、技术风险与解决方案

| 风险 | 影响 | 解决方案 |
|------|------|----------|
| Java版本不兼容（source/target=8） | 项目无法编译 | 修改pom.xml为source/target=17 |
| 空列表IN查询报错 | MySQL执行失败 | 调用listByIds前检查列表是否为空 |
| ChatServiceImpl硬编码课程/校区ID | 新增数据无法识别 | 改造为从数据库动态查询 |
| DeepSeek API不稳定 | 对话中断 | 加重试（最多3次）+ 降级提示 |
| AI幻觉（编造不存在的数据） | 返回错误信息 | Function Calling结果做数据库校验 |
| DeepSeek API限流 | 高并发被拒 | 请求排队 + 限流 + 缓存热门问答 |
| Token超限 | 长对话报错 | 控制历史消息≤20条，超长做摘要压缩 |
| ConcurrentHashMap并发串扰 | 会话数据混乱 | Redis持久化 + 分布式锁 |
| Redis宕机 | 会话数据丢失 | 自动降级为内存存储 + 日志告警 |
| 敏感信息泄露 | 合规风险 | 日志脱敏 + 入参校验 + SQL参数化 |

---

## 九、测试策略

### 9.1 单元测试

- Function Calling函数：参数校验、边界条件、异常返回
- IntentMatcher：意图识别准确率≥95%
- SessionContext：状态流转正确性

### 9.2 集成测试

- AI + Function Calling完整链路（模拟10轮对话）
- 多轮对话上下文传递
- 异常场景（AI调用失败、数据库异常、Redis宕机）

### 9.3 AI专项测试

- 意图识别测试集：准备50条标准对话，验证意图识别准确率
- 幻觉测试：验证AI不会编造不存在的课程/校区
- 多轮对话测试：验证5轮以上对话的上下文保持

### 9.4 功能测试

- 每个Function Calling函数的正常/异常流程
- 状态机所有状态流转路径
- 管理后台CRUD操作

### 9.5 性能测试

- 100并发会话的响应时间（≤10秒）
- Redis读写性能
- AI调用耗时统计

### 9.6 上线标准

**准入标准**：
- 所有P0需求开发完成
- 单元测试覆盖率≥60%
- AI专项测试通过率≥90%
- 无P0/P1级别bug

**准出标准**：
- 全量测试用例执行完毕
- 遗留bug均有明确修复计划
- 产品验收通过
- 灰度发布1天无异常

---

## 十、第三方对接规范

### 10.1 人工客服系统对接接口

后续如需对接第三方人工客服系统，需提供以下接口：

| 接口 | 方法 | 入参 | 出参 |
|------|------|------|------|
| 创建会话 | POST /api/agent/session | sessionId, customerInfo | agentSessionId |
| 获取坐席列表 | GET /api/agent/available | 无 | agents[] |
| 发送消息 | POST /api/agent/message | agentSessionId, content | success |
| 关闭会话 | POST /api/agent/close | agentSessionId | success |

### 10.2 对话上下文导出格式

```json
{
  "sessionId": "session_xxx",
  "customerId": 1,
  "customerName": "张三",
  "customerPhone": "138****8000",
  "transferTime": "2026-07-02T10:30:00",
  "transferReason": "学员主动要求",
  "messages": [
    {
      "role": "user",
      "content": "我想预约Python课",
      "timestamp": "2026-07-02T10:25:00"
    },
    {
      "role": "assistant",
      "content": "好的，请问您贵姓？",
      "timestamp": "2026-07-02T10:25:02"
    }
  ],
  "context": {
    "selectedCourse": "Python数据分析",
    "selectedCampus": "北京中关村校区",
    "pendingStep": "FILL_INFO"
  }
}
```
