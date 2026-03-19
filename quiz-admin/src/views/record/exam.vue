<template>
  <div class="page-container">
    <!-- 搜索区 -->
    <n-card :bordered="false" class="search-card">
      <n-space>
        <n-input v-model:value="query.keyword" placeholder="用户昵称/手机号" clearable style="width: 180px" @keyup.enter="fetchData">
          <template #prefix><n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14"/></svg></n-icon></template>
        </n-input>
        <n-select v-model:value="query.bankId" placeholder="选择题库" clearable :options="bankOptions" style="width: 180px" />
        <n-button type="primary" @click="fetchData">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>
    </n-card>

    <!-- 表格区 -->
    <n-card :bordered="false" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">考试记录</span>
          <n-text depth="3">共 {{ total }} 条记录</n-text>
        </div>
      </template>

      <n-data-table :columns="columns" :data="tableData" :loading="loading" :row-key="(row: any) => row.id" striped />

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
    <n-modal v-model:show="showDetail" preset="card" title="考试详情" style="width: 750px">
      <n-data-table :columns="detailColumns" :data="detailData" :loading="detailLoading" size="small" :max-height="400" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NButton, NTag } from 'naive-ui'
import { getExamList, getExamDetail } from '@/api/record'
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
const detailData = ref<any[]>([])
const detailLoading = ref(false)

const columns: DataTableColumns = [
  { title: 'ID', key: 'id', width: 60 },
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
  {
    title: '成绩',
    key: 'score',
    width: 120,
    render(row: any) {
      const passed = row.score >= row.passScore
      return h('div', null, [
        h('span', { style: `font-size:20px;font-weight:600;color:${passed ? '#18a058' : '#d03050'}` }, row.score),
        h('span', { style: 'color:#999;font-size:12px;margin-left:4px' }, `/ ${row.passScore}分及格`),
      ])
    }
  },
  {
    title: '答题情况',
    key: 'correct',
    width: 100,
    render(row: any) {
      return h('span', null, [
        h('span', { style: 'color:#18a058;font-weight:500' }, row.correctCount),
        h('span', { style: 'color:#999' }, ` / ${row.totalCount} 题`),
      ])
    }
  },
  {
    title: '结果',
    key: 'passed',
    width: 80,
    render(row: any) {
      const passed = row.score >= row.passScore
      return h(NTag, { type: passed ? 'success' : 'error', size: 'small', bordered: false }, () => passed ? '及格' : '不及格')
    }
  },
  {
    title: '用时',
    key: 'duration',
    width: 80,
    render(row: any) {
      if (!row.duration) return '-'
      const min = Math.floor(row.duration / 60)
      const sec = row.duration % 60
      return `${min}分${sec}秒`
    }
  },
  {
    title: '时间',
    key: 'createTime',
    width: 160,
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
  { title: '题目ID', key: 'questionId', width: 70 },
  { title: '题目内容', key: 'content', ellipsis: { tooltip: true } },
  { title: '用户答案', key: 'userAnswer', width: 100 },
  { title: '正确答案', key: 'correctAnswer', width: 100 },
  {
    title: '结果',
    key: 'isCorrect',
    width: 80,
    render(row: any) {
      return h(NTag, { type: row.isCorrect ? 'success' : 'error', size: 'small', bordered: false }, () => row.isCorrect ? '正确' : '错误')
    }
  }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getExamList(query) as any
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
  showDetail.value = true
  detailLoading.value = true
  try {
    const res = await getExamDetail(id) as any
    detailData.value = res?.answers || res?.details || []
  } finally {
    detailLoading.value = false
  }
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
  gap: 16px;
}

.search-card, .main-card {
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

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}
</style>
