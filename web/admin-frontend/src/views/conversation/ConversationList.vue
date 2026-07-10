<template>
  <div>
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true">
        <el-form-item label="手机号">
          <el-input v-model="searchPhone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="会话ID">
          <el-input v-model="searchSessionId" placeholder="请输入会话ID" clearable />
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="sessionId" label="会话ID" />
        <el-table-column prop="customerPhone" label="手机号" width="130" />
        <el-table-column prop="messageCount" label="消息数量" width="90" />
        <el-table-column prop="firstTime" label="首次消息" width="170" />
        <el-table-column prop="lastTime" label="最后消息" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="$router.push(`/conversation/${row.sessionId}`)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 16px; justify-content: flex-end;" v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadData" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getConversationList } from '../../api/conversation'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const searchPhone = ref('')
const searchSessionId = ref('')
const dateRange = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value, phone: searchPhone.value, sessionId: searchSessionId.value }
    if (dateRange.value) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getConversationList(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
