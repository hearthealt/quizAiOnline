<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">活跃分析</span>
          <n-text depth="3">{{ currentDateLabel }} 活跃用户 {{ total }}</n-text>
        </div>
      </template>

      <n-space class="search-bar">
        <n-date-picker v-model:value="query.date" type="date" clearable style="width: 180px" />
        <n-input v-model:value="query.keyword" placeholder="搜索昵称/手机号" clearable style="width: 220px" @keyup.enter="fetchAll" />
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>

      <div class="overview-grid">
        <div class="overview-card">
          <span class="overview-label">活跃用户</span>
          <span class="overview-value">{{ overview.activeUsers || 0 }}</span>
        </div>
        <div class="overview-card">
          <span class="overview-label">登录用户</span>
          <span class="overview-value">{{ overview.loginUsers || 0 }}</span>
        </div>
        <div class="overview-card">
          <span class="overview-label">练习用户</span>
          <span class="overview-value">{{ overview.practiceUsers || 0 }}</span>
        </div>
        <div class="overview-card">
          <span class="overview-label">考试用户</span>
          <span class="overview-value">{{ overview.examUsers || 0 }}</span>
        </div>
        <div class="overview-card">
          <span class="overview-label">练习答题</span>
          <span class="overview-value">{{ overview.practiceAnswers || 0 }}</span>
        </div>
        <div class="overview-card">
          <span class="overview-label">考试答题</span>
          <span class="overview-value">{{ overview.examAnswers || 0 }}</span>
        </div>
      </div>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: any) => row.userId"
        size="small"
        striped
      />

      <div class="pagination-wrap">
        <n-pagination
          :page="query.pageNum"
          :page-size="query.pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <n-drawer v-model:show="showDrawer" :width="720">
      <n-drawer-content title="用户当日行为详情" closable>
        <template v-if="detailLoading">
          <div class="loading-wrap">
            <n-spin size="small" />
          </div>
        </template>

        <template v-else-if="detailData">
          <div class="detail-user">
            <n-avatar :src="resolveAssetUrl(detailData.user?.avatar)" :size="56" round />
            <div class="detail-user-copy">
              <div class="detail-user-name">{{ detailData.user?.nickname || '-' }}</div>
              <div class="detail-user-meta">{{ detailData.user?.phone || '未绑定手机号' }}</div>
            </div>
          </div>

          <div class="summary-grid">
            <div class="summary-item">
              <span class="summary-label">最后活跃</span>
              <span class="summary-value small">{{ formatTime(detailData.summary?.lastActiveTime) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">练习 / 答题</span>
              <span class="summary-value">{{ detailData.summary?.practiceRecordCount || 0 }} / {{ detailData.summary?.practiceAnswerCount || 0 }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">考试 / 答题</span>
              <span class="summary-value">{{ detailData.summary?.examRecordCount || 0 }} / {{ detailData.summary?.examAnswerCount || 0 }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">AI / 收藏</span>
              <span class="summary-value">{{ detailData.summary?.aiCallCount || 0 }} / {{ detailData.summary?.favoriteCount || 0 }}</span>
            </div>
          </div>

          <div class="section-header">
            <div class="section-title">行为流水</div>
            <n-text depth="3">共 {{ detailData.timeline?.total || 0 }} 条</n-text>
          </div>
          <n-spin :show="timelineLoading">
            <div v-if="detailData.timeline?.list?.length" class="timeline-panel">
              <div class="timeline-list">
                <div v-for="item in detailData.timeline.list" :key="item.id" class="timeline-item">
                  <div class="timeline-main">
                    <div class="timeline-title">{{ actionText(item.actionType) }}</div>
                    <div class="timeline-desc">{{ buildTimelineDesc(item) }}</div>
                  </div>
                  <div class="timeline-time">{{ formatTime(item.createTime) }}</div>
                </div>
              </div>
            </div>
            <n-empty v-else description="暂无行为日志" size="small" />
          </n-spin>
          <div v-if="(detailData.timeline?.total || 0) > 0" class="timeline-pagination">
            <n-pagination
              :page="timelineQuery.pageNum"
              :page-size="timelineQuery.pageSize"
              :item-count="detailData.timeline?.total || 0"
              show-size-picker
              :page-sizes="[10, 20, 50]"
              @update:page="handleTimelinePageChange"
              @update:page-size="handleTimelinePageSizeChange"
            />
          </div>

          <div class="section-title">当日答题</div>
          <n-data-table
            :columns="detailColumns"
            :data="detailData.answers?.list || []"
            :loading="answerLoading"
            size="small"
            :max-height="320"
          />
          <div v-if="(detailData.answers?.total || 0) > 0" class="timeline-pagination">
            <n-pagination
              :page="answerQuery.pageNum"
              :page-size="answerQuery.pageSize"
              :item-count="detailData.answers?.total || 0"
              show-size-picker
              :page-sizes="[10, 20, 50]"
              @update:page="handleAnswerPageChange"
              @update:page-size="handleAnswerPageSizeChange"
            />
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref } from 'vue'
import { NAvatar, NButton, NTag } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import dayjs from 'dayjs'
import { getActivityOverview, getActiveUsers, getActivityUserDetail } from '@/api/activity'
import { resolveAssetUrl } from '@/utils/assets'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const overview = ref<Record<string, any>>({})

const query = reactive({
  date: dayjs().valueOf() as number | null,
  keyword: '',
  pageNum: 1,
  pageSize: 10
})

const showDrawer = ref(false)
const detailLoading = ref(false)
const timelineLoading = ref(false)
const answerLoading = ref(false)
const detailData = ref<any>(null)
const currentDetailUserId = ref<number | null>(null)
const timelineQuery = reactive({
  pageNum: 1,
  pageSize: 10
})
const answerQuery = reactive({
  pageNum: 1,
  pageSize: 10
})

const currentDateParam = computed(() => dayjs(query.date || Date.now()).format('YYYY-MM-DD'))
const currentDateLabel = computed(() => dayjs(query.date || Date.now()).format('YYYY-MM-DD'))

const columns: DataTableColumns = [
  {
    title: '用户',
    key: 'user',
    width: 220,
    render(row: any) {
      return h('div', { style: 'display:flex;align-items:center;gap:10px' }, [
        h(NAvatar, { src: resolveAssetUrl(row.userAvatar), size: 38, round: true }),
        h('div', null, [
          h('div', { style: 'font-weight:600' }, row.userNickname || '-'),
          h('div', { style: 'font-size:12px;color:#94a3b8' }, row.userPhone || '未绑定手机号')
        ])
      ])
    }
  },
  {
    title: '最后活跃',
    key: 'lastActiveTime',
    width: 150,
    render(row: any) {
      return formatTime(row.lastActiveTime)
    }
  },
  {
    title: '练习',
    key: 'practice',
    width: 140,
    render(row: any) {
      return h('div', { style: 'display:flex;gap:6px;flex-wrap:wrap' }, [
        h(NTag, { size: 'small', bordered: false, type: 'info' }, () => `记录 ${row.practiceRecordCount || 0}`),
        h(NTag, { size: 'small', bordered: false, type: 'success' }, () => `答题 ${row.practiceAnswerCount || 0}`)
      ])
    }
  },
  {
    title: '考试',
    key: 'exam',
    width: 140,
    render(row: any) {
      return h('div', { style: 'display:flex;gap:6px;flex-wrap:wrap' }, [
        h(NTag, { size: 'small', bordered: false, type: 'warning' }, () => `记录 ${row.examRecordCount || 0}`),
        h(NTag, { size: 'small', bordered: false, type: 'error' }, () => `答题 ${row.examAnswerCount || 0}`)
      ])
    }
  },
  {
    title: 'AI / 收藏',
    key: 'misc',
    width: 110,
    render(row: any) {
      return `${row.aiCallCount || 0} / ${row.favoriteCount || 0}`
    }
  },
  {
    title: '活跃值',
    key: 'totalActionCount',
    width: 90
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render(row: any) {
      return h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => openDetail(row.userId) }, () => '查看详情')
    }
  }
]

const detailColumns: DataTableColumns = [
  {
    title: '时间',
    key: 'answerTime',
    width: 130,
    render(row: any) {
      return formatTime(row.answerTime)
    }
  },
  {
    title: '来源',
    key: 'source',
    width: 90,
    render(row: any) {
      return h(NTag, { size: 'small', bordered: false, type: row.source === 'PRACTICE' ? 'info' : 'warning' }, () => row.source === 'PRACTICE' ? '练习' : '考试')
    }
  },
  { title: '题库', key: 'bankName', width: 130, ellipsis: { tooltip: true } },
  { title: '题目', key: 'questionContent', ellipsis: { tooltip: true } },
  {
    title: '结果',
    key: 'isCorrect',
    width: 90,
    render(row: any) {
      if (row.isCorrect === null || row.isCorrect === undefined) {
        return h(NTag, { size: 'small', bordered: false, type: 'warning' }, () => '待判定')
      }
      return h(NTag, { size: 'small', bordered: false, type: row.isCorrect ? 'success' : 'error' }, () => row.isCorrect ? '正确' : '错误')
    }
  }
]

async function fetchOverview() {
  overview.value = await getActivityOverview(currentDateParam.value).catch(() => ({}))
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getActiveUsers({
      date: currentDateParam.value,
      keyword: query.keyword || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }) as any
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

async function fetchAll() {
  await Promise.all([fetchOverview(), fetchData()])
}

function handleSearch() {
  query.pageNum = 1
  fetchAll()
}

function handleReset() {
  query.date = dayjs().valueOf()
  query.keyword = ''
  query.pageNum = 1
  fetchAll()
}

function handlePageChange(page: number) {
  query.pageNum = page
  fetchData()
}

function handlePageSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  fetchData()
}

async function openDetail(userId: number) {
  currentDetailUserId.value = userId
  timelineQuery.pageNum = 1
  timelineQuery.pageSize = 10
  answerQuery.pageNum = 1
  answerQuery.pageSize = 10
  showDrawer.value = true
  detailData.value = null
  await fetchDetail('init')
}

async function fetchDetail(mode: 'init' | 'timeline' | 'answers' = 'init') {
  if (!currentDetailUserId.value) return
  if (mode === 'init' || !detailData.value) {
    detailLoading.value = true
  } else if (mode === 'timeline') {
    timelineLoading.value = true
  } else {
    answerLoading.value = true
  }
  try {
    detailData.value = await getActivityUserDetail(currentDetailUserId.value, {
      date: currentDateParam.value,
      timelinePageNum: timelineQuery.pageNum,
      timelinePageSize: timelineQuery.pageSize,
      answerPageNum: answerQuery.pageNum,
      answerPageSize: answerQuery.pageSize
    })
  } finally {
    detailLoading.value = false
    timelineLoading.value = false
    answerLoading.value = false
  }
}

function handleTimelinePageChange(page: number) {
  timelineQuery.pageNum = page
  void fetchDetail('timeline')
}

function handleTimelinePageSizeChange(size: number) {
  timelineQuery.pageSize = size
  timelineQuery.pageNum = 1
  void fetchDetail('timeline')
}

function handleAnswerPageChange(page: number) {
  answerQuery.pageNum = page
  void fetchDetail('answers')
}

function handleAnswerPageSizeChange(size: number) {
  answerQuery.pageSize = size
  answerQuery.pageNum = 1
  void fetchDetail('answers')
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('MM-DD HH:mm:ss') : '-'
}

function actionText(actionType?: string) {
  const map: Record<string, string> = {
    LOGIN: '登录',
    PRACTICE_START: '开始练习',
    PRACTICE_ANSWER: '练习答题',
    PRACTICE_FINISH: '完成练习',
    EXAM_START: '开始考试',
    EXAM_ANSWER: '考试答题',
    EXAM_SUBMIT: '提交考试',
    AI_CHAT: 'AI 提问',
    FAVORITE_ADD: '添加收藏'
  }
  return map[actionType || ''] || actionType || '未知行为'
}

function parseTimelineExt(item: any) {
  if (!item?.extJson || typeof item.extJson !== 'string') {
    return null
  }
  try {
    return JSON.parse(item.extJson)
  } catch {
    return null
  }
}

function buildTimelineDesc(item: any) {
  const parts: string[] = []
  const ext = parseTimelineExt(item)
  if (item.recordId) parts.push(`记录 ${item.recordId}`)
  if (item.questionId) parts.push(`题目 ${item.questionId}`)
  if (item.userAnswer) parts.push(`答案 ${item.userAnswer}`)
  if (item.isCorrect === 1) parts.push('结果 正确')
  if (item.isCorrect === 0) parts.push('结果 错误')
  if (item.actionType === 'AI_CHAT' && ext?.prompt) parts.push(`提问 ${ext.prompt}`)
  if (item.actionType === 'PRACTICE_START') {
    if (ext?.mode) parts.push(`模式 ${ext.mode}`)
    if (ext?.totalCount !== undefined) parts.push(`题数 ${ext.totalCount}`)
  }
  if (item.actionType === 'PRACTICE_FINISH') {
    if (ext?.answerCount !== undefined) parts.push(`已答 ${ext.answerCount}`)
    if (ext?.correctCount !== undefined) parts.push(`答对 ${ext.correctCount}`)
  }
  if (item.actionType === 'EXAM_START' && ext?.totalCount !== undefined) {
    parts.push(`题数 ${ext.totalCount}`)
  }
  if (item.actionType === 'EXAM_SUBMIT') {
    if (ext?.totalCount !== undefined) parts.push(`题数 ${ext.totalCount}`)
    if (ext?.correctCount !== undefined) parts.push(`答对 ${ext.correctCount}`)
    if (ext?.score !== undefined) parts.push(`得分 ${ext.score}`)
  }
  return parts.length ? parts.join(' · ') : '无附加信息'
}

void fetchAll()
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
  font-size: 15px;
  font-weight: 600;
}

.search-bar {
  margin-bottom: 12px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.overview-card {
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.overview-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.overview-value {
  display: block;
  margin-top: 6px;
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.loading-wrap {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}

.detail-user {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-user-name {
  font-size: 18px;
  font-weight: 600;
}

.detail-user-meta {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 13px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 20px;
}

.summary-item {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.summary-label {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.summary-value {
  display: block;
  margin-top: 6px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.summary-value.small {
  font-size: 14px;
}

.section-title {
  margin: 16px 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin: 16px 0 10px;
}

.section-header .section-title {
  margin: 0;
}

.timeline-panel {
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.timeline-title {
  font-size: 14px;
  font-weight: 600;
}

.timeline-desc,
.timeline-time {
  color: #64748b;
  font-size: 12px;
}

.timeline-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 1280px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .overview-grid,
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
