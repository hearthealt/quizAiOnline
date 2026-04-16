<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">练习记录</span>
          <n-text depth="3">共 {{ total }} 条记录</n-text>
        </div>
      </template>

      <n-space class="search-bar">
        <n-input v-model:value="query.keyword" placeholder="用户昵称/手机号" clearable style="width: 180px" @keyup.enter="fetchData">
          <template #prefix><n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14"/></svg></n-icon></template>
        </n-input>
        <n-select v-model:value="query.bankId" placeholder="选择题库" clearable :options="bankOptions" style="width: 180px" />
        <n-button type="primary" @click="fetchData">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>

      <div class="answer-legend">
        <span class="legend-dot is-correct"></span><span>答对</span>
        <span class="legend-dot is-wrong"></span><span>答错</span>
        <span class="legend-dot is-unanswered"></span><span>未答</span>
        <span class="legend-dot is-total"></span><span>总题数</span>
      </div>

      <n-data-table :columns="columns" :data="tableData" :loading="loading" :row-key="(row: any) => row.id" striped size="small" />

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

    <!-- 详情弹窗 -->
    <n-modal v-model:show="showDetail" preset="card" title="答题详情" style="width: min(960px, calc(100vw - 32px))">
      <div class="detail-shell">
        <n-space class="detail-tools">
          <n-input
            v-model:value="detailQuery.keyword"
            placeholder="搜索题目内容或题目ID"
            clearable
            style="width: 260px"
            @keyup.enter="handleDetailSearch"
          />
          <n-select
            v-model:value="detailQuery.result"
            :options="detailResultOptions"
            placeholder="结果筛选"
            clearable
            style="width: 160px"
          />
          <n-button type="primary" @click="handleDetailSearch">搜索</n-button>
          <n-button @click="handleDetailReset">重置</n-button>
        </n-space>

        <div class="detail-summary">
          <div class="summary-item is-correct">
            <span class="summary-label">答对</span>
            <span class="summary-value">{{ detailSummary.correctCount }}</span>
          </div>
          <div class="summary-item is-wrong">
            <span class="summary-label">答错</span>
            <span class="summary-value">{{ detailSummary.wrongCount }}</span>
          </div>
          <div class="summary-item is-unanswered">
            <span class="summary-label">未答</span>
            <span class="summary-value">{{ detailSummary.unansweredCount }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">总题数</span>
            <span class="summary-value">{{ detailSummary.totalCount }}</span>
          </div>
        </div>

        <n-data-table :columns="detailColumns" :data="detailData" :loading="detailLoading" size="small" :max-height="420" />

        <div class="detail-pagination">
          <n-pagination
            :page="detailQuery.pageNum"
            :page-size="detailQuery.pageSize"
            :item-count="detailTotal"
            show-size-picker
            :page-sizes="[10, 20, 50]"
            @update:page="handleDetailPageChange"
            @update:page-size="handleDetailPageSizeChange"
          />
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NButton, NTag } from 'naive-ui'
import { getPracticeList, getPracticeDetail } from '@/api/record'
import { getList as getBankList } from '@/api/bank'
import dayjs from 'dayjs'
import type { DataTableColumns } from 'naive-ui'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const bankOptions = ref<{ label: string; value: number }[]>([])

const query = reactive({
  keyword: '',
  bankId: null as number | null,
  pageNum: 1,
  pageSize: 10
})

const showDetail = ref(false)
const currentDetailId = ref<number | null>(null)
const detailData = ref<any[]>([])
const detailLoading = ref(false)
const detailTotal = ref(0)
const detailSummary = ref({
  totalCount: 0,
  answerCount: 0,
  correctCount: 0,
  wrongCount: 0,
  unansweredCount: 0,
})
const detailQuery = reactive({
  keyword: '',
  result: null as string | null,
  pageNum: 1,
  pageSize: 10,
})
const detailResultOptions = [
  { label: '答对', value: 'CORRECT' },
  { label: '答错', value: 'WRONG' },
  { label: '未答', value: 'UNANSWERED' },
]

const columns: DataTableColumns = [
  {
    title: '用户',
    key: 'user',
    width: 150,
    render(row: any) {
      return h('div', null, [
        h('div', { style: 'font-weight:500' }, row.userNickname),
        h('div', { style: 'font-size:12px;color:#999' }, row.userPhone),
      ])
    }
  },
  { title: '题库', key: 'bankName', ellipsis: { tooltip: true } },
  { title: '模式', key: 'mode', width: 100, render(row: any) { return h(NTag, { size: 'small', bordered: false, type: 'info' }, () => row.mode) } },
  {
    title: '答题情况',
    key: 'progress',
    width: 320,
    render(row: any) {
      return renderAnswerStats(row)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render(row: any) {
      const map: Record<number, { text: string; type: 'success' | 'warning' | 'info' }> = {
        0: { text: '进行中', type: 'warning' },
        1: { text: '已完成', type: 'success' },
        2: { text: '已放弃', type: 'info' }
      }
      const item = map[row.status] || { text: '未知', type: 'info' }
      return h(NTag, { type: item.type, size: 'small', bordered: false }, () => item.text)
    }
  },
  {
    title: '时间',
    key: 'createTime',
    width: 260,
    render(row: any) { return row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '-' }
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    render(row: any) {
      return h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => viewDetail(row.id) }, () => '查看详情')
    }
  }
]

const detailColumns: DataTableColumns = [
  { title: '题目内容', key: 'content', ellipsis: { tooltip: true } },
  {
    title: '用户答案',
    key: 'userAnswer',
    width: 120,
    render(row: any) {
      return row.userAnswer || '-'
    }
  },
  { title: '正确答案', key: 'correctAnswer', width: 120, render(row: any) { return row.correctAnswer || '-' } },
  {
    title: '结果',
    key: 'isCorrect',
    width: 100,
    render(row: any) {
      if (row.isCorrect === null || row.isCorrect === undefined) {
        return h(NTag, { type: 'warning', size: 'small', bordered: false }, () => '未答')
      }
      return h(NTag, { type: row.isCorrect ? 'success' : 'error', size: 'small', bordered: false }, () => row.isCorrect ? '正确' : '错误')
    }
  }
]

function renderAnswerStats(row: any) {
  return h('div', { style: 'display:flex;flex-wrap:wrap;gap:6px' }, [
    h(NTag, { size: 'small', bordered: false, type: 'success', title: `答对 ${row.correctCount || 0}` }, () => `${row.correctCount || 0}`),
    h(NTag, { size: 'small', bordered: false, type: 'error', title: `答错 ${row.wrongCount || 0}` }, () => `${row.wrongCount || 0}`),
    h(NTag, { size: 'small', bordered: false, type: 'warning', title: `未答 ${row.unansweredCount || 0}` }, () => `${row.unansweredCount || 0}`),
    h(NTag, { size: 'small', bordered: false, type: 'info', title: `总题数 ${row.totalCount || 0}` }, () => `${row.totalCount || 0}`),
  ])
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPracticeList(query) as any
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
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

function handleReset() {
  query.keyword = ''
  query.bankId = null
  query.pageNum = 1
  fetchData()
}

async function viewDetail(id: number) {
  currentDetailId.value = id
  detailQuery.keyword = ''
  detailQuery.result = null
  detailQuery.pageNum = 1
  showDetail.value = true
  await fetchDetail()
}

async function fetchDetail() {
  if (!currentDetailId.value) return
  detailLoading.value = true
  try {
    const res = await getPracticeDetail(currentDetailId.value, detailQuery) as any
    detailData.value = res?.details?.list || []
    detailTotal.value = res?.details?.total || 0
    detailSummary.value = {
      totalCount: res?.summary?.totalCount || 0,
      answerCount: res?.summary?.answerCount || 0,
      correctCount: res?.summary?.correctCount || 0,
      wrongCount: res?.summary?.wrongCount || 0,
      unansweredCount: res?.summary?.unansweredCount || 0,
    }
  } finally {
    detailLoading.value = false
  }
}

function handleDetailPageChange(page: number) {
  detailQuery.pageNum = page
  fetchDetail()
}

function handleDetailPageSizeChange(size: number) {
  detailQuery.pageSize = size
  detailQuery.pageNum = 1
  fetchDetail()
}

function handleDetailSearch() {
  detailQuery.pageNum = 1
  fetchDetail()
}

function handleDetailReset() {
  detailQuery.keyword = ''
  detailQuery.result = null
  detailQuery.pageNum = 1
  fetchDetail()
}

async function loadBanks() {
  try {
    const res = await getBankList({ pageNum: 1, pageSize: 999 }) as any
    bankOptions.value = (res?.list || []).map((b: any) => ({ label: b.name, value: b.id }))
  } catch {}
}

onMounted(() => {
  fetchData()
  loadBanks()
})
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

.search-bar {
  margin-bottom: 12px;
}

.answer-legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 12px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  display: inline-block;
}

.legend-dot.is-correct {
  background: #18a058;
}

.legend-dot.is-wrong {
  background: #d03050;
}

.legend-dot.is-unanswered {
  background: #f0a020;
}

.legend-dot.is-total {
  background: #2080f0;
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.summary-item.is-correct {
  background: #ecfdf3;
  color: #027a48;
}

.summary-item.is-wrong {
  background: #fef3f2;
  color: #b42318;
}

.summary-item.is-unanswered {
  background: #fffaeb;
  color: #b54708;
}

.detail-shell {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-tools {
  align-items: center;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
}

.summary-label {
  display: block;
  font-size: 12px;
  opacity: 0.9;
}

.summary-value {
  display: block;
  margin-top: 6px;
  font-size: 24px;
  font-weight: 600;
  line-height: 1;
}

.detail-pagination {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .detail-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
