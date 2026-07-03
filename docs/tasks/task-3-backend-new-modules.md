# 任务3：后端新模块（FAQ、提示词、统计）

## 目标

实现 FAQ 知识库管理、系统提示词管理、统计面板 3 个新模块的后端 API。这些模块需要新建数据库表和完整的 Entity/Mapper/Service/Controller。

## 前置条件

- 任务1已完成（AdminResponse、PageResult 已就位）
- MySQL 数据库 `ai_customer` 已启动

## 通用约定

- 所有 Controller 放在 `controller/admin` 包
- 所有接口返回 `AdminResponse<T>` 格式
- 新建的 Entity/Mapper/Service 遵循项目现有模式（参考 Customer/Course 等）

## 任务清单

### 3.1 FAQ 知识库

**数据库表** — 追加到 `schema-admin.sql`：
```sql
CREATE TABLE IF NOT EXISTS `faq` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question` VARCHAR(500) NOT NULL COMMENT '问题',
    `answer` TEXT NOT NULL COMMENT '答案',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
    `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词(逗号分隔)',
    `weight` DOUBLE DEFAULT 1.0 COMMENT '匹配权重',
    `status` TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FAQ知识库表';
```

**Faq.java**（放在 `entity` 包）：
```java
package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("faq")
public class Faq {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问题 */
    private String question;

    /** 答案 */
    private String answer;

    /** 分类 */
    private String category;

    /** 关键词(逗号分隔) */
    private String keywords;

    /** 匹配权重 */
    private Double weight;

    /** 状态：1启用 0禁用 */
    private Integer status;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

**FaqMapper.java**（放在 `mapper` 包）：
```java
package com.aicustomer.mapper;

import com.aicustomer.entity.Faq;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FaqMapper extends BaseMapper<Faq> {
}
```

**FaqService.java**（放在 `service` 包）：
```java
package com.aicustomer.service;

import com.aicustomer.entity.Faq;
import com.baomidou.mybatisplus.extension.service.IService;

public interface FaqService extends IService<Faq> {
}
```

**FaqServiceImpl.java**（放在 `service/impl` 包）：
```java
package com.aicustomer.service.impl;

import com.aicustomer.entity.Faq;
import com.aicustomer.mapper.FaqMapper;
import com.aicustomer.service.FaqService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FaqServiceImpl extends ServiceImpl<FaqMapper, Faq> implements FaqService {
}
```

**AdminFaqController.java**（放在 `controller/admin` 包）：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/faq/list` | page, size, question(可选), category(可选), status(可选) | 分页查询FAQ |
| GET | `/api/admin/faq/{id}` | - | FAQ详情 |
| POST | `/api/admin/faq` | @RequestBody Faq | 新增FAQ |
| PUT | `/api/admin/faq/{id}` | @RequestBody Faq | 修改FAQ |
| DELETE | `/api/admin/faq/{id}` | - | 删除FAQ |

### 3.2 系统提示词管理

**数据库表** — 追加到 `schema-admin.sql`：
```sql
CREATE TABLE IF NOT EXISTS `system_prompt` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `content` TEXT NOT NULL COMMENT '提示词内容',
    `version` INT NOT NULL COMMENT '版本号',
    `is_active` TINYINT DEFAULT 0 COMMENT '1当前生效 0历史版本',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统提示词版本表';
```

**SystemPrompt.java**（放在 `entity` 包）：
```java
package com.aicustomer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("system_prompt")
public class SystemPrompt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 提示词内容 */
    private String content;

    /** 版本号 */
    private Integer version;

    /** 是否当前生效：1是 0否 */
    @TableField("is_active")
    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

**SystemPromptMapper.java**（放在 `mapper` 包）：
```java
package com.aicustomer.mapper;

import com.aicustomer.entity.SystemPrompt;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemPromptMapper extends BaseMapper<SystemPrompt> {
}
```

**SystemPromptService.java**（放在 `service` 包）：
```java
package com.aicustomer.service;

import com.aicustomer.entity.SystemPrompt;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface SystemPromptService extends IService<SystemPrompt> {

    /** 获取当前生效的提示词 */
    SystemPrompt getActivePrompt();

    /** 获取最近10个历史版本 */
    List<SystemPrompt> getHistory();

    /** 更新提示词（创建新版本，旧版本标记为非生效） */
    void updatePrompt(String content);

    /** 回滚到指定版本 */
    void rollback(Long id);
}
```

**SystemPromptServiceImpl.java**（放在 `service/impl` 包）：

```java
package com.aicustomer.service.impl;

import com.aicustomer.entity.SystemPrompt;
import com.aicustomer.mapper.SystemPromptMapper;
import com.aicustomer.service.SystemPromptService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemPromptServiceImpl extends ServiceImpl<SystemPromptMapper, SystemPrompt> implements SystemPromptService {

    @Override
    public SystemPrompt getActivePrompt() {
        return lambdaQuery().eq(SystemPrompt::getIsActive, 1).one();
    }

    @Override
    public List<SystemPrompt> getHistory() {
        return lambdaQuery()
                .eq(SystemPrompt::getIsActive, 0)
                .orderByDesc(SystemPrompt::getVersion)
                .last("LIMIT 10")
                .list();
    }

    @Override
    @Transactional
    public void updatePrompt(String content) {
        // 获取当前最大版本号
        SystemPrompt current = getActivePrompt();
        int newVersion = (current != null ? current.getVersion() : 0) + 1;

        // 将当前生效的标记为历史版本
        if (current != null) {
            current.setIsActive(0);
            updateById(current);
        }

        // 创建新版本
        SystemPrompt prompt = new SystemPrompt();
        prompt.setContent(content);
        prompt.setVersion(newVersion);
        prompt.setIsActive(1);
        save(prompt);
    }

    @Override
    @Transactional
    public void rollback(Long id) {
        SystemPrompt target = getById(id);
        if (target == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 将当前生效的标记为历史
        SystemPrompt current = getActivePrompt();
        if (current != null) {
            current.setIsActive(0);
            updateById(current);
        }

        // 获取最大版本号 + 1
        SystemPrompt latest = lambdaQuery().orderByDesc(SystemPrompt::getVersion).last("LIMIT 1").one();
        int newVersion = (latest != null ? latest.getVersion() : 0) + 1;

        // 创建回滚版本（内容来自目标版本，版本号递增）
        SystemPrompt rollback = new SystemPrompt();
        rollback.setContent(target.getContent());
        rollback.setVersion(newVersion);
        rollback.setIsActive(1);
        save(rollback);
    }
}
```

**AdminPromptController.java**（放在 `controller/admin` 包）：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/prompt` | - | 获取当前生效的提示词 |
| GET | `/api/admin/prompt/history` | - | 获取最近10个历史版本 |
| PUT | `/api/admin/prompt` | @RequestBody PromptRequest | 更新提示词 |
| PUT | `/api/admin/prompt/rollback/{versionId}` | - | 回滚到指定版本 |

**PromptRequest DTO**（放在 `controller/admin` 包）：
```java
package com.aicustomer.controller.admin;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PromptRequest {
    @NotBlank(message = "提示词内容不能为空")
    private String content;
}
```

### 3.3 统计面板

**StatisticsService.java**（放在 `service` 包）— 纯查询服务，不继承 IService：
```java
package com.aicustomer.service;

import java.util.List;
import java.util.Map;

public interface StatisticsService {

    /** 概览数据：今日/本周/本月预约数 */
    Map<String, Object> getOverview();

    /** 预约状态分布 */
    List<Map<String, Object>> getReservationStatus();

    /** 热门课程 TOP5 */
    List<Map<String, Object>> getTopCourses();

    /** 热门校区 TOP5 */
    List<Map<String, Object>> getTopCampuses();

    /** 预约转化率（咨询→预约） */
    Map<String, Object> getConversion();
}
```

**StatisticsServiceImpl.java**（放在 `service/impl` 包）：

```java
package com.aicustomer.service.impl;

import com.aicustomer.entity.Reservation;
import com.aicustomer.mapper.ReservationMapper;
import com.aicustomer.mapper.CustomerMapper;
import com.aicustomer.service.StatisticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final ReservationMapper reservationMapper;
    private final CustomerMapper customerMapper;

    @Override
    public Map<String, Object> getOverview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        long todayCount = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreateTime, todayStart));
        long weekCount = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreateTime, weekStart));
        long monthCount = reservationMapper.selectCount(
            new LambdaQueryWrapper<Reservation>().ge(Reservation::getCreateTime, monthStart));
        long totalCustomers = customerMapper.selectCount(null);

        Map<String, Object> result = new HashMap<>();
        result.put("todayReservations", todayCount);
        result.put("weekReservations", weekCount);
        result.put("monthReservations", monthCount);
        result.put("totalCustomers", totalCustomers);
        return result;
    }

    @Override
    public List<Map<String, Object>> getReservationStatus() {
        List<Reservation> all = reservationMapper.selectList(null);
        Map<Integer, Long> grouped = all.stream()
            .collect(Collectors.groupingBy(Reservation::getStatus, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        String[] statusNames = {"待确认", "已确认", "已完成", "已取消"};
        for (int i = 0; i < statusNames.length; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", i);
            item.put("name", statusNames[i]);
            item.put("count", grouped.getOrDefault(i, 0L));
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTopCourses() {
        List<Reservation> all = reservationMapper.selectList(null);
        Map<Long, Long> grouped = all.stream()
            .filter(r -> r.getCourseId() != null)
            .collect(Collectors.groupingBy(Reservation::getCourseId, Collectors.counting()));

        return grouped.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("courseId", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTopCampuses() {
        List<Reservation> all = reservationMapper.selectList(null);
        Map<Long, Long> grouped = all.stream()
            .filter(r -> r.getCampusId() != null)
            .collect(Collectors.groupingBy(Reservation::getCampusId, Collectors.counting()));

        return grouped.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(entry -> {
                Map<String, Object> item = new HashMap<>();
                item.put("campusId", entry.getKey());
                item.put("count", entry.getValue());
                return item;
            })
            .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getConversion() {
        long totalCustomers = customerMapper.selectCount(null);
        long totalReservations = reservationMapper.selectCount(null);
        double rate = totalCustomers > 0 ? (double) totalReservations / totalCustomers * 100 : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalCustomers", totalCustomers);
        result.put("totalReservations", totalReservations);
        result.put("conversionRate", Math.round(rate * 100.0) / 100.0);
        return result;
    }
}
```

**AdminStatisticsController.java**（放在 `controller/admin` 包）：

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| GET | `/api/admin/statistics/overview` | - | 概览数据 |
| GET | `/api/admin/statistics/reservation-status` | - | 预约状态分布 |
| GET | `/api/admin/statistics/top-courses` | - | 热门课程 TOP5 |
| GET | `/api/admin/statistics/top-campuses` | - | 热门校区 TOP5 |
| GET | `/api/admin/statistics/conversion` | - | 预约转化率 |

## 新建文件清单

| 文件 | 包 | 说明 |
|------|-----|------|
| Faq.java | entity | FAQ实体 |
| FaqMapper.java | mapper | FAQ Mapper |
| FaqService.java | service | FAQ Service接口 |
| FaqServiceImpl.java | service/impl | FAQ Service实现 |
| AdminFaqController.java | controller/admin | FAQ管理API |
| SystemPrompt.java | entity | 提示词实体 |
| SystemPromptMapper.java | mapper | 提示词Mapper |
| SystemPromptService.java | service | 提示词Service接口 |
| SystemPromptServiceImpl.java | service/impl | 提示词Service实现 |
| AdminPromptController.java | controller/admin | 提示词管理API |
| PromptRequest.java | controller/admin | 提示词更新DTO |
| StatisticsService.java | service | 统计Service接口 |
| StatisticsServiceImpl.java | service/impl | 统计Service实现 |
| AdminStatisticsController.java | controller/admin | 统计面板API |

## 修改文件清单

| 文件 | 修改内容 |
|------|----------|
| schema-admin.sql | 追加 faq 和 system_prompt 表 DDL |

## 注意事项

- `SystemPromptServiceImpl` 中的 `updatePrompt` 和 `rollback` 方法需要 `@Transactional`
- `StatisticsServiceImpl` 的查询在数据量小时用 Java Stream 聚合即可，不需要写自定义 SQL
- `SystemPrompt` 实体的 `isActive` 字段需要 `@TableField("is_active")` 注解指定列名
- 所有新表执行：`mysql -u root -p ai_customer < src/main/resources/schema-admin.sql`
