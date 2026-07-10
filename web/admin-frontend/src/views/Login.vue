<template>
  <div class="login-container">
    <!-- 动态背景粒子 -->
    <div class="particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
    </div>

    <!-- 登录卡片 -->
    <div class="login-wrapper" :class="{ 'show': visible }">
      <!-- Logo 区域 -->
      <div class="logo-area">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z" />
            <path d="M8 14s1.5 2 4 2 4-2 4-2" />
            <path d="M9 9h.01M15 9h.01" stroke-linecap="round" />
          </svg>
        </div>
        <h1 class="title">AI 智能客服</h1>
        <p class="subtitle">管理后台</p>
      </div>

      <!-- 表单区域 -->
      <div class="form-area">
        <div class="input-group" :class="{ 'focus': usernameFocused, 'has-value': form.username }">
          <label class="input-label">用户名</label>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <input
              v-model="form.username"
              type="text"
              @focus="usernameFocused = true"
              @blur="usernameFocused = false"
              @keyup.enter="focusPassword"
              autocomplete="username"
            />
          </div>
        </div>

        <div class="input-group" :class="{ 'focus': passwordFocused, 'has-value': form.password }">
          <label class="input-label">密码</label>
          <div class="input-wrapper">
            <svg class="input-icon" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.5">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
            <input
              ref="passwordInput"
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              @focus="passwordFocused = true"
              @blur="passwordFocused = false"
              @keyup.enter="handleLogin"
              autocomplete="current-password"
            />
            <span class="toggle-password" @click="showPassword = !showPassword">
              <svg v-if="!showPassword" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
            </span>
          </div>
        </div>

        <!-- 错误提示 -->
        <div class="error-msg" :class="{ 'show': errorMsg }">{{ errorMsg }}</div>

        <!-- 登录按钮 -->
        <button
          class="login-btn"
          :class="{ 'loading': loading }"
          :disabled="loading"
          @click="handleLogin"
        >
          <span v-if="!loading" class="btn-text">登 录</span>
          <span v-else class="btn-loading">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </span>
        </button>

        <!-- 提示 -->
        <div class="hint">
          <span class="hint-dot"></span>
          默认账号：admin / 123456
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'

const router = useRouter()
const passwordInput = ref(null)
const visible = ref(false)
const loading = ref(false)
const showPassword = ref(false)
const usernameFocused = ref(false)
const passwordFocused = ref(false)
const errorMsg = ref('')

const form = reactive({ username: '', password: '' })

// 生成粒子样式
const particleStyle = (i) => {
  const size = Math.random() * 6 + 2
  const duration = Math.random() * 20 + 10
  const delay = Math.random() * 10
  const left = Math.random() * 100
  const opacity = Math.random() * 0.5 + 0.1
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    animationDuration: `${duration}s`,
    animationDelay: `${delay}s`,
    opacity
  }
}

const focusPassword = () => {
  passwordInput.value?.focus()
}

const handleLogin = async () => {
  errorMsg.value = ''

  if (!form.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!form.password.trim()) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    await login(form)
    sessionStorage.setItem('adminUser', form.username)
    // 成功动画
    visible.value = false
    setTimeout(() => router.push('/'), 300)
  } catch (e) {
    errorMsg.value = e?.message || '用户名或密码错误'
    // 抖动动画
    const wrapper = document.querySelector('.login-wrapper')
    wrapper?.classList.add('shake')
    setTimeout(() => wrapper?.classList.remove('shake'), 500)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  setTimeout(() => { visible.value = true }, 100)
})
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #0c1445 0%, #1a1a5e 30%, #2d1b69 60%, #1a0a3e 100%);
  overflow: hidden;
}

/* 粒子动画 */
.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.particle {
  position: absolute;
  bottom: -10px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 50%;
  animation: floatUp linear infinite;
}
@keyframes floatUp {
  0% { transform: translateY(0) rotate(0deg); opacity: 0; }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { transform: translateY(-100vh) rotate(720deg); opacity: 0; }
}

/* 登录卡片入场 */
.login-wrapper {
  position: relative;
  z-index: 1;
  width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.3);
  transform: translateY(30px);
  opacity: 0;
  transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1);
}
.login-wrapper.show {
  transform: translateY(0);
  opacity: 1;
}

/* 抖动动画 */
.login-wrapper.shake {
  animation: shake 0.5s ease-in-out;
}
@keyframes shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-12px); }
  40% { transform: translateX(10px); }
  60% { transform: translateX(-6px); }
  80% { transform: translateX(4px); }
}

/* Logo 区域 */
.logo-area {
  text-align: center;
  margin-bottom: 36px;
}
.logo-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  margin-bottom: 16px;
  animation: pulse 2s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(102, 126, 234, 0.4); }
  50% { box-shadow: 0 0 0 15px rgba(102, 126, 234, 0); }
}
.title {
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  margin: 0 0 6px;
  letter-spacing: 2px;
}
.subtitle {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  margin: 0;
  letter-spacing: 4px;
}

/* 输入框 */
.input-group {
  margin-bottom: 24px;
}
.input-label {
  display: block;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin-bottom: 8px;
  transition: color 0.3s;
  padding-left: 4px;
}
.input-group.focus .input-label,
.input-group.has-value .input-label {
  color: #667eea;
}
.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}
.input-icon {
  position: absolute;
  left: 14px;
  color: rgba(255, 255, 255, 0.3);
  transition: color 0.3s;
  pointer-events: none;
}
.input-group.focus .input-icon {
  color: #667eea;
}
.input-wrapper input {
  width: 100%;
  padding: 14px 14px 14px 44px;
  background: rgba(255, 255, 255, 0.06);
  border: 1.5px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: #fff;
  font-size: 15px;
  outline: none;
  transition: all 0.3s;
}
.input-wrapper input::placeholder {
  color: rgba(255, 255, 255, 0.2);
}
.input-group.focus .input-wrapper input {
  border-color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.15);
}
.toggle-password {
  position: absolute;
  right: 14px;
  color: rgba(255, 255, 255, 0.3);
  cursor: pointer;
  transition: color 0.3s;
  display: flex;
}
.toggle-password:hover {
  color: rgba(255, 255, 255, 0.7);
}

/* 错误提示 */
.error-msg {
  height: 0;
  overflow: hidden;
  color: #ff6b6b;
  font-size: 13px;
  text-align: center;
  transition: all 0.3s;
  margin-bottom: 0;
}
.error-msg.show {
  height: 24px;
  margin-bottom: 12px;
  line-height: 24px;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  height: 50px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s;
}
.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(102, 126, 234, 0.4);
}
.login-btn:active:not(:disabled) {
  transform: translateY(0);
}
.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}
.login-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transform: translateX(-100%);
  transition: transform 0.5s;
}
.login-btn:hover::before {
  transform: translateX(100%);
}

/* 加载动画 */
.btn-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.dot {
  width: 8px;
  height: 8px;
  background: #fff;
  border-radius: 50%;
  animation: bounce 1.4s ease-in-out infinite;
}
.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* 提示 */
.hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
}
.hint-dot {
  width: 6px;
  height: 6px;
  background: #667eea;
  border-radius: 50%;
}
</style>
