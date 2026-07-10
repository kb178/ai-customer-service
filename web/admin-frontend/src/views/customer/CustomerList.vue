<template>
  <div>
    <el-card style="margin-bottom: 16px;">
      <el-form :inline="true">
        <el-form-item label="姓名">
          <el-input v-model="searchName" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchPhone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="姓名" width="80" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" width="160" />
        <el-table-column prop="age" label="年龄" width="60" />
        <el-table-column prop="education" label="学历" width="80" />
        <el-table-column prop="interest" label="兴趣" width="120" />
        <el-table-column prop="source" label="来源" width="120" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top: 16px; justify-content: flex-end;" v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadData" />
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog title="客户详情" v-model="detailVisible" width="700px">
      <el-descriptions :column="2" border v-if="currentCustomer">
        <el-descriptions-item label="姓名">{{ currentCustomer.name }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ currentCustomer.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ currentCustomer.email }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ currentCustomer.age }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ currentCustomer.education }}</el-descriptions-item>
        <el-descriptions-item label="兴趣">{{ currentCustomer.interest }}</el-descriptions-item>
        <el-descriptions-item label="来源">{{ currentCustomer.source }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentCustomer.createTime }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin-top: 20px;">预约记录</h4>
      <el-table :data="customerReservations" stripe>
        <el-table-column prop="id" label="预约ID" width="70" />
        <el-table-column prop="courseId" label="课程ID" width="80" />
        <el-table-column prop="campusId" label="校区ID" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="['info','','success','danger'][row.status]">{{ ['待确认','已确认','已完成','已取消'][row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCustomerList, getCustomerReservations } from '../../api/customer'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const searchName = ref('')
const searchPhone = ref('')

const detailVisible = ref(false)
const currentCustomer = ref(null)
const customerReservations = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCustomerList({ page: page.value, size: size.value, name: searchName.value, phone: searchPhone.value })
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  currentCustomer.value = row
  detailVisible.value = true
  try {
    const res = await getCustomerReservations(row.id)
    customerReservations.value = res.data
  } catch (e) {
    customerReservations.value = []
  }
}

onMounted(loadData)
</script>
