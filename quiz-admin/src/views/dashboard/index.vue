<template>
  <div class="dashboard-container" :class="{ 'is-admin': !isSuperAdmin }">
    <section class="dashboard-summary">
      <div class="summary-copy">
        <span class="role-label">{{ roleText }}</span>
        <h1>{{ isSuperAdmin ? '系统仪表盘' : '题库仪表盘' }}</h1>
        <p>{{ isSuperAdmin ? '用户、题库、会员和 AI 调用数据汇总。' : '题库维护、题目导入和 EZTest 直连数据汇总。' }}</p>
      </div>
      <div class="summary-count">
        <span>题目总数</span>
        <strong>{{ formatNumber(overview.totalQuestions) }}</strong>
      </div>
    </section>

    <section class="stat-grid">
      <div v-for="stat in statItems" :key="stat.label" class="stat-tile" :class="`tone-${stat.tone}`">
        <div class="stat-copy">
          <span>{{ stat.label }}</span>
          <strong>{{ stat.value }}</strong>
        </div>
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" :d="stat.icon" />
          </svg>
        </div>
      </div>
    </section>

    <div class="workspace-grid">
      <div class="chart-column">
        <section class="panel chart-panel">
          <div class="panel-head">
            <div>
              <h2>7日答题趋势</h2>
              <p>最近 7 天答题量变化</p>
            </div>
            <span class="panel-number">{{ formatNumber(trendTotal) }} 次</span>
          </div>
          <div ref="trendChartRef" class="chart-container"></div>
        </section>

        <section class="panel chart-panel">
          <div class="panel-head">
            <div>
              <h2>题库热度排行</h2>
              <p>按答题人次排序的题库</p>
            </div>
            <span class="panel-number">{{ topBankName }}</span>
          </div>
          <div ref="rankChartRef" class="chart-container"></div>
        </section>
      </div>

      <aside class="side-column">
        <section class="panel quick-panel">
          <div class="panel-head compact">
            <div>
              <h2>快捷入口</h2>
              <p>{{ isSuperAdmin ? '常用管理模块' : '题库管理模块' }}</p>
            </div>
          </div>
          <div class="action-list">
            <button
              v-for="action in actionItems"
              :key="action.path"
              type="button"
              class="action-row"
              :class="`tone-${action.tone}`"
              @click="go(action.path)"
            >
              <span class="action-icon">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path fill="currentColor" :d="action.icon" />
                </svg>
              </span>
              <span class="action-label">{{ action.label }}</span>
            </button>
          </div>
        </section>

        <section v-if="isSuperAdmin" class="panel ops-panel">
          <div class="panel-head compact">
            <div>
              <h2>运营摘要</h2>
              <p>订单、用户、AI 状态</p>
            </div>
          </div>

          <button type="button" class="ops-row tone-money" @click="go('/vip/order')">
            <span class="ops-label">待审核订单</span>
            <strong>{{ formatNumber(pendingOrderTotal) }}</strong>
            <small>{{ pendingOrders[0]?.planName || '暂无待处理订单' }}</small>
          </button>

          <button type="button" class="ops-row tone-primary" @click="go('/user')">
            <span class="ops-label">最新注册用户</span>
            <strong>{{ formatNumber(recentUserTotal) }}</strong>
            <small>{{ latestUserText }}</small>
          </button>

          <button type="button" class="ops-row tone-ai" @click="go('/ai/log')">
            <span class="ops-label">AI 今日调用</span>
            <strong>{{ formatNumber(aiStats.todayCalls) }}</strong>
            <small>成功率 {{ aiStats.successRate || 0 }}%</small>
          </button>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import { getOverview, getTrend, getRank, getVipStats, getAiStats, getPendingOrders, getRecentUsers } from '@/api/dashboard'
import { useAuthStore } from '@/stores/auth'

const icons = {
  user: 'M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4',
  question: 'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m-5 14H7v-2h7zm3-4H7v-2h10zm0-4H7V7h10z',
  active: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2m-2 15l-5-5l1.41-1.41L10 14.17l7.59-7.59L19 8z',
  answers: 'M9 16.17L4.83 12l-1.42 1.41L9 19L21 7l-1.41-1.41z',
  vip: 'M12 1L3 5v6c0 5.55 3.84 10.74 9 12c5.16-1.26 9-6.45 9-12V5zm0 6c1.4 0 2.8 1.1 2.8 2.5V11c.6 0 1.2.6 1.2 1.3v3.5c0 .6-.6 1.2-1.3 1.2H9.2c-.6 0-1.2-.6-1.2-1.3v-3.5c0-.6.6-1.2 1.2-1.2v-1.5C9.2 8.1 10.6 7 12 7',
  money: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10s10-4.48 10-10S17.52 2 12 2m1.41 16.09V20h-2.67v-1.93c-1.71-.36-3.16-1.46-3.27-3.4h1.96c.1 1.05.82 1.87 2.65 1.87c1.96 0 2.4-.98 2.4-1.59c0-.83-.44-1.61-2.67-2.14c-2.48-.6-4.18-1.62-4.18-3.67c0-1.72 1.39-2.84 3.11-3.21V4h2.67v1.95c1.86.45 2.79 1.86 2.85 3.39H14.3c-.05-1.11-.64-1.87-2.22-1.87c-1.5 0-2.4.68-2.4 1.64c0 .84.65 1.39 2.67 1.91s4.18 1.39 4.18 3.91c-.01 1.83-1.38 2.83-3.12 3.16',
  ai: 'M21 10.12h-6.78l2.74-2.82c-2.73-2.7-7.15-2.8-9.88-.1a6.875 6.875 0 0 0 0 9.79a7.02 7.02 0 0 0 9.88 0C18.32 15.65 19 14.08 19 12.1h2c0 1.98-.88 4.55-2.64 6.29c-3.51 3.48-9.21 3.48-12.72 0c-3.5-3.47-3.53-9.11-.02-12.58a8.987 8.987 0 0 1 12.65 0L21 3z',
  folder: 'M10 4l2 2h8c1.1 0 2 .9 2 2v10c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2l.01-12C2.01 4.9 2.9 4 4 4z',
  bank: 'M4 6H2v14c0 1.1.9 2 2 2h14v-2H4zm16-4H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2m0 14H8V4h12z',
  eztest: 'M5 20h14v-2H5m14-9h-4V3H9v6H5l7 7z',
  stats: 'M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2M9 17H7v-7h2zm4 0h-2V7h2zm4 0h-2v-4h2z',
} as const

const authStore = useAuthStore()
const router = useRouter()
const isSuperAdmin = computed(() => authStore.adminInfo?.role === 'super_admin')
const roleText = computed(() => isSuperAdmin.value ? '超级管理员' : '管理员')

const overview = reactive({ totalUsers: 0, totalQuestions: 0, todayActive: 0, todayAnswers: 0 })
const vipStats = reactive({ totalVipUsers: 0, totalRevenue: 0 })
const aiStats = reactive({ totalCalls: 0, todayCalls: 0, successCalls: 0, failCalls: 0, totalTokens: 0, successRate: '0' })
const pendingOrders = ref<any[]>([])
const pendingOrderTotal = ref(0)
const recentUsers = ref<any[]>([])
const recentUserTotal = ref(0)
const trendItems = ref<any[]>([])
const rankItems = ref<any[]>([])

const trendChartRef = ref<HTMLElement | null>(null)
const rankChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null
let rankChart: echarts.ECharts | null = null

const trendTotal = computed(() => trendItems.value.reduce((sum, item) => sum + Number(item.count || 0), 0))
const topBankName = computed(() => rankItems.value[0]?.name || '暂无数据')
const latestUserText = computed(() => {
  const user = recentUsers.value[0]
  if (!user) return '暂无新用户'
  const name = user.nickname || user.username || '新用户'
  return `${name} · ${formatTime(user.createTime)}`
})

const statItems = computed(() => {
  const base = [
    { label: '题目总数', value: formatNumber(overview.totalQuestions), icon: icons.question, tone: 'question' },
    { label: '今日答题', value: formatNumber(overview.todayAnswers), icon: icons.answers, tone: 'success' },
    { label: '今日活跃', value: formatNumber(overview.todayActive), icon: icons.active, tone: 'info' },
  ]
  if (!isSuperAdmin.value) {
    return [
      ...base,
      { label: '7日答题', value: formatNumber(trendTotal.value), icon: icons.stats, tone: 'warning' },
    ]
  }
  return [
    { label: '用户总数', value: formatNumber(overview.totalUsers), icon: icons.user, tone: 'primary' },
    ...base,
    { label: 'VIP 用户', value: formatNumber(vipStats.totalVipUsers), icon: icons.vip, tone: 'warning' },
    { label: '总收入', value: formatCurrency(vipStats.totalRevenue), icon: icons.money, tone: 'money' },
    { label: 'AI 调用', value: formatNumber(aiStats.totalCalls), icon: icons.ai, tone: 'ai' },
  ]
})

const actionItems = computed(() => {
  const core = [
    { label: '分类管理', path: '/category', icon: icons.folder, tone: 'info' },
    { label: '题库管理', path: '/bank', icon: icons.bank, tone: 'question' },
    { label: '题目管理', path: '/question', icon: icons.question, tone: 'success' },
    { label: 'EZTest直连', path: '/eztest/api', icon: icons.eztest, tone: 'warning' },
  ]
  if (!isSuperAdmin.value) return core
  return [
    { label: '题目管理', path: '/question', icon: icons.question, tone: 'success' },
    { label: '用户管理', path: '/user', icon: icons.user, tone: 'primary' },
    { label: 'VIP订单', path: '/vip/order', icon: icons.money, tone: 'money' },
    { label: '系统设置', path: '/system/setting', icon: icons.stats, tone: 'info' },
  ]
})

function formatNumber(value: unknown) {
  return Number(value || 0).toLocaleString('zh-CN')
}

function formatCurrency(value: unknown) {
  return `¥${Number(value || 0).toLocaleString('zh-CN')}`
}

function formatTime(time?: string) {
  return time ? dayjs(time).format('MM-DD HH:mm') : '-'
}

function go(path: string) {
  router.push(path)
}

async function loadDashboardData() {
  const [overviewData, trendData, rankData, vipData, aiData, ordersData, usersData] = await Promise.all([
    getOverview().catch(() => ({})),
    getTrend(7).catch(() => []),
    getRank(10).catch(() => []),
    isSuperAdmin.value ? getVipStats().catch(() => ({})) : Promise.resolve({}),
    isSuperAdmin.value ? getAiStats().catch(() => ({})) : Promise.resolve({}),
    isSuperAdmin.value ? getPendingOrders().catch(() => ({ list: [], total: 0 })) : Promise.resolve({ list: [], total: 0 }),
    isSuperAdmin.value ? getRecentUsers().catch(() => ({ list: [], total: 0 })) : Promise.resolve({ list: [], total: 0 }),
  ])

  Object.assign(overview, overviewData)
  Object.assign(vipStats, vipData)
  trendItems.value = Array.isArray(trendData) ? trendData : []
  rankItems.value = Array.isArray(rankData) ? rankData : []

  const ai = (aiData as any) || {}
  aiStats.totalCalls = ai.totalCalls || 0
  aiStats.todayCalls = ai.todayCalls || 0
  aiStats.successCalls = ai.successCalls || 0
  aiStats.failCalls = ai.failCalls || 0
  aiStats.totalTokens = ai.totalTokens || 0
  aiStats.successRate = ai.totalCalls > 0 ? ((ai.successCalls || 0) / ai.totalCalls * 100).toFixed(1) : '0'

  const orders = (ordersData as any) || {}
  pendingOrders.value = orders.list || []
  pendingOrderTotal.value = Number(orders.total ?? pendingOrders.value.length)

  const users = (usersData as any) || {}
  recentUsers.value = users.list || []
  recentUserTotal.value = Number(users.total ?? recentUsers.value.length)
}

function renderCharts() {
  if (trendChartRef.value) {
    trendChart?.dispose()
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 46, right: 18, bottom: 28, top: 24 },
      xAxis: {
        type: 'category',
        data: trendItems.value.map((item) => item.date),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280' },
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#6b7280' },
      },
      series: [{
        name: '答题数',
        type: 'line',
        smooth: true,
        symbolSize: 7,
        data: trendItems.value.map((item) => item.count),
        itemStyle: { color: '#b6402c' },
        lineStyle: { width: 3, color: '#b6402c' },
        areaStyle: { color: 'rgba(182, 64, 44, 0.1)' },
      }],
    })
  }

  if (rankChartRef.value) {
    rankChart?.dispose()
    rankChart = echarts.init(rankChartRef.value)
    const ranks = rankItems.value.slice(0, 8).reverse()
    rankChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 106, right: 18, bottom: 24, top: 16 },
      xAxis: {
        type: 'value',
        axisLine: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9' } },
        axisLabel: { color: '#6b7280' },
      },
      yAxis: {
        type: 'category',
        data: ranks.map((item) => item.name),
        axisLine: { lineStyle: { color: '#e5e7eb' } },
        axisLabel: { color: '#6b7280', width: 86, overflow: 'truncate' },
      },
      series: [{
        name: '答题人次',
        type: 'bar',
        data: ranks.map((item) => item.count),
        itemStyle: { borderRadius: [0, 4, 4, 0], color: '#3b82f6' },
      }],
    })
  }
}

function handleResize() {
  trendChart?.resize()
  rankChart?.resize()
}

onMounted(async () => {
  await loadDashboardData()
  await nextTick()
  renderCharts()
  window.addEventListener('resize', handleResize)
})

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
  gap: 14px;
}

.dashboard-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  padding: 18px 20px;
  border: 1px solid rgba(95, 68, 47, 0.1);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: var(--shadow-soft);
}

.summary-copy {
  min-width: 0;
}

.role-label {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  color: var(--color-primary);
  background: var(--color-primary-fade);
  font-size: 12px;
  font-weight: 700;
}

.summary-copy h1 {
  margin: 10px 0 4px;
  font-size: 24px;
  line-height: 1.2;
  color: var(--color-text);
}

.summary-copy p {
  margin: 0;
  color: var(--color-text-muted);
  font-size: 13px;
}

.summary-count {
  display: grid;
  gap: 4px;
  min-width: 140px;
  justify-items: end;
}

.summary-count span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.summary-count strong {
  color: var(--color-text);
  font-size: 28px;
  line-height: 1;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
}

.stat-tile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 82px;
  padding: 14px;
  border: 1px solid rgba(95, 68, 47, 0.1);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow-soft);
}

.stat-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.stat-copy span {
  color: var(--color-text-muted);
  font-size: 12px;
}

.stat-copy strong {
  color: var(--color-text);
  font-size: 22px;
  line-height: 1.1;
}

.stat-icon,
.action-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  color: var(--tone-color);
  background: var(--tone-bg);
}

.stat-icon svg,
.action-icon svg {
  width: 20px;
  height: 20px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 330px;
  gap: 14px;
  align-items: start;
}

.chart-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-width: 0;
}

.panel {
  padding: 16px;
  border: 1px solid rgba(95, 68, 47, 0.1);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow-soft);
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head.compact {
  margin-bottom: 10px;
}

.panel-head h2 {
  margin: 0;
  color: var(--color-text);
  font-size: 15px;
  line-height: 1.35;
}

.panel-head p {
  margin: 3px 0 0;
  color: var(--color-text-muted);
  font-size: 12px;
}

.panel-number {
  flex: 0 0 auto;
  color: var(--color-primary);
  font-size: 13px;
  font-weight: 700;
}

.chart-container {
  height: 260px;
}

.action-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.dashboard-container:not(.is-admin) .action-list {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.action-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  height: 46px;
  padding: 0 12px;
  border: 1px solid rgba(95, 68, 47, 0.08);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.8);
  color: var(--color-text);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.action-row:hover {
  transform: translateY(-1px);
  border-color: var(--tone-color);
  background: var(--tone-bg);
}

.action-icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: var(--tone-color);
  background: var(--tone-bg);
}

.action-label {
  font-size: 13px;
  font-weight: 700;
}

.ops-panel {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ops-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 4px 10px;
  width: 100%;
  padding: 12px;
  border: 1px solid rgba(95, 68, 47, 0.08);
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  text-align: left;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.ops-row:hover {
  transform: translateY(-1px);
  border-color: var(--tone-color);
  background: var(--tone-bg);
}

.ops-label {
  color: var(--color-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.ops-row strong {
  grid-row: span 2;
  align-self: center;
  color: var(--tone-color);
  font-size: 22px;
  line-height: 1;
}

.ops-row small {
  min-width: 0;
  color: var(--color-text-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tone-primary {
  --tone-color: #b6402c;
  --tone-bg: rgba(182, 64, 44, 0.08);
}

.tone-question {
  --tone-color: #2563eb;
  --tone-bg: rgba(37, 99, 235, 0.09);
}

.tone-info {
  --tone-color: #0891b2;
  --tone-bg: rgba(8, 145, 178, 0.09);
}

.tone-success {
  --tone-color: #16803c;
  --tone-bg: rgba(22, 128, 60, 0.09);
}

.tone-warning {
  --tone-color: #b45309;
  --tone-bg: rgba(180, 83, 9, 0.1);
}

.tone-money {
  --tone-color: #7c3aed;
  --tone-bg: rgba(124, 58, 237, 0.09);
}

.tone-ai {
  --tone-color: #be185d;
  --tone-bg: rgba(190, 24, 93, 0.09);
}

@media (max-width: 1200px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .side-column {
    display: flex;
  }

  .action-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .dashboard-summary {
    align-items: flex-start;
    flex-direction: column;
  }

  .summary-count {
    justify-items: start;
  }

  .side-column,
  .action-list {
    grid-template-columns: 1fr;
  }

  .chart-container {
    height: 230px;
  }
}
</style>
