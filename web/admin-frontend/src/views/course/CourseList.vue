<template>
  <div class="page-container">
    <!-- 搜索栏 -->
    <el-card shadow="hover" class="search-card">
      <el-form :inline="true">
        <el-form-item label="课程名称">
          <el-input v-model="searchName" placeholder="请输入课程名称" clearable @keyup.enter="loadData" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="课程分类">
          <el-select v-model="searchCategoryId" placeholder="全部" clearable style="width: 150px;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
          <el-button type="success" @click="openDialog()">
            <el-icon><Plus /></el-icon>新增课程
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card shadow="hover" class="table-card">
      <el-table :data="list" v-loading="loading" stripe highlight-current-row>
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column prop="name" label="课程名称" min-width="140">
          <template #default="{ row }">
            <span class="link-text">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.category || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">
            <span class="price">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="课时" width="70" align="center">
          <template #default="{ row }">{{ row.duration }}h</template>
        </el-table-column>
        <el-table-column prop="targetAudience" label="目标人群" width="130" show-overflow-tooltip />
        <el-table-column label="学员" width="90" align="center">
          <template #default="{ row }">
            <span :class="{ 'full': row.currentStudents >= row.maxStudents }">
              {{ row.currentStudents || 0 }}/{{ row.maxStudents || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="dark" round>
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDialog(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-popconfirm title="确定删除该课程？" @confirm="handleDelete(row.id)" width="200">
              <template #reference>
                <el-button type="danger" link>
                  <el-icon><Delete /></el-icon>删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>

        <!-- 空状态 -->
        <template #empty>
          <el-empty description="暂无课程数据">
            <el-button type="primary" @click="openDialog()">新增课程</el-button>
          </el-empty>
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

    <!-- 弹窗表单 -->
    <el-dialog :title="isEdit ? '编辑课程' : '新增课程'" v-model="dialogVisible" width="600px" @closed="resetForm" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%;">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课时" prop="duration">
              <el-input-number v-model="form.duration" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="目标人群">
              <el-input v-model="form.targetAudience" placeholder="如：零基础学员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最大学员数">
              <el-input-number v-model="form.maxStudents" :min="1" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入课程描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { getCourseList, createCourse, updateCourse, deleteCourse, getCategories } from '../../api/course'

const list = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const size = ref(10)
const searchName = ref('')
const searchCategoryId = ref(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)
const formRef = ref()

const form = reactive({ name: '', categoryId: null, price: 0, duration: 1, targetAudience: '', maxStudents: 30, status: 1, description: '' })
const rules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (searchName.value) params.name = searchName.value
    if (searchCategoryId.value) params.categoryId = searchCategoryId.value
    const res = await getCourseList(params)
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data || []
  } catch (e) { /* ignore */ }
}

const handleReset = () => {
  searchName.value = ''
  searchCategoryId.value = null
  page.value = 1
  loadData()
}

const openDialog = (row) => {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    Object.assign(form, row)
  } else {
    isEdit.value = false
    editId.value = null
  }
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, { name: '', categoryId: null, price: 0, duration: 1, targetAudience: '', maxStudents: 30, status: 1, description: '' })
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCourse(editId.value, form)
      ElMessage.success('修改成功')
    } else {
      await createCourse(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (id) => {
  await deleteCourse(id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
  loadCategories()
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

.search-card {
  margin-bottom: 16px;
  border-radius: 12px;
}
.search-card :deep(.el-card__body) {
  padding-bottom: 2px;
}

.table-card {
  border-radius: 12px;
}

.link-text {
  color: #303133;
  font-weight: 500;
}

.price {
  color: #f56c6c;
  font-weight: 600;
}

.full {
  color: #f56c6c;
  font-weight: 600;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
