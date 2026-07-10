# 任务5：前端管理页面

## 目标

实现 9 个管理模块的前端页面：课程管理、校区管理、预约管理、客户管理、对话记录、留言管理、FAQ管理、提示词管理、统计面板。替换任务4中的占位页面。

## 前置条件

- 任务4已完成（前端项目初始化、路由、布局已就位）
- 任务2、3已完成（后端 API 可用）

## 通用约定

- 每个页面使用 `el-table` + `el-pagination` 做列表
- 新增/编辑使用 `el-dialog` + `el-form` 弹窗表单
- 删除使用 `el-popconfirm` 二次确认
- 搜索使用 `el-input` + `el-select` + `el-button`
- 所有 API 调用封装在 `src/main/resources/admin-frontend/src/api/` 目录下
- 使用 `<script setup>` 语法

## API 封装模板

每个模块的 API 文件格式统一：

```javascript
// src/main/resources/admin-frontend/src/api/xxx.js
import request from './request'

export function getXxxList(params) {
  return request.get('/admin/xxx/list', { params })
}

export function getXxx(id) {
  return request.get(`/admin/xxx/${id}`)
}

export function createXxx(data) {
  return request.post('/admin/xxx', data)
}

export function updateXxx(id, data) {
  return request.put(`/admin/xxx/${id}`, data)
}

export function deleteXxx(id) {
  return request.delete(`/admin/xxx/${id}`)
}
```

## 任务清单

### 5.1 课程管理页面

**src/api/course.js**：
- `getCourseList(params)` — GET /admin/course/list
- `getCourse(id)` — GET /admin/course/{id}
- `createCourse(data)` — POST /admin/course
- `updateCourse(id, data)` — PUT /admin/course/{id}
- `deleteCourse(id)` — DELETE /admin/course/{id}
- `getCategories()` — GET /admin/course-category/list

**src/views/course/CourseList.vue**：

功能：
- 搜索栏：课程名称输入框 + 课程分类下拉 + 搜索按钮 + 新增按钮
- 表格列：ID、课程名称、分类、价格、课时、目标人群、状态、操作
- 操作列：编辑、删除
- 弹窗表单：课程名称、分类（下拉）、价格、课时、目标人群、最大学员数、状态（开关）、描述（文本域）
- 分页组件

### 5.2 校区管理页面

**src/api/campus.js**：
- `getCampusList(params)` — GET /admin/campus/list
- `getCampus(id)` — GET /admin/campus/{id}
- `createCampus(data)` — POST /admin/campus
- `updateCampus(id, data)` — PUT /admin/campus/{id}
- `deleteCampus(id)` — DELETE /admin/campus/{id}
- `getCampusCourses(id)` — GET /admin/campus/{id}/courses

**src/views/campus/CampusList.vue**：

功能：
- 搜索栏：校区名称 + 城市下拉 + 搜索 + 新增
- 表格列：ID、校区名称、地址、联系电话、营业时间、状态、操作
- 操作列：编辑、删除
- 弹窗表单：名称、地址、省份（下拉）、城市（下拉联动）、电话、营业时间、纬度、经度、状态
- 分页

### 5.3 预约管理页面

**src/api/reservation.js**：
- `getReservationList(params)` — GET /admin/reservation/list
- `getReservation(id)` — GET /admin/reservation/{id}
- `confirmReservation(id)` — PUT /admin/reservation/{id}/confirm
- `cancelReservation(id)` — PUT /admin/reservation/{id}/cancel
- `completeReservation(id)` — PUT /admin/reservation/{id}/complete

**src/views/reservation/ReservationList.vue**：

功能：
- 搜索栏：手机号 + 状态下拉（全部/待确认/已确认/已完成/已取消）+ 日期范围选择器 + 搜索
- 表格列：ID、客户姓名、手机号、课程ID、校区ID、预约时间、状态（Tag标签）、创建时间、操作
- 状态列使用 `el-tag`：待确认(info)、已确认(primary)、已完成(success)、已取消(danger)
- 操作列：确认（仅待确认状态显示）、完成（仅已确认状态显示）、取消（仅未完成/未取消状态显示）
- 确认/取消操作使用 `ElMessageBox.confirm` 二次确认
- 分页

### 5.4 客户管理页面

**src/api/customer.js**：
- `getCustomerList(params)` — GET /admin/customer/list
- `getCustomer(id)` — GET /admin/customer/{id}
- `getCustomerReservations(id)` — GET /admin/customer/{id}/reservations

**src/views/customer/CustomerList.vue**：

功能：
- 搜索栏：姓名 + 手机号 + 搜索
- 表格列：ID、姓名、手机号、邮箱、年龄、学历、兴趣、来源、创建时间、操作
- 操作列：查看详情（弹窗显示客户信息 + 预约记录列表）
- 分页

### 5.5 对话记录页面

**src/api/conversation.js**：
- `getConversationList(params)` — GET /admin/conversation/list
- `getSessionDetail(sessionId)` — GET /admin/conversation/session/{sessionId}

**src/views/conversation/ConversationList.vue**：

功能：
- 搜索栏：手机号 + sessionId + 日期范围 + 搜索
- 表格列：sessionId、手机号、消息数量、首次消息时间、最后消息时间、操作
- 操作列：查看详情（跳转到 ConversationDetail 页面）
- 分页

**src/views/conversation/ConversationDetail.vue**：

功能：
- 页面顶部：返回按钮 + sessionId 显示
- 对话时间线：使用 `el-timeline` 展示消息
  - 用户消息靠左，灰色气泡
  - AI回复靠右，蓝色气泡
  - 每条消息显示时间戳
- 通过路由参数获取 sessionId：`const route = useRoute(); const sessionId = route.params.sessionId`
- 页面加载时调用 `getSessionDetail(sessionId)` 获取对话记录

### 5.6 留言管理页面

**src/api/leaveMessage.js**：
- `getLeaveMessageList(params)` — GET /admin/leave-message/list
- `getLeaveMessage(id)` — GET /admin/leave-message/{id}
- `handleLeaveMessage(id, data)` — PUT /admin/leave-message/{id}/handle

**src/views/leaveMessage/LeaveMessageList.vue**：

功能：
- 搜索栏：状态下拉（全部/待处理/处理中/已解决/已忽略）+ 搜索
- 表格列：ID、会话ID、客户姓名、联系电话、留言内容（截断显示）、分类、状态（Tag）、处理人、创建时间、操作
- 状态列使用 `el-tag`：待处理(info)、处理中(warning)、已解决(success)、已忽略(info)
- 操作列：处理（弹窗填写处理人、备注、选择状态）
- 分页

### 5.7 FAQ管理页面

**src/api/faq.js**：
- `getFaqList(params)` — GET /admin/faq/list
- `getFaq(id)` — GET /admin/faq/{id}
- `createFaq(data)` — POST /admin/faq
- `updateFaq(id, data)` — PUT /admin/faq/{id}
- `deleteFaq(id)` — DELETE /admin/faq/{id}

**src/views/faq/FaqList.vue**：

功能：
- 搜索栏：问题关键词 + 分类下拉 + 状态下拉 + 搜索 + 新增
- 表格列：ID、问题、分类、关键词、权重、状态（开关）、排序、操作
- 操作列：编辑、删除
- 弹窗表单：问题、答案（文本域）、分类、关键词（逗号分隔）、权重（数字）、状态、排序
- 分页

### 5.8 提示词管理页面

**src/api/prompt.js**：
- `getActivePrompt()` — GET /admin/prompt
- `getPromptHistory()` — GET /admin/prompt/history
- `updatePrompt(data)` — PUT /admin/prompt
- `rollbackPrompt(versionId)` — PUT /admin/prompt/rollback/{versionId}

**src/views/prompt/PromptEdit.vue**：

功能：
- 左侧（主区域）：
  - 标题："当前生效的提示词"
  - 文本编辑器：`el-input` type="textarea"，rows=20
  - 保存按钮
- 右侧（历史版本列表）：
  - 标题："历史版本"
  - `el-table` 展示版本列表：版本号、创建时间、操作（回滚）
  - 回滚操作使用 `ElMessageBox.confirm` 二次确认
- 保存时调用 updatePrompt，回滚时调用 rollbackPrompt
- 页面加载时同时获取当前提示词和历史版本

### 5.9 统计面板页面

**src/api/statistics.js**：
- `getOverview()` — GET /admin/statistics/overview
- `getReservationStatus()` — GET /admin/statistics/reservation-status
- `getTopCourses()` — GET /admin/statistics/top-courses
- `getTopCampuses()` — GET /admin/statistics/top-campuses
- `getConversion()` — GET /admin/statistics/conversion

**src/views/Dashboard.vue**：

功能：
- 顶部统计卡片（`el-row` + `el-col`）：
  - 今日预约数、本周预约数、本月预约数、客户总数
  - 使用 `el-statistic` 组件
- 中间图表区（`el-row`）：
  - 左侧：预约状态分布（`el-table`，显示状态名称+数量）
  - 右侧：热门课程 TOP5（`el-table`，显示课程ID+数量）
- 底部：
  - 热门校区 TOP5
  - 预约转化率（客户总数、预约总数、转化率百分比）
- 页面加载时调用 5 个统计接口

## 新建/修改文件清单

### 新建 API 文件
| 文件 | 说明 |
|------|------|
| src/main/resources/admin-frontend/src/api/course.js | 课程 API |
| src/main/resources/admin-frontend/src/api/campus.js | 校区 API |
| src/main/resources/admin-frontend/src/api/reservation.js | 预约 API |
| src/main/resources/admin-frontend/src/api/customer.js | 客户 API |
| src/main/resources/admin-frontend/src/api/conversation.js | 对话记录 API |
| src/main/resources/admin-frontend/src/api/leaveMessage.js | 留言 API |
| src/main/resources/admin-frontend/src/api/faq.js | FAQ API |
| src/main/resources/admin-frontend/src/api/prompt.js | 提示词 API |
| src/main/resources/admin-frontend/src/api/statistics.js | 统计 API |

### 替换占位页面
| 文件 | 说明 |
|------|------|
| src/main/resources/admin-frontend/src/views/course/CourseList.vue | 课程管理（替换占位） |
| src/main/resources/admin-frontend/src/views/campus/CampusList.vue | 校区管理（替换占位） |
| src/main/resources/admin-frontend/src/views/reservation/ReservationList.vue | 预约管理（替换占位） |
| src/main/resources/admin-frontend/src/views/customer/CustomerList.vue | 客户管理（替换占位） |
| src/main/resources/admin-frontend/src/views/conversation/ConversationList.vue | 对话列表（替换占位） |
| src/main/resources/admin-frontend/src/views/conversation/ConversationDetail.vue | 对话详情（替换占位） |
| src/main/resources/admin-frontend/src/views/leaveMessage/LeaveMessageList.vue | 留言管理（替换占位） |
| src/main/resources/admin-frontend/src/views/faq/FaqList.vue | FAQ管理（替换占位） |
| src/main/resources/admin-frontend/src/views/prompt/PromptEdit.vue | 提示词管理（替换占位） |
| src/main/resources/admin-frontend/src/views/Dashboard.vue | 统计面板（替换占位） |

## 注意事项

- 所有列表页面使用统一的分页参数：page（从1开始）、size
- 表格空数据时使用 `el-empty` 组件
- 弹窗表单关闭时需要重置表单验证状态
- 删除操作使用 `ElMessageBox.confirm` 或 `el-popconfirm` 做二次确认
- 状态标签颜色映射：待确认(info)、已确认(primary)、已完成(success)、已取消(danger)
- 日期选择器使用 `el-date-picker` type="daterange"
- 对话详情使用 `el-timeline` + `el-timeline-item`
- 提示词编辑器使用 `el-input type="textarea"` 即可，不需要 Monaco Editor
- 统计面板暂时用 `el-table` 展示数据，不需要 ECharts 图表（简化实现）
