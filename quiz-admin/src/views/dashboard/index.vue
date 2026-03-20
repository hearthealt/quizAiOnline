<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card" style="--card-color: #667eea">
        <div class="stat-icon">
          <n-icon size="28">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4"/></svg>
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalUsers }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>
      
      <div class="stat-card" style="--card-color: #f093fb">
        <div class="stat-icon">
          <n-icon size="28">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m-5 14H7v-2h7zm3-4H7v-2h10zm0-4H7V7h10z"/></svg>
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.totalQuestions }}</div>
          <div class="stat-label">题目总数</div>
        </div>
      </div>
      
      <div class="stat-card" style="--card-color: #4facfe">
        <div class="stat-icon">
          <n-icon size="28">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2m-2 15l-5-5l1.41-1.41L10 14.17l7.59-7.59L19 8z"/></svg>
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.todayActive }}</div>
          <div class="stat-label">今日活跃</div>
        </div>
      </div>
      
      <div class="stat-card" style="--card-color: #43e97b">
        <div class="stat-icon">
          <n-icon size="28">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19L21 7l-1.41-1.41z"/></svg>
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview.todayAnswers }}</div>
          <div class="stat-label">今日答题</div>
        </div>
      </div>

      <div class="stat-card" style="--card-color: #fa709a">
        <div class="stat-icon">
          <n-icon size="28">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12c5.16-1.26 9-6.45 9-12V5zm0 6c1.4 0 2.8 1.1 2.8 2.5V11c.6 0 1.2.6 1.2 1.3v3.5c0 .6-.6 1.2-1.3 1.2H9.2c-.6 0-1.2-.6-1.2-1.3v-3.5c0-.6.6-1.2 1.2-1.2v-1.5C9.2 8.1 10.6 7 12 7m0 1.2c-.8 0-1.5.5-1.5 1.3V11h3v-1.5c0-.8-.7-1.3-1.5-1.3"/></svg>
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ vipStats.totalVipUsers || 0 }}</div>
          <div class="stat-label">VIP用户</div>
        </div>
      </div>

      <div class="stat-card" style="--card-color: #ffecd2">
        <div class="stat-icon" style="color: #f5af19">
          <n-icon size="28">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2m1.41 16.09V20h-2.67v-1.93c-1.71-.36-3.16-1.46-3.27-3.4h1.96c.1 1.05.82 1.87 2.65 1.87c1.96 0 2.4-.98 2.4-1.59c0-.83-.44-1.61-2.67-2.14c-2.48-.6-4.18-1.62-4.18-3.67c0-1.72 1.39-2.84 3.11-3.21V4h2.67v1.95c1.86.45 2.79 1.86 2.85 3.39H14.3c-.05-1.11-.64-1.87-2.22-1.87c-1.5 0-2.4.68-2.4 1.64c0 .84.65 1.39 2.67 1.91s4.18 1.39 4.18 3.91c-.01 1.83-1.38 2.83-3.12 3.16"/></svg>
          </n-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">¥{{ vipStats.totalRevenue || 0 }}</div>
          <div class="stat-label">总收入</div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <n-card :bordered="false" size="small" class="quick-actions">
      <template #header>
        <span class="section-title">快捷入口</span>
      </template>
      <div class="action-grid">
        <div class="action-item" @click="$router.push('/question')">
          <div class="action-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
            <n-icon size="24"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon>
          </div>
          <span>新增题目</span>
        </div>
        <div class="action-item" @click="$router.push('/bank')">
          <div class="action-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
            <n-icon size="24"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M4 6H2v14c0 1.1.9 2 2 2h14v-2H4zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2m0 14H8V4h12z"/></svg></n-icon>
          </div>
          <span>题库管理</span>
        </div>
        <div class="action-item" @click="$router.push('/user')">
          <div class="action-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
            <n-icon size="24"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3s1.34 3 3 3m-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5S5 6.34 5 8s1.34 3 3 3m0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5m8 0c-.29 0-.62.02-.97.05c1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5"/></svg></n-icon>
          </div>
          <span>用户管理</span>
        </div>
        <div class="action-item" @click="$router.push('/vip/order')">
          <div class="action-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%)">
            <n-icon size="24"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m0 16H5V5h14zM17.99 9l-1.41-1.42l-6.59 6.59l-2.58-2.57l-1.42 1.41l4 3.99z"/></svg></n-icon>
          </div>
          <span>VIP审核</span>
        </div>
        <div class="action-item" @click="$router.push('/ai/config')">
          <div class="action-icon" style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)">
            <n-icon size="24"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M21 10.12h-6.78l2.74-2.82c-2.73-2.7-7.15-2.8-9.88-.1a6.875 6.875 0 0 0 0 9.79a7.02 7.02 0 0 0 9.88 0C18.32 15.65 19 14.08 19 12.1h2c0 1.98-.88 4.55-2.64 6.29c-3.51 3.48-9.21 3.48-12.72 0c-3.5-3.47-3.53-9.11-.02-12.58a8.987 8.987 0 0 1 12.65 0L21 3z"/></svg></n-icon>
          </div>
          <span>AI配置</span>
        </div>
        <div class="action-item" @click="$router.push('/statistics')">
          <div class="action-icon" style="background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)">
            <n-icon size="24"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2M9 17H7v-7h2zm4 0h-2V7h2zm4 0h-2v-4h2z"/></svg></n-icon>
          </div>
          <span>数据统计</span>
        </div>
      </div>
    </n-card>

    <!-- 主内容区 -->
    <div class="main-content">
      <!-- 左侧图表 -->
      <div class="chart-area">
        <n-card :bordered="false" size="small" class="chart-card">
          <template #header>
            <span class="section-title">7日答题趋势</span>
          </template>
          <div ref="trendChartRef" class="chart-container"></div>
        </n-card>
        
        <n-card :bordered="false" size="small" class="chart-card">
          <template #header>
            <span class="section-title">题库热度排行</span>
          </template>
          <div ref="rankChartRef" class="chart-container"></div>
        </n-card>
      </div>

      <!-- 右侧信息 -->
      <div class="info-area">
        <!-- 待审核订单 -->
        <n-card :bordered="false" size="small" class="info-card">
          <template #header>
            <div class="card-header-flex">
              <span class="section-title">待审核订单</span>
              <n-button text type="primary" size="small" @click="$router.push('/vip/order')">查看全部</n-button>
            </div>
          </template>
          <div v-if="pendingOrders.length === 0" class="empty-tip">暂无待审核订单</div>
          <div v-else class="order-list">
            <div v-for="order in pendingOrders" :key="order.id" class="order-item">
              <div class="order-info">
                <span class="order-name">{{ order.userNickname }}</span>
                <span class="order-plan">{{ order.planName }}</span>
              </div>
              <div class="order-amount">¥{{ order.amount }}</div>
            </div>
          </div>
        </n-card>

        <!-- 最新注册 -->
        <n-card :bordered="false" size="small" class="info-card">
          <template #header>
            <div class="card-header-flex">
              <span class="section-title">最新注册用户</span>
              <n-button text type="primary" size="small" @click="$router.push('/user')">查看全部</n-button>
            </div>
          </template>
          <div v-if="recentUsers.length === 0" class="empty-tip">暂无用户</div>
          <div v-else class="user-list">
            <div v-for="user in recentUsers" :key="user.id" class="user-item">
              <n-avatar :src="user.avatar" :size="36" round />
              <div class="user-info">
                <div class="user-name">{{ user.nickname }}</div>
                <div class="user-time">{{ formatTime(user.createTime) }}</div>
              </div>
              <n-tag v-if="user.isVip === 1" type="warning" size="small" round>VIP</n-tag>
            </div>
          </div>
        </n-card>

        <!-- AI调用统计 -->
        <n-card :bordered="false" size="small" class="info-card">
          <template #header>
            <div class="card-header-flex">
              <span class="section-title">AI调用统计</span>
              <n-button text type="primary" size="small" @click="$router.push('/ai/log')">查看日志</n-button>
            </div>
          </template>
          <div class="ai-stats">
            <div class="ai-stat-item">
              <div class="ai-stat-value">{{ aiStats.totalCalls || 0 }}</div>
              <div class="ai-stat-label">总调用</div>
            </div>
            <div class="ai-stat-item">
              <div class="ai-stat-value">{{ aiStats.todayCalls || 0 }}</div>
              <div class="ai-stat-label">今日调用</div>
            </div>
            <div class="ai-stat-item">
              <div class="ai-stat-value">{{ aiStats.successRate || 0 }}%</div>
              <div class="ai-stat-label">成功率</div>
            </div>
          </div>
        </n-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getOverview, getTrend, getRank, getVipStats, getAiStats, getPendingOrders, getRecentUsers } from '@/api/dashboard'
import dayjs from 'dayjs'

const overview = reactive({ totalUsers: 0, totalQuestions: 0, todayActive: 0, todayAnswers: 0 })
const vipStats = reactive({ totalVipUsers: 0, totalRevenue: 0 })
const aiStats = reactive({ totalCalls: 0, todayCalls: 0, successRate: '0' })
const pendingOrders = ref<any[]>([])
const recentUsers = ref<any[]>([])

const trendChartRef = ref<HTMLElement | null>(null)
const rankChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let rankChart: echarts.ECharts | null = null

function formatTime(time: string) {
  return dayjs(time).format('MM-DD HH:mm')
}

onMounted(async () => {
  // 并行加载数据
  const [overviewData, trendData, rankData, vipData, aiData, ordersData, usersData] = await Promise.all([
    getOverview().catch(() => ({})),
    getTrend(7).catch(() => []),
    getRank(10).catch(() => []),
    getVipStats().catch(() => ({})),
    getAiStats().catch(() => ({})),
    getPendingOrders().catch(() => ({ list: [] })),
    getRecentUsers().catch(() => ({ list: [] })),
  ])

  Object.assign(overview, overviewData)
  Object.assign(vipStats, vipData)
  
  // AI 统计计算成功率
  const ai = aiData as any || {}
  aiStats.totalCalls = ai.totalCalls || 0
  aiStats.todayCalls = ai.todayCalls || 0
  aiStats.successRate = ai.totalCalls > 0 ? ((ai.successCalls || 0) / ai.totalCalls * 100).toFixed(1) : '0'
  pendingOrders.value = (ordersData as any)?.list || []
  recentUsers.value = (usersData as any)?.list || []

  // 初始化图表
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    const trends = (trendData as any) || []
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trends.map((t: any) => t.date), axisLine: { lineStyle: { color: '#e0e0e0' } }, axisLabel: { color: '#666' } },
      yAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f0f0f0' } }, axisLabel: { color: '#666' } },
      series: [{
        name: '答题数',
        type: 'line',
        smooth: true,
        data: trends.map((t: any) => t.count),
        areaStyle: { opacity: 0.2, color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#fff' }]) },
        itemStyle: { color: '#667eea' },
        lineStyle: { width: 3 },
      }],
      grid: { left: 50, right: 20, bottom: 30, top: 20 },
    })
  }

  if (rankChartRef.value) {
    rankChart = echarts.init(rankChartRef.value)
    const ranks = ((rankData as any) || []).slice(0, 8).reverse()
    rankChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value', axisLine: { show: false }, splitLine: { lineStyle: { color: '#f0f0f0' } }, axisLabel: { color: '#666' } },
      yAxis: { type: 'category', data: ranks.map((r: any) => r.name), axisLine: { lineStyle: { color: '#e0e0e0' } }, axisLabel: { color: '#666', width: 80, overflow: 'truncate' } },
      series: [{
        name: '答题人次',
        type: 'bar',
        data: ranks.map((r: any) => r.count),
        itemStyle: { borderRadius: [0, 4, 4, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#667eea' }, { offset: 1, color: '#764ba2' }]) },
      }],
      grid: { left: 100, right: 20, bottom: 20, top: 10 },
    })
  }

  window.addEventListener('resize', handleResize)
})

function handleResize() {
  trendChart?.resize()
  rankChart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  rankChart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: var(--card-color);
  color: #fff;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 2px;
}

.quick-actions {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.action-item:hover {
  background: #f5f5f5;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  color: #fff;
}

.action-item span {
  font-size: 13px;
  color: #666;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 12px;
}

.chart-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chart-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.chart-container {
  height: 220px;
}

.info-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.card-header-flex {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 20px 0;
  font-size: 13px;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.order-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.order-name {
  font-size: 14px;
  font-weight: 500;
}

.order-plan {
  font-size: 12px;
  color: #999;
}

.order-amount {
  font-size: 16px;
  font-weight: 600;
  color: #667eea;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
}

.user-time {
  font-size: 12px;
  color: #999;
}

.ai-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  text-align: center;
}

.ai-stat-item {
  padding: 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.ai-stat-value {
  font-size: 20px;
  font-weight: 600;
  color: #667eea;
}

.ai-stat-label {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

@media (max-width: 1400px) {
  .stat-cards {
    grid-template-columns: repeat(3, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 1fr;
  }
  .info-area {
    flex-direction: row;
  }
  .info-card {
    flex: 1;
  }
}
</style>
