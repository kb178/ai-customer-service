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
