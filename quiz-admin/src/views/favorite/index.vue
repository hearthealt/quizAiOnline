<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">收藏管理</span>
          <n-text depth="3">共 {{ total }} 条记录</n-text>
        </div>
      </template>

      <n-space class="search-bar">
        <n-input v-model:value="query.keyword" placeholder="用户昵称/手机号" clearable style="width: 180px" @keyup.enter="handleSearch">
          <template #prefix><n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14"/></svg></n-icon></template>
        </n-input>
        <n-select v-model:value="query.bankId" placeholder="选择题库" clearable :options="bankOptions" style="width: 180px" />
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NIcon, NTag } from 'naive-ui'
import { getAdminFavoriteList } from '@/api/favorite'
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
  {
    title: '题库',
    key: 'bankName',
    width: 240,
    render(row: any) {
      const text = row.bankName || '-'
      return h(NTag, { size: 'small', bordered: false, type: 'info', title: text }, () =>
        h('span', {
          style: 'display:inline-block;max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;vertical-align:bottom;'
        }, text)
      )
    }
  },
  { title: '题目内容', key: 'questionContent', ellipsis: { tooltip: true } },
  {
    title: '收藏时间',
    key: 'createTime',
    width: 160,
    render: (row: any) => row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '-'
  }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminFavoriteList(query) as any
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
</style>
