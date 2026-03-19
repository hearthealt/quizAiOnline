<template>
  <div class="page-container">
    <n-card :bordered="false" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">数据统计</span>
          <n-date-picker v-model:value="dateRange" type="daterange" clearable @update:value="onDateChange" />
        </div>
      </template>

      <div class="chart-grid">
        <div class="chart-item">
          <div class="chart-title">用户增长趋势</div>
          <div ref="userGrowthRef" class="chart-container" />
        </div>
        <div class="chart-item">
          <div class="chart-title">答题趋势</div>
          <div ref="answerTrendRef" class="chart-container" />
        </div>
        <div class="chart-item">
          <div class="chart-title">题库热度排行</div>
          <div ref="bankHotRef" class="chart-container" />
        </div>
        <div class="chart-item">
          <div class="chart-title">正确率分布</div>
          <div ref="accuracyRef" class="chart-container" />
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { getUserGrowth, getAnswerTrend, getBankHot, getAccuracyDist } from '@/api/statistics'
import * as echarts from 'echarts'
import dayjs from 'dayjs'

const dateRange = ref<[number, number] | null>(null)

const userGrowthRef = ref<HTMLElement>()
const answerTrendRef = ref<HTMLElement>()
const bankHotRef = ref<HTMLElement>()
const accuracyRef = ref<HTMLElement>()

let userGrowthChart: echarts.ECharts
let answerTrendChart: echarts.ECharts
let bankHotChart: echarts.ECharts
let accuracyChart: echarts.ECharts

const chartTheme = {
  grid: { left: 50, right: 20, bottom: 30, top: 20 },
  axisLine: { lineStyle: { color: '#e0e0e0' } },
  axisLabel: { color: '#666' },
  splitLine: { lineStyle: { color: '#f0f0f0' } },
}

function getDateParams() {
  if (!dateRange.value) return {}
  return {
    startDate: dayjs(dateRange.value[0]).format('YYYY-MM-DD'),
    endDate: dayjs(dateRange.value[1]).format('YYYY-MM-DD')
  }
}

async function loadUserGrowth() {
  try {
    const list = (await getUserGrowth(getDateParams()) as any) || []
    userGrowthChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: list.map((i: any) => i.date), axisLine: chartTheme.axisLine, axisLabel: chartTheme.axisLabel },
      yAxis: { type: 'value', axisLine: { show: false }, splitLine: chartTheme.splitLine, axisLabel: chartTheme.axisLabel },
      series: [{ name: '新增用户', type: 'line', data: list.map((i: any) => i.count), smooth: true, areaStyle: { opacity: 0.2, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#fff' }]) }, itemStyle: { color: '#667eea' }, lineStyle: { width: 3 } }],
      grid: chartTheme.grid,
    })
  } catch {}
}

async function loadAnswerTrend() {
  try {
    const list = (await getAnswerTrend(getDateParams()) as any) || []
    answerTrendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: list.map((i: any) => i.date), axisLine: chartTheme.axisLine, axisLabel: chartTheme.axisLabel },
      yAxis: { type: 'value', axisLine: { show: false }, splitLine: chartTheme.splitLine, axisLabel: chartTheme.axisLabel },
      series: [{ name: '答题数', type: 'line', data: list.map((i: any) => i.count), smooth: true, areaStyle: { opacity: 0.2, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#43e97b' }, { offset: 1, color: '#fff' }]) }, itemStyle: { color: '#43e97b' }, lineStyle: { width: 3 } }],
      grid: chartTheme.grid,
    })
  } catch {}
}

async function loadBankHot() {
  try {
    const list = ((await getBankHot(10) as any) || []).reverse()
    bankHotChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value', axisLine: { show: false }, splitLine: chartTheme.splitLine, axisLabel: chartTheme.axisLabel },
      yAxis: { type: 'category', data: list.map((i: any) => i.name), axisLine: chartTheme.axisLine, axisLabel: chartTheme.axisLabel },
      series: [{ name: '热度', type: 'bar', data: list.map((i: any) => i.count), itemStyle: { borderRadius: [0, 4, 4, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]) } }],
      grid: { left: 120, right: 20, bottom: 20, top: 10 },
    })
  } catch {}
}

async function loadAccuracy() {
  try {
    const list = (await getAccuracyDist() as any) || []
    const colors = ['#667eea', '#43e97b', '#f093fb', '#4facfe', '#fa709a']
    accuracyChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { color: '#666' } },
      series: [{
        name: '正确率分布',
        type: 'pie',
        radius: ['40%', '70%'],
        data: list.map((i: any, idx: number) => ({ name: i.name, value: i.count, itemStyle: { color: colors[idx % colors.length] } })),
        label: { formatter: '{b}: {d}%', color: '#666' },
      }]
    })
  } catch {}
}

function initCharts() {
  userGrowthChart = echarts.init(userGrowthRef.value!)
  answerTrendChart = echarts.init(answerTrendRef.value!)
  bankHotChart = echarts.init(bankHotRef.value!)
  accuracyChart = echarts.init(accuracyRef.value!)
}

function loadAll() {
  loadUserGrowth()
  loadAnswerTrend()
  loadBankHot()
  loadAccuracy()
}

function onDateChange() {
  loadUserGrowth()
  loadAnswerTrend()
}

function handleResize() {
  userGrowthChart?.resize()
  answerTrendChart?.resize()
  bankHotChart?.resize()
  accuracyChart?.resize()
}

onMounted(() => {
  initCharts()
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  userGrowthChart?.dispose()
  answerTrendChart?.dispose()
  bankHotChart?.dispose()
  accuracyChart?.dispose()
})
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.main-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.chart-item {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px;
}

.chart-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #667eea;
}

.chart-container {
  height: 300px;
}

@media (max-width: 1200px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
