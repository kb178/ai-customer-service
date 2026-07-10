<template>
  <div>
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="searchStatus" placeholder="全部" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已忽略" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="sessionId" label="会话ID" width="120" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户姓名" width="100" />
        <el-table-column prop="customerPhone" label="联系电话" width="130" />
        <el-table-column prop="message" label="留言内容" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="处理人" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 16px; justify-content: flex-end;" v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadData" />
    </el-card>

    <!-- 处理弹窗 -->
    <el-dialog title="处理留言" v-model="handleVisible" width="500px" @closed="resetHandleForm">
      <el-form :model="handleForm" label-width="80px">
        <el-form-item label="留言内容">
          <div>{{ currentMessage?.message }}</div>
        </el-form-item>
        <el-form-item label="处理人">
          <el-input v-model="handleForm.handler" />
        </el-form-item>
        <el-form-item label="处理备注">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="handleForm.status">
            <el-option label="处理中" :value="1" />
            <el-option label="已解决" :value="2" />
            <el-option label="已忽略" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitHandle">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLeaveMessageList, handleLeaveMessage } from '../../api/leaveMessage'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const searchStatus = ref(null)

const handleVisible = ref(false)
const currentMessage = ref(null)
const submitting = ref(false)
const handleForm = reactive({ handler: '', handleRemark: '', status: 1 })

const statusText = (s) => ['待处理', '处理中', '已解决', '已忽略'][s] || '未知'
const statusType = (s) => ['info', 'warning', 'success', 'info'][s] || 'info'

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLeaveMessageList({ page: page.value, size: size.value, status: searchStatus.value })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const openHandle = (row) => {
  currentMessage.value = row
  handleForm.handler = row.handler || ''
  handleForm.handleRemark = row.handleRemark || ''
  handleForm.status = row.status === 0 ? 1 : row.status
  handleVisible.value = true
}

const resetHandleForm = () => {
  Object.assign(handleForm, { handler: '', handleRemark: '', status: 1 })
}

const submitHandle = async () => {
  submitting.value = true
  try {
    await handleLeaveMessage(currentMessage.value.id, handleForm)
    ElMessage.success('处理成功')
    handleVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
</script>
