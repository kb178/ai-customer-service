<template>
  <div class="dashboard">
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6" v-for="(card, i) in statCards" :key="i">
        <div class="stat-card" :style="{ background: card.bg }">
          <div class="stat-info">
            <div class="stat-label">{{ card.label }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
          <div class="stat-icon">
            <el-icon :size="32"><component :is="card.icon" /></el-icon>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" style="margin-bottom: 16px;">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">预约状态分布</span></template>
          <div ref="statusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">热门课程 TOP5</span></template>
          <div ref="courseChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">热门校区 TOP5</span></template>
          <div ref="campusChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header><span class="card-title">预约转化率</span></template>
          <div class="conversion-area">
            <div class="conversion-ring">
              <div class="ring-value">{{ conversion.conversionRate || 0 }}%</div>
              <div class="ring-label">转化率</div>
            </div>
            <div class="conversion-details">
              <div class="detail-item">
                <span class="detail-label">客户总数</span>
                <span class="detail-value">{{ conversion.totalCustomers || 0 }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">预约总数</span>
                <span class="detail-value">{{ conversion.totalReservations || 0 }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import * as echarts from 'echarts'
import { Calendar, User, DataLine, Trophy } from '@element-plus/icons-vue'
import { getOverview, getReservationStatus, getTopCourses, getTopCampuses, getConversion } from '../api/statistics'

const overview = reactive({})
const reservationStatus = ref([])
const topCourses = ref([])
const topCampuses = ref([])
const conversion = reactive({})

const statusChartRef = ref(null)
const courseChartRef = ref(null)
const campusChartRef = ref(null)

let statusChart = null
let courseChart = null
let campusChart = null

const statCards = computed(() => [
  { label: '今日预约', value: overview.todayReservations || 0, bg: 'linear-gradient(135deg, #667eea, #764ba2)', icon: Calendar },
  { label: '本周预约', value: overview.weekReservations || 0, bg: 'linear-gradient(135deg, #f093fb, #f5576c)', icon: DataLine },
  { label: '本月预约', value: overview.monthReservations || 0, bg: 'linear-gradient(135deg, #4facfe, #00f2fe)', icon: Trophy },
  { label: '客户总数', value: overview.totalCustomers || 0, bg: 'linear-gradient(135deg, #43e97b, #38f9d7)', icon: User }
])

const initStatusChart = (data) => {
  if (!statusChartRef.value) return
  statusChart = echarts.init(statusChartRef.value)
  const colorMap = { '待确认': '#e6a23c', '已确认': '#409eff', '已完成': '#67c23a', '已取消': '#909399' }
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#606266' } },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '40%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}条' },
      data: data.map(item => ({
        name: item.name,
        value: item.count,
        itemStyle: { color: colorMap[item.name] || '#409eff' }
      }))
    }]
  })
}

const initCourseChart = (data) => {
  if (!courseChartRef.value) return
  courseChart = echarts.init(courseChartRef.value)
  courseChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => `课程${d.courseId}`), axisLabel: { color: '#606266' } },
    yAxis: { type: 'value', axisLabel: { color: '#606266' }, splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    grid: { left: 50, right: 20, top: 20, bottom: 40 },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      barWidth: 30,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667eea' },
          { offset: 1, color: '#764ba2' }
        ])
      }
    }]
  })
}

const initCampusChart = (data) => {
  if (!campusChartRef.value) return
  campusChart = echarts.init(campusChartRef.value)
  campusChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', axisLabel: { color: '#606266' }, splitLine: { lineStyle: { type: 'dashed', color: '#eee' } } },
    yAxis: { type: 'category', data: data.map(d => `校区${d.campusId}`), axisLabel: { color: '#606266' } },
    grid: { left: 70, right: 30, top: 20, bottom: 20 },
    series: [{
      type: 'bar',
      data: data.map(d => d.count),
      barWidth: 24,
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#4facfe' },
          { offset: 1, color: '#00f2fe' }
        ])
      }
    }]
  })
}

const handleResize = () => {
  statusChart?.resize()
  courseChart?.resize()
  campusChart?.resize()
}

onMounted(async () => {
  try {
    const [o, s, tc, ca, cv] = await Promise.all([
      getOverview(), getReservationStatus(), getTopCourses(), getTopCampuses(), getConversion()
    ])
    Object.assign(overview, o.data || {})
    reservationStatus.value = s.data || []
    topCourses.value = tc.data || []
    topCampuses.value = ca.data || []
    Object.assign(conversion, cv.data || {})

    await nextTick()
    initStatusChart(reservationStatus.value)
    initCourseChart(topCourses.value)
    initCampusChart(topCampuses.value)
  } catch (e) {
    console.error('加载统计数据失败', e)
  }

  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  statusChart?.dispose()
  courseChart?.dispose()
  campusChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard {
  animation: fadeIn 0.4s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 统计卡片 */
.stat-cards {
  margin-bottom: 16px;
}
.stat-card {
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transition: transform 0.3s, box-shadow 0.3s;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15);
}
.stat-label {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 1px;
}
.stat-icon {
  opacity: 0.3;
}

/* 图表卡片 */
.chart-card {
  border-radius: 12px;
  overflow: hidden;
}
.chart-card :deep(.el-card__header) {
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}
.chart-container {
  height: 280px;
  padding: 10px;
}

/* 转化率区域 */
.conversion-area {
  height: 280px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
}
.conversion-ring {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  background: conic-gradient(#667eea 0% var(--rate, 0%), #f0f0f0 var(--rate, 0%) 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}
.conversion-ring::before {
  content: '';
  position: absolute;
  width: 120px;
  height: 120px;
  background: #fff;
  border-radius: 50%;
}
.ring-value {
  position: relative;
  z-index: 1;
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}
.ring-label {
  position: relative;
  z-index: 1;
  font-size: 12px;
  color: #909399;
}
.conversion-details {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-label {
  font-size: 13px;
  color: #909399;
}
.detail-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}
</style>
