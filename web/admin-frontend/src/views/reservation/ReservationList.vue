<template>
  <div class="page-container">
    <el-card shadow="hover" class="search-card">
      <el-form :inline="true">
        <el-form-item label="手机号">
          <el-input v-model="searchPhone" placeholder="请输入手机号" clearable @keyup.enter="loadData" style="width: 180px;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchStatus" placeholder="全部" clearable style="width: 120px;">
            <el-option label="待确认" :value="0" />
            <el-option label="已确认" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 260px;" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card">
      <el-table :data="list" v-loading="loading" stripe highlight-current-row>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="customerName" label="客户姓名" width="100" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="courseId" label="课程ID" width="80" align="center" />
        <el-table-column prop="campusId" label="校区ID" width="80" align="center" />
        <el-table-column prop="appointmentTime" label="预约时间" width="170" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="dark" round>
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" link size="small" @click="handleConfirm(row.id)">
              <el-icon><Check /></el-icon>确认
            </el-button>
            <el-button v-if="row.status === 1" type="primary" link size="small" @click="handleComplete(row.id)">
              <el-icon><CircleCheck /></el-icon>完成
            </el-button>
            <el-button v-if="row.status < 2" type="danger" link size="small" @click="handleCancel(row.id)">
              <el-icon><Close /></el-icon>取消
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无预约数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check, CircleCheck, Close } from '@element-plus/icons-vue'
import { getReservationList, confirmReservation, cancelReservation, completeReservation } from '../../api/reservation'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const searchPhone = ref('')
const searchStatus = ref(null)
const dateRange = ref(null)

const statusText = (s) => ['待确认', '已确认', '已完成', '已取消'][s] || '未知'
const statusType = (s) => ['warning', '', 'success', 'info'][s] || 'info'

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchPhone.value) params.phone = searchPhone.value
    if (searchStatus.value !== null && searchStatus.value !== '') params.status = searchStatus.value
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const res = await getReservationList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchPhone.value = ''
  searchStatus.value = null
  dateRange.value = null
  page.value = 1
  loadData()
}

const handleConfirm = async (id) => {
  try {
    await ElMessageBox.confirm('确定确认该预约？', '确认操作', { type: 'warning' })
    await confirmReservation(id)
    ElMessage.success('已确认')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleComplete = async (id) => {
  try {
    await ElMessageBox.confirm('确定标记为已完成？', '确认操作', { type: 'info' })
    await completeReservation(id)
    ElMessage.success('已标记完成')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定取消该预约？此操作不可撤销。', '警告', { type: 'warning', confirmButtonText: '确定取消', cancelButtonText: '再想想' })
    await cancelReservation(id)
    ElMessage.success('已取消')
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

onMounted(loadData)
</script>

<style scoped>
.page-container {
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
.search-card {
  margin-bottom: 16px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 2px;
}
.table-card {
  border-radius: 12px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
