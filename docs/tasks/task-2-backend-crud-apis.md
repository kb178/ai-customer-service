# 任务2：后端 CRUD 管理 API

## 目标

实现课程、校区、预约、客户、留言、对话记录 6 个模块的管理后台 API。

## 前置条件

- 任务1已完成（AdminResponse、PageResult、AdminInterceptor 已就位）
- 项目可正常启动

## 通用约定

- 所有 Controller 放在 `controller/admin` 包
- 所有接口返回 `AdminResponse<T>` 格式
- 分页接口返回 `AdminResponse<PageResult<T>>`
- 使用 MyBatis-Plus 的 `Page<T>` 和 `LambdaQueryWrapper` 实现分页查询
- 分页参数：`page`（默认1）、`size`（默认10）
- 每个 Controller 使用 `@RestController`、`@RequestMapping`、`@RequiredArgsConstructor`

**Controller 模板**：
```java
package com.aicustomer.controller.admin;

import com.aicustomer.service.XxxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/xxx")
@RequiredArgsConstructor
public class AdminXxxController {

    private final XxxService xxxService;

    // 接口方法...
}
```

**分页参数模板**：
```java
@GetMapping("/list")
public AdminResponse<PageResult<Xxx>> list(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name) {
    if (page < 1) page = 1;
    if (size < 1) size = 10;
    if (size > 100) size = 100;

    Page<Xxx> pageParam = new Page<>(page, size);
    LambdaQueryWrapper<Xxx> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(StringUtils.isNotBlank(name), Xxx::getName, name)
           .orderByDesc(Xxx::getCreateTime);
    Page<Xxx> result = xxxService.page(pageParam, wrapper);
    return AdminResponse.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, size));
}
```

## 任务清单

### 2.1 课程管理

新建 `controller/admin/AdminCourseController.java`：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/course/list` | page, size, name(可选), categoryId(可选) | 分页查询课程 |
| GET | `/api/admin/course/{id}` | - | 课程详情 |
| POST | `/api/admin/course` | @RequestBody Course | 新增课程 |
| PUT | `/api/admin/course/{id}` | @RequestBody Course | 修改课程 |
| DELETE | `/api/admin/course/{id}` | - | 删除课程（逻辑删除） |
| GET | `/api/admin/course-category/list` | - | 查询所有课程分类 |

复用现有 `CourseService`、`CourseCategoryService`。

### 2.2 校区管理

新建 `controller/admin/AdminCampusController.java`：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/campus/list` | page, size, name(可选), cityId(可选) | 分页查询校区 |
| GET | `/api/admin/campus/{id}` | - | 校区详情 |
| POST | `/api/admin/campus` | @RequestBody Campus | 新增校区 |
| PUT | `/api/admin/campus/{id}` | @RequestBody Campus | 修改校区 |
| DELETE | `/api/admin/campus/{id}` | - | 删除校区 |
| GET | `/api/admin/campus/{id}/courses` | - | 校区开设的课程 |

复用现有 `CampusService`、`CampusCourseService`。校区课程查询：
```java
List<CampusCourse> list = campusCourseService.lambdaQuery()
    .eq(CampusCourse::getCampusId, id)
    .list();
```

### 2.3 预约管理

新建 `controller/admin/AdminReservationController.java`：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/reservation/list` | page, size, phone(可选), status(可选), startDate(可选), endDate(可选) | 分页查询预约 |
| GET | `/api/admin/reservation/{id}` | - | 预约详情 |
| PUT | `/api/admin/reservation/{id}/confirm` | - | 确认预约（状态→1） |
| PUT | `/api/admin/reservation/{id}/cancel` | - | 取消预约（状态→3） |
| PUT | `/api/admin/reservation/{id}/complete` | - | 标记完成（状态→2） |

**状态变更实现模板**（confirm/cancel/complete 通用）：
```java
@PutMapping("/{id}/confirm")
public AdminResponse<Void> confirm(@PathVariable Long id) {
    Reservation reservation = reservationService.getById(id);
    if (reservation == null) {
        return AdminResponse.error(404, "预约不存在");
    }
    int oldStatus = reservation.getStatus();
    reservation.setStatus(BizConstants.STATUS_CONFIRMED);
    reservationService.updateById(reservation);

    // 记录状态变更日志
    ReservationLog log = new ReservationLog();
    log.setReservationId(id);
    log.setOldStatus(oldStatus);
    log.setNewStatus(BizConstants.STATUS_CONFIRMED);
    log.setOperator("admin");
    log.setRemark("管理员确认预约");
    reservationLogService.save(log);

    return AdminResponse.ok();
}
```

状态常量参考 `BizConstants`：
- `STATUS_PENDING = 0`（待确认）
- `STATUS_CONFIRMED = 1`（已确认）
- `STATUS_COMPLETED = 2`（已完成）
- `STATUS_CANCELLED = 3`（已取消）

**日期筛选实现**：
```java
// startDate 格式 "yyyy-MM-dd"，转为当天 00:00:00
if (StringUtils.isNotBlank(startDate)) {
    wrapper.ge(Reservation::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
}
// endDate 格式 "yyyy-MM-dd"，转为当天 23:59:59
if (StringUtils.isNotBlank(endDate)) {
    wrapper.le(Reservation::getCreateTime, LocalDate.parse(endDate).atTime(LocalTime.MAX));
}
```

### 2.4 客户管理

新建 `controller/admin/AdminCustomerController.java`：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/customer/list` | page, size, name(可选), phone(可选) | 分页查询客户 |
| GET | `/api/admin/customer/{id}` | - | 客户详情 |
| GET | `/api/admin/customer/{id}/reservations` | - | 客户的预约记录 |

复用现有 `CustomerService`、`ReservationService`。客户预约记录：
```java
List<Reservation> reservations = reservationService.lambdaQuery()
    .eq(Reservation::getPhone, customer.getPhone())
    .orderByDesc(Reservation::getCreateTime)
    .list();
```

### 2.5 留言管理

新建 `controller/admin/AdminLeaveMessageController.java`：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/leave-message/list` | page, size, status(可选) | 分页查询留言 |
| GET | `/api/admin/leave-message/{id}` | - | 留言详情 |
| PUT | `/api/admin/leave-message/{id}/handle` | @RequestBody HandleRequest | 处理留言 |

**HandleRequest DTO**（放在 `controller/admin` 包）：
```java
package com.aicustomer.controller.admin;

import lombok.Data;

@Data
public class HandleRequest {
    /** 处理人 */
    private String handler;
    /** 处理备注 */
    private String handleRemark;
    /** 状态：1处理中 2已解决 3已忽略 */
    private Integer status;
}
```

处理留言实现：
```java
@PutMapping("/{id}/handle")
public AdminResponse<Void> handle(@PathVariable Long id, @RequestBody HandleRequest request) {
    LeaveMessage msg = leaveMessageService.getById(id);
    if (msg == null) {
        return AdminResponse.error(404, "留言不存在");
    }
    msg.setHandler(request.getHandler());
    msg.setHandleRemark(request.getHandleRemark());
    msg.setStatus(request.getStatus());
    msg.setHandleTime(LocalDateTime.now());
    leaveMessageService.updateById(msg);
    return AdminResponse.ok();
}
```

### 2.6 对话记录

新建 `controller/admin/AdminConversationController.java`：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/conversation/list` | page, size, phone(可选), sessionId(可选), startDate(可选), endDate(可选) | 分页查询对话 |
| GET | `/api/admin/conversation/session/{sessionId}` | - | 会话完整时间线 |

**对话列表聚合查询**（按 sessionId 分组）：

需要在 `ConversationLogService` 中新增方法：
```java
// ConversationLogService 接口新增
IPage<Map<String, Object>> selectSessionList(IPage<Map<String, Object>> page, String phone, String sessionId, LocalDateTime startDate, LocalDateTime endDate);
```

ConversationLogServiceImpl 实现：
```java
@Override
public IPage<Map<String, Object>> selectSessionList(IPage<Map<String, Object>> page, String phone, String sessionId, LocalDateTime startDate, LocalDateTime endDate) {
    LambdaQueryWrapper<ConversationLog> wrapper = new LambdaQueryWrapper<>();
    wrapper(StringUtils.isNotBlank(phone), ConversationLog::getCustomerPhone, phone)
       .eq(StringUtils.isNotBlank(sessionId), ConversationLog::getSessionId, sessionId)
       .ge(startDate != null, ConversationLog::getCreateTime, startDate)
       .le(endDate != null, ConversationLog::getCreateTime, endDate);

    // 先查所有符合条件的记录，再 Java Stream 聚合
    List<ConversationLog> all = list(wrapper);
    Map<String, List<ConversationLog>> grouped = all.stream()
        .collect(Collectors.groupingBy(ConversationLog::getSessionId));

    List<Map<String, Object>> records = grouped.entrySet().stream()
        .map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", entry.getKey());
            map.put("customerPhone", entry.getValue().get(0).getCustomerPhone());
            map.put("messageCount", entry.getValue().size());
            map.put("firstTime", entry.getValue().stream().map(ConversationLog::getCreateTime).min(LocalDateTime::compareTo).orElse(null));
            map.put("lastTime", entry.getValue().stream().map(ConversationLog::getCreateTime).max(LocalDateTime::compareTo).orElse(null));
            return map;
        })
        .sorted((a, b) -> ((LocalDateTime) b.get("lastTime")).compareTo((LocalDateTime) a.get("lastTime")))
        .collect(Collectors.toList());

    // 手动分页
    int total = records.size();
    int from = (int) ((page.getCurrent() - 1) * page.getSize());
    int to = Math.min(from + (int) page.getSize(), records.size());
    List<Map<String, Object>> pageRecords = from < total ? records.subList(from, to) : Collections.emptyList();

    return new Page<Map<String, Object>>(page.getCurrent(), page.getSize(), total).setRecords(pageRecords);
}
```

**会话详情**：
```java
@GetMapping("/session/{sessionId}")
public AdminResponse<List<ConversationLog>> sessionDetail(@PathVariable String sessionId) {
    List<ConversationLog> logs = conversationLogService.lambdaQuery()
        .eq(ConversationLog::getSessionId, sessionId)
        .orderByAsc(ConversationLog::getCreateTime)
        .list();
    return AdminResponse.ok(logs);
}
```

## 新建文件清单

| 文件 | 包 | 说明 |
|------|-----|------|
| AdminCourseController.java | controller/admin | 课程管理 |
| AdminCampusController.java | controller/admin | 校区管理 |
| AdminReservationController.java | controller/admin | 预约管理 |
| AdminCustomerController.java | controller/admin | 客户管理 |
| AdminLeaveMessageController.java | controller/admin | 留言管理 |
| HandleRequest.java | controller/admin | 留言处理 DTO |
| AdminConversationController.java | controller/admin | 对话记录 |

## 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| ConversationLogService.java | 新增 selectSessionList 方法 |
| ConversationLogServiceImpl.java | 实现 selectSessionList |

## 注意事项

- 所有删除操作使用 MyBatis-Plus 的逻辑删除（`@TableLogic`），不需要手动处理
- 预约状态变更必须同时写入 reservation_log 表
- 分页参数要做边界检查：page < 1 时设为 1，size < 1 时设为 10，size > 100 时设为 100
- 日期参数格式：`yyyy-MM-dd`，用 `LocalDate.parse()` 解析
- `StringUtils` 使用 `org.springframework.util.StringUtils`
- 所有 Controller 使用 `@RequiredArgsConstructor` + `final` 字段注入
