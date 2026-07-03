# 任务4：前端项目初始化

## 目标

初始化 Vue 3 + Element Plus 前端项目，实现登录页、后台布局、路由配置，为后续 9 个管理页面搭建基础框架。

## 前置条件

- Node.js 18+ 已安装
- npm 可用
- 任务1已完成（登录接口可用）

## 任务清单

### 4.1 初始化项目

在项目根目录下执行：
```bash
npm create vite@latest admin-frontend -- --template vue
cd admin-frontend
npm install
npm install element-plus @element-plus/icons-vue vue-router axios echarts
```

### 4.2 配置 Vite

修改 `admin-frontend/vite.config.js`：

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8082',
        changeOrigin: true
      }
    }
  }
})
```

### 4.3 入口文件

**admin-frontend/index.html**：
```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>AI智能客服 - 管理后台</title>
</head>
<body>
  <div id="app"></div>
  <script type="module" src="/src/main.js"></script>
</body>
</html>
```

**admin-frontend/src/main.js**：
```javascript
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
```

**admin-frontend/src/App.vue**：
```vue
<template>
  <router-view />
</template>
```

### 4.4 路由配置

**admin-frontend/src/router/index.js**：

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue'),
        meta: { title: '仪表盘' }
      },
      {
        path: 'course',
        name: 'CourseList',
        component: () => import('../views/course/CourseList.vue'),
        meta: { title: '课程管理' }
      },
      {
        path: 'campus',
        name: 'CampusList',
        component: () => import('../views/campus/CampusList.vue'),
        meta: { title: '校区管理' }
      },
      {
        path: 'reservation',
        name: 'ReservationList',
        component: () => import('../views/reservation/ReservationList.vue'),
        meta: { title: '预约管理' }
      },
      {
        path: 'customer',
        name: 'CustomerList',
        component: () => import('../views/customer/CustomerList.vue'),
        meta: { title: '客户管理' }
      },
      {
        path: 'conversation',
        name: 'ConversationList',
        component: () => import('../views/conversation/ConversationList.vue'),
        meta: { title: '对话记录' }
      },
      {
        path: 'conversation/:sessionId',
        name: 'ConversationDetail',
        component: () => import('../views/conversation/ConversationDetail.vue'),
        meta: { title: '对话详情' }
      },
      {
        path: 'leave-message',
        name: 'LeaveMessageList',
        component: () => import('../views/leaveMessage/LeaveMessageList.vue'),
        meta: { title: '留言管理' }
      },
      {
        path: 'faq',
        name: 'FaqList',
        component: () => import('../views/faq/FaqList.vue'),
        meta: { title: 'FAQ管理' }
      },
      {
        path: 'prompt',
        name: 'PromptEdit',
        component: () => import('../views/prompt/PromptEdit.vue'),
        meta: { title: '提示词管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  const isLoggedIn = sessionStorage.getItem('adminUser')
  if (to.path !== '/login' && !isLoggedIn) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

### 4.5 Axios 封装

**admin-frontend/src/api/request.js**：

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 响应拦截器
request.interceptors.response.use(
  response => {
    const data = response.data
    if (data.code && data.code !== 200) {
      ElMessage.error(data.message || '请求失败')
      return Promise.reject(data)
    }
    return data
  },
  error => {
    if (error.response) {
      if (error.response.status === 401) {
        ElMessage.error('未登录，请先登录')
        sessionStorage.removeItem('adminUser')
        router.push('/login')
      } else {
        ElMessage.error(error.response.data?.message || '请求失败')
      }
    } else {
      ElMessage.error('网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
```

### 4.6 登录 API

**admin-frontend/src/api/auth.js**：

```javascript
import request from './request'

export function login(data) {
  return request.post('/auth/login', data)
}
```

### 4.7 登录页

**admin-frontend/src/views/Login.vue**：

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <template #header>
        <h2 style="text-align: center; margin: 0;">AI智能客服 - 管理后台</h2>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" size="large" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleLogin" style="width: 100%;" size="large">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await login(form)
    sessionStorage.setItem('adminUser', form.username)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
}
</style>
```

### 4.8 后台布局

**admin-frontend/src/layout/AdminLayout.vue**：

```vue
<template>
  <el-container style="height: 100vh;">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" style="background: #304156; transition: width 0.3s;">
      <div class="logo" :class="{ collapsed: isCollapse }">
        <span v-if="!isCollapse">AI客服后台</span>
        <span v-else>AI</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/course">
          <el-icon><Reading /></el-icon>
          <template #title>课程管理</template>
        </el-menu-item>
        <el-menu-item index="/campus">
          <el-icon><OfficeBuilding /></el-icon>
          <template #title>校区管理</template>
        </el-menu-item>
        <el-menu-item index="/reservation">
          <el-icon><Calendar /></el-icon>
          <template #title>预约管理</template>
        </el-menu-item>
        <el-menu-item index="/customer">
          <el-icon><User /></el-icon>
          <template #title>客户管理</template>
        </el-menu-item>
        <el-menu-item index="/conversation">
          <el-icon><ChatDotRound /></el-icon>
          <template #title>对话记录</template>
        </el-menu-item>
        <el-menu-item index="/leave-message">
          <el-icon><Message /></el-icon>
          <template #title>留言管理</template>
        </el-menu-item>
        <el-menu-item index="/faq">
          <el-icon><QuestionFilled /></el-icon>
          <template #title>FAQ管理</template>
        </el-menu-item>
        <el-menu-item index="/prompt">
          <el-icon><Edit /></el-icon>
          <template #title>提示词管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧内容 -->
    <el-container>
      <!-- 顶栏 -->
      <el-header style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee;">
        <el-icon style="cursor: pointer; font-size: 20px;" @click="isCollapse = !isCollapse">
          <Fold v-if="!isCollapse" />
          <Expand v-else />
        </el-icon>
        <div style="display: flex; align-items: center; gap: 12px;">
          <span>{{ username }}</span>
          <el-button type="danger" link @click="handleLogout">退出</el-button>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main style="background: #f5f7fa; padding: 20px;">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  DataAnalysis, Reading, OfficeBuilding, Calendar,
  User, ChatDotRound, Message, QuestionFilled, Edit,
  Fold, Expand
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)
const username = ref(sessionStorage.getItem('adminUser') || 'admin')

const activeMenu = computed(() => route.path)

const handleLogout = () => {
  sessionStorage.removeItem('adminUser')
  router.push('/login')
}
</script>

<style scoped>
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
}
.logo.collapsed {
  font-size: 14px;
}
.el-menu {
  border-right: none;
}
</style>
```

### 4.9 占位页面

为每个管理模块创建占位页面（后续任务会填充具体内容）：

**admin-frontend/src/views/Dashboard.vue**：
```vue
<template>
  <div>
    <h2>仪表盘</h2>
    <p>统计面板功能将在后续实现。</p>
  </div>
</template>
```

为以下目录创建同名占位文件，内容与 Dashboard.vue 类似：
- `views/course/CourseList.vue` — "课程管理功能将在后续实现。"
- `views/campus/CampusList.vue` — "校区管理功能将在后续实现。"
- `views/reservation/ReservationList.vue` — "预约管理功能将在后续实现。"
- `views/customer/CustomerList.vue` — "客户管理功能将在后续实现。"
- `views/conversation/ConversationList.vue` — "对话记录功能将在后续实现。"
- `views/conversation/ConversationDetail.vue` — "对话详情功能将在后续实现。"
- `views/leaveMessage/LeaveMessageList.vue` — "留言管理功能将在后续实现。"
- `views/faq/FaqList.vue` — "FAQ管理功能将在后续实现。"
- `views/prompt/PromptEdit.vue` — "提示词管理功能将在后续实现。"

## 新建文件清单

```
admin-frontend/
├── index.html
├── package.json
├── vite.config.js
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── router/index.js
│   ├── api/request.js
│   ├── api/auth.js
│   ├── layout/AdminLayout.vue
│   └── views/
│       ├── Login.vue
│       ├── Dashboard.vue
│       ├── course/CourseList.vue
│       ├── campus/CampusList.vue
│       ├── reservation/ReservationList.vue
│       ├── customer/CustomerList.vue
│       ├── conversation/ConversationList.vue
│       ├── conversation/ConversationDetail.vue
│       ├── leaveMessage/LeaveMessageList.vue
│       ├── faq/FaqList.vue
│       └── prompt/PromptEdit.vue
```

## 验证方式

1. `cd admin-frontend && npm run dev`
2. 浏览器访问 `http://localhost:5173`
3. 应自动跳转到登录页
4. 输入 admin/123456 登录
5. 登录成功后进入后台布局，左侧菜单可点击
6. 各菜单项点击后显示对应的占位页面
7. 点击退出按钮回到登录页

## 注意事项

- 后端需要先启动（端口 8082），前端 Vite proxy 才能转发请求
- 登录状态使用 `sessionStorage` 存储，关闭浏览器后需要重新登录
- 路由守卫在每次跳转时检查 `sessionStorage` 中是否有 `adminUser`
- Element Plus 图标需要全局注册才能在模板中使用
