<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">今日答题</span>
          <n-text depth="3">{{ currentDateLabel }} 共 {{ total }} 条</n-text>
        </div>
      </template>

      <n-space class="search-bar">
        <n-date-picker v-model:value="query.date" type="date" clearable style="width: 180px" />
        <n-input v-model:value="query.keyword" placeholder="用户/题目/题库" clearable style="width: 220px" @keyup.enter="handleSearch" />
        <n-select v-model:value="query.bankId" placeholder="题库" clearable :options="bankOptions" style="width: 180px" />
        <n-select v-model:value="query.source" placeholder="来源" clearable :options="sourceOptions" style="width: 120px" />
        <n-select v-model:value="query.result" placeholder="结果" clearable :options="resultOptions" style="width: 120px" />
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :row-key="(row: any) => `${row.source}-${row.recordId}-${row.questionId}`"
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
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref, computed } from 'vue'
import { NTag } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import dayjs from 'dayjs'
import { getAnswerDetails } from '@/api/activity'
import { getList as getBankList } from '@/api/bank'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const bankOptions = ref<{ label: string; value: number }[]>([])

const sourceOptions = [
  { label: '练习', value: 'PRACTICE' },
  { label: '考试', value: 'EXAM' }
]

const resultOptions = [
  { label: '正确', value: 'CORRECT' },
  { label: '错误', value: 'WRONG' },
  { label: '待判定', value: 'PENDING' }
]

const query = reactive({
  date: dayjs().valueOf() as number | null,
  keyword: '',
  bankId: null as number | null,
  source: null as string | null,
  result: null as string | null,
  pageNum: 1,
  pageSize: 10
})

const currentDateParam = computed(() => dayjs(query.date || Date.now()).format('YYYY-MM-DD'))
const currentDateLabel = computed(() => dayjs(query.date || Date.now()).format('YYYY-MM-DD'))

const columns: DataTableColumns = [
  {
    title: '时间',
    key: 'answerTime',
    width: 150,
    render(row: any) {
      return row.answerTime ? dayjs(row.answerTime).format('YYYY-MM-DD HH:mm:ss') : '-'
    }
  },
  {
    title: '用户',
    key: 'user',
    width: 170,
    render(row: any) {
      return h('div', null, [
        h('div', { style: 'font-weight:600' }, row.userNickname || '-'),
        h('div', { style: 'font-size:12px;color:#94a3b8' }, row.userPhone || '未绑定手机号')
      ])
    }
  },
  { title: '题库', key: 'bankName', width: 140, ellipsis: { tooltip: true } },
  {
    title: '来源',
    key: 'source',
    width: 90,
    render(row: any) {
      return h(NTag, { size: 'small', bordered: false, type: row.source === 'PRACTICE' ? 'info' : 'warning' }, () => row.source === 'PRACTICE' ? '练习' : '考试')
    }
  },
  {
    title: '题目',
    key: 'question',
    minWidth: 280,
    render(row: any) {
      return h('div', null, [
        h('div', { style: 'font-size:12px;color:#94a3b8;margin-bottom:4px' }, `#${row.questionId}`),
        h('div', row.questionContent || '-')
      ])
    }
  },
  { title: '用户答案', key: 'userAnswer', width: 100, render(row: any) { return row.userAnswer || '-' } },
  { title: '正确答案', key: 'correctAnswer', width: 100, render(row: any) { return row.correctAnswer || '-' } },
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

async function fetchData() {
  loading.value = true
  try {
    const res = await getAnswerDetails({
      date: currentDateParam.value,
      keyword: query.keyword || undefined,
      bankId: query.bankId || undefined,
      source: query.source || undefined,
      result: query.result || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    }) as any
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchData()
}

function handleReset() {
  query.date = dayjs().valueOf()
  query.keyword = ''
  query.bankId = null
  query.source = null
  query.result = null
  query.pageNum = 1
  fetchData()
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

async function loadBanks() {
  try {
    const res = await getBankList({ pageNum: 1, pageSize: 999 }) as any
    bankOptions.value = (res?.list || []).map((item: any) => ({ label: item.name, value: item.id }))
  } catch {
    bankOptions.value = []
  }
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}
</style>
