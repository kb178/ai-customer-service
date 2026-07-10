<template>
  <div>
    <el-row :gutter="20">
      <!-- 左侧：编辑区 -->
      <el-col :span="16">
        <el-card>
          <template #header><span>当前生效的提示词</span></template>
          <el-input v-model="content" type="textarea" :rows="20" placeholder="请输入提示词内容" />
          <div style="margin-top: 16px; text-align: right;">
            <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧：历史版本 -->
      <el-col :span="8">
        <el-card>
          <template #header><span>历史版本</span></template>
          <el-table :data="history" v-loading="historyLoading" stripe size="small">
            <el-table-column prop="version" label="版本" width="60" />
            <el-table-column prop="createTime" label="时间" />
            <el-table-column label="操作" width="70">
              <template #default="{ row }">
                <el-popconfirm title="确定回滚到此版本？" @confirm="handleRollback(row.id)">
                  <template #reference>
                    <el-button type="warning" link size="small">回滚</el-button>
                  </template>
                </el-popconfirm>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getActivePrompt, getPromptHistory, updatePrompt, rollbackPrompt } from '../../api/prompt'

const content = ref('')
const history = ref([])
const saving = ref(false)
const historyLoading = ref(false)

const loadData = async () => {
  historyLoading.value = true
  try {
    const [activeRes, historyRes] = await Promise.all([
      getActivePrompt(),
      getPromptHistory()
    ])
    content.value = activeRes.data?.content || ''
    history.value = historyRes.data || []
  } finally {
    historyLoading.value = false
  }
}

const handleSave = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('提示词内容不能为空')
    return
  }
  saving.value = true
  try {
    await updatePrompt({ content: content.value })
    ElMessage.success('保存成功')
    loadData()
  } finally {
    saving.value = false
  }
}

const handleRollback = async (id) => {
  await rollbackPrompt(id)
  ElMessage.success('回滚成功')
  loadData()
}

onMounted(loadData)
</script>
