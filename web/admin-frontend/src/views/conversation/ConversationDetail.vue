<template>
  <div class="page-container">
    <el-card shadow="hover" class="chat-card">
      <template #header>
        <div class="chat-header">
          <el-button @click="$router.back()" text>
            <el-icon><ArrowLeft /></el-icon>返回
          </el-button>
          <div class="session-info">
            <span class="session-label">会话ID</span>
            <el-tag size="small" effect="plain" class="session-id">{{ sessionId }}</el-tag>
          </div>
          <el-tag size="small" type="info">共 {{ logs.length }} 条消息</el-tag>
        </div>
      </template>

      <div ref="chatContainer" class="chat-container" v-loading="loading">
        <el-empty v-if="!loading && logs.length === 0" description="暂无对话记录" />

        <div v-for="(log, index) in logs" :key="index" :class="['message-row', log.role === 'user' ? 'is-user' : 'is-assistant']">
          <!-- 头像 -->
          <div :class="['avatar', log.role === 'user' ? 'avatar-user' : 'avatar-ai']">
            {{ log.role === 'user' ? '客' : 'AI' }}
          </div>

          <!-- 气泡 -->
          <div class="bubble-wrap">
            <div class="role-name">{{ log.role === 'user' ? '客户' : 'AI 助手' }}</div>
            <div :class="['bubble', log.role === 'user' ? 'bubble-user' : 'bubble-ai']">
              <div class="content">{{ log.content }}</div>
            </div>
            <div class="msg-time">{{ log.createTime }}</div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getSessionDetail } from '../../api/conversation'

const route = useRoute()
const sessionId = route.params.sessionId
const logs = ref([])
const loading = ref(false)
const chatContainer = ref(null)

const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getSessionDetail(sessionId)
    logs.value = res.data || []
    scrollToBottom()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-container {
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

.chat-card {
  border-radius: 12px;
}
.chat-card :deep(.el-card__body) {
  padding: 0 !important;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
}
.session-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}
.session-label {
  font-size: 13px;
  color: #909399;
}
.session-id {
  font-family: monospace;
}

/* 聊天区域 */
.chat-container {
  height: calc(100vh - 240px);
  min-height: 400px;
  overflow-y: auto;
  padding: 24px;
  background: #f8f9fb;
}

/* 消息行 */
.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  animation: msgIn 0.3s ease;
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.message-row.is-user {
  flex-direction: row;
}
.message-row.is-assistant {
  flex-direction: row-reverse;
}

/* 头像 */
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  flex-shrink: 0;
}
.avatar-user {
  background: linear-gradient(135deg, #f093fb, #f5576c);
}
.avatar-ai {
  background: linear-gradient(135deg, #667eea, #764ba2);
}

/* 气泡 */
.bubble-wrap {
  max-width: 70%;
  min-width: 80px;
}
.role-name {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
  padding: 0 4px;
}
.is-assistant .role-name {
  text-align: right;
}
.bubble {
  padding: 14px 18px;
  border-radius: 16px;
  position: relative;
  word-break: break-word;
  line-height: 1.7;
  font-size: 14px;
}
.bubble-user {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.bubble-ai {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-bottom-right-radius: 4px;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.2);
}
.content {
  white-space: pre-wrap;
}
.msg-time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 6px;
  padding: 0 4px;
}
.is-assistant .msg-time {
  text-align: right;
}
</style>
