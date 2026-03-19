<template>
  <div class="page-container">
    <n-card :bordered="false" class="search-card">
      <n-space>
        <n-input v-model:value="query.keyword" placeholder="用户昵称/手机号" clearable style="width: 180px" @keyup.enter="fetchData">
          <template #prefix><n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14"/></svg></n-icon></template>
        </n-input>
        <n-select v-model:value="query.status" placeholder="订单状态" clearable :options="statusOptions" style="width: 140px" />
        <n-button type="primary" @click="fetchData">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>
    </n-card>

    <n-card :bordered="false" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">VIP订单列表</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NButton, NSpace, NTag, useMessage, useDialog } from 'naive-ui'
import { getOrderList, approveOrder, rejectOrder } from '@/api/vip'
import dayjs from 'dayjs'
import type { DataTableColumns } from 'naive-ui'

const message = useMessage()
const dialog = useDialog()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)

const statusOptions = [
  { label: '待审核', value: 0 },
  { label: '已开通', value: 1 },
  { label: '已驳回', value: 2 }
]

const statusMap: Record<number, { text: string; type: 'warning' | 'success' | 'default' }> = {
  0: { text: '待审核', type: 'warning' },
  1: { text: '已开通', type: 'success' },
  2: { text: '已驳回', type: 'default' }
}

const query = reactive({
  keyword: '',
  status: null as number | null,
  pageNum: 1,
  pageSize: 10
})

const columns: DataTableColumns = [
  { title: '订单号', key: 'orderNo', width: 180, render: (row: any) => h('span', { style: 'font-family:monospace;font-size:12px' }, row.orderNo) },
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
  { title: '套餐', key: 'planName', width: 100, render: (row: any) => h(NTag, { size: 'small', bordered: false, type: 'warning' }, () => row.planName) },
  {
    title: '金额',
    key: 'amount',
    width: 120,
    render: (row: any) => h('span', { style: 'font-size:16px;font-weight:600;color:#667eea' }, `¥${row.amount}`)
  },
  { title: '时长', key: 'duration', width: 80, render: (row: any) => `${row.duration}天` },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render(row: any) {
      const item = statusMap[row.status] || { text: '未知', type: 'default' }
      return h(NTag, { type: item.type, size: 'small', bordered: false }, () => item.text)
    }
  },
  { title: '支付方式', key: 'paymentMethod', width: 80 },
  {
    title: '创建时间',
    key: 'createTime',
    width: 150,
    render: (row: any) => row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '-'
  },
  {
    title: '操作',
    key: 'action',
    width: 130,
    render(row: any) {
      if (row.status !== 0) return h(NTag, { size: 'small', bordered: false }, () => '已处理')
      return h(NSpace, { size: 4 }, () => [
        h(NButton, { size: 'small', type: 'primary', onClick: () => handleApprove(row) }, () => '通过'),
        h(NButton, { size: 'small', type: 'error', onClick: () => handleReject(row) }, () => '驳回'),
      ])
    }
  }
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getOrderList(query) as any
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
  query.status = null
  query.pageNum = 1
  fetchData()
}

function handleApprove(row: any) {
  dialog.warning({
    title: '确认通过',
    content: `确定通过订单 ${row.orderNo} 的VIP申请？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await approveOrder(row.id)
      message.success('已通过')
      fetchData()
    }
  })
}

function handleReject(row: any) {
  dialog.error({
    title: '确认驳回',
    content: `确定驳回订单 ${row.orderNo}？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await rejectOrder(row.id)
      message.success('已驳回')
      fetchData()
    }
  })
}

onMounted(() => fetchData())
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
