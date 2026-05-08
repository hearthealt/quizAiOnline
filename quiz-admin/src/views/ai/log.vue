<template>
  <PageContainer title="AI调用日志" :subtitle="'共 ' + total + ' 条记录'">
    <DataTableSection>
      <template #search>
        <n-select v-model:value="query.callType" :options="callTypeOptions" placeholder="调用类型" clearable style="width: 120px" />
        <n-select v-model:value="query.status" :options="statusOptions" placeholder="状态" clearable style="width: 100px" />
        <n-select v-model:value="query.mode" :options="modeOptions" placeholder="调用模式" clearable style="width: 160px" />
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </template>
      <n-data-table :columns="columns" :data="tableData" :loading="loading" :row-key="(row: any) => row.id" :row-props="rowProps" striped size="small" />
      <template #pagination>
        <n-pagination
          :page="query.pageNum"
          :page-size="query.pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[10, 20, 50]"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </template>
    </DataTableSection>

    <n-modal v-model:show="showDetail" preset="card" title="调用详情" style="width: 700px">
      <div class="detail-section">
        <div class="detail-title">Prompt</div>
        <n-code :code="currentRow?.prompt || ''" language="text" word-wrap />
      </div>
      <div class="detail-section" v-if="currentRow?.route">
        <div class="detail-title">调用链路</div>
        <n-code :code="currentRow.route" language="text" word-wrap />
      </div>
      <div class="detail-section">
        <div class="detail-title">Result</div>
        <n-code :code="currentRow?.result || ''" language="text" word-wrap />
      </div>
      <div v-if="currentRow?.errorMsg" class="detail-section">
        <div class="detail-title" style="color: var(--color-error)">错误信息</div>
        <n-code :code="currentRow.errorMsg" language="text" word-wrap />
      </div>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NTag } from 'naive-ui'
import { getLogList } from '@/api/ai'
import dayjs from 'dayjs'
import type { DataTableColumns } from 'naive-ui'

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const showDetail = ref(false)
const currentRow = ref<any>(null)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  callType: null as string | null,
  status: null as number | null,
  mode: null as string | null
})

const callTypeOptions = [
  { label: '管理端', value: 'ADMIN' },
  { label: '用户端', value: 'USER' }
]

const statusOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

const modeMap: Record<string, string> = {
  CHAT: 'AI对话',
  GENERATE_ANALYSIS: '生成解析',
  GENERATE_ANSWER: '生成答案',
  GENERATE_BOTH: '生成答案+解析'
}

const modeOptions = [
  { label: 'AI对话', value: 'CHAT' },
  { label: '生成解析', value: 'GENERATE_ANALYSIS' },
  { label: '生成答案', value: 'GENERATE_ANSWER' },
  { label: '生成答案+解析', value: 'GENERATE_BOTH' }
]

const routeMap: Record<string, string> = {
  responses: 'Responses',
  chat: 'Chat',
  'chat-minimal': 'Chat 最小请求',
  'chat-stream-fallback': 'Chat 流式回退'
}

const columns: DataTableColumns = [
  {
    title: '调用类型',
    key: 'callType',
    width: 90,
    render(row: any) {
      const isAdmin = row.callType === 'ADMIN'
      return h(NTag, { type: isAdmin ? 'info' : 'success', size: 'small', bordered: false }, () => isAdmin ? '管理端' : '用户端')
    }
  },
  { title: '题目ID', key: 'questionId', width: 80 },
  { title: '操作人', key: 'operatorName', width: 100, render(row: any) { return row.operatorName || '-' } },
  {
    title: '模式',
    key: 'mode',
    width: 130,
    render(row: any) { return h(NTag, { size: 'small', bordered: false }, () => modeMap[row.mode] || row.mode) }
  },
  {
    title: '链路',
    key: 'route',
    width: 140,
    render(row: any) {
      return row.route
        ? h(NTag, { size: 'small', bordered: false, type: 'warning' }, () => routeMap[row.route] || row.route)
        : '-'
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row: any) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small', bordered: false }, () => row.status === 1 ? '成功' : '失败')
    }
  },
  {
    title: '耗时',
    key: 'costMs',
    width: 80,
    render(row: any) { return h('span', { style: row.costMs > 3000 ? 'color:var(--color-error)' : '' }, `${row.costMs}ms`) }
  },
  { title: 'Token', key: 'tokensUsed', width: 70 },
  { title: '错误信息', key: 'errorMsg', ellipsis: { tooltip: true } },
  {
    title: '时间',
    key: 'createTime',
    width: 150,
    render: (row: any) => row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '-'
  }
]

function rowProps(row: any) {
  return {
    style: 'cursor: pointer',
    onClick: () => {
      currentRow.value = row
      showDetail.value = true
    }
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLogList(query) as any
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

function handleSearch() {
  query.pageNum = 1
  fetchData()
}

function handleReset() {
  query.callType = null
  query.status = null
  query.mode = null
  query.pageNum = 1
  fetchData()
}

onMounted(() => fetchData())
</script>

<style scoped>
.detail-section {
  margin-bottom: 16px;
}

.detail-section:last-child {
  margin-bottom: 0;
}

.detail-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid var(--color-primary);
}
</style>
