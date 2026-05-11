<template>
  <PageContainer title="AI批量任务" :subtitle="'共 ' + total + ' 条记录'">
    <DataTableSection>
      <template #search>
        <n-select v-model:value="query.status" :options="statusOptions" placeholder="任务状态" clearable style="width: 140px" />
        <n-select v-model:value="query.mode" :options="modeOptions" placeholder="生成模式" clearable style="width: 160px" />
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
        <n-button :loading="loading" @click="fetchData()">刷新</n-button>
        <n-switch v-model:value="autoRefresh" />
        <span class="refresh-label">自动刷新</span>
        <n-select
          v-model:value="refreshPreset"
          :options="refreshIntervalOptions"
          :disabled="!autoRefresh"
          style="width: 110px"
        />
        <n-input-number
          v-if="refreshPreset === 'custom'"
          v-model:value="customRefreshSeconds"
          :min="1"
          :max="3600"
          :step="1"
          :disabled="!autoRefresh"
          style="width: 130px"
        >
          <template #suffix>秒</template>
        </n-input-number>
      </template>

      <n-data-table :columns="columns" :data="tableData" :loading="loading" :row-key="(row: AiBatchJob) => row.id" striped size="small" />

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

    <n-modal v-model:show="showDetail" preset="card" title="批量任务详情" style="width: 760px">
      <n-descriptions v-if="currentJob" :column="2" label-placement="left" bordered size="small">
        <n-descriptions-item label="任务ID">{{ currentJob.id }}</n-descriptions-item>
        <n-descriptions-item label="状态">{{ currentJob.statusText }}</n-descriptions-item>
        <n-descriptions-item label="生成模式">{{ modeMap[currentJob.mode] || currentJob.mode }}</n-descriptions-item>
        <n-descriptions-item label="覆盖已有">{{ currentJob.overwrite === 1 ? '是' : '否' }}</n-descriptions-item>
        <n-descriptions-item label="并发数">{{ currentJob.concurrency }}</n-descriptions-item>
        <n-descriptions-item label="总数">{{ currentJob.totalCount }}</n-descriptions-item>
        <n-descriptions-item label="提交数">{{ currentJob.submittedCount }}</n-descriptions-item>
        <n-descriptions-item label="成功">{{ currentJob.successCount }}</n-descriptions-item>
        <n-descriptions-item label="失败">{{ currentJob.failCount }}</n-descriptions-item>
        <n-descriptions-item label="跳过">{{ currentJob.skippedCount }}</n-descriptions-item>
        <n-descriptions-item label="待处理">{{ currentJob.pendingCount }}</n-descriptions-item>
        <n-descriptions-item label="开始时间">{{ formatTime(currentJob.startTime) }}</n-descriptions-item>
        <n-descriptions-item label="结束时间">{{ formatTime(currentJob.endTime) }}</n-descriptions-item>
      </n-descriptions>

      <div v-if="currentJob?.errorMsg" class="detail-section">
        <div class="detail-title error">任务错误</div>
        <n-code :code="currentJob.errorMsg" language="text" word-wrap />
      </div>

      <div class="detail-section">
        <div class="detail-title">最近失败题目</div>
        <n-data-table
          :columns="failedColumns"
          :data="currentJob?.recentFailedItems || []"
          :pagination="false"
          size="small"
          striped
        />
      </div>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { NButton, NPopconfirm, NProgress, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import dayjs from 'dayjs'
import { cancelBatchJob, deleteBatchJob, getBatchJob, getBatchJobList, pauseBatchJob, resumeBatchJob, retryFailedBatchJob } from '@/api/ai'
import type { AiBatchJob, AiBatchJobItem } from '@/types'

const message = useMessage()
const loading = ref(false)
const tableData = ref<AiBatchJob[]>([])
const total = ref(0)
const showDetail = ref(false)
const currentJob = ref<AiBatchJob | null>(null)
const autoRefresh = ref(true)
const refreshing = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

type RefreshPreset = number | 'custom'

const REFRESH_PRESET_KEY = 'aiBatchRefreshPreset'
const CUSTOM_REFRESH_SECONDS_KEY = 'aiBatchCustomRefreshSeconds'

function loadRefreshPreset(): RefreshPreset {
  const value = localStorage.getItem(REFRESH_PRESET_KEY)
  if (value === 'custom') {
    return 'custom'
  }
  const seconds = Number(value)
  return [5, 10, 30].includes(seconds) ? seconds : 5
}

function loadCustomRefreshSeconds() {
  const seconds = Number(localStorage.getItem(CUSTOM_REFRESH_SECONDS_KEY))
  if (!Number.isFinite(seconds) || seconds < 1) {
    return 15
  }
  return Math.min(3600, Math.floor(seconds))
}

const refreshPreset = ref<RefreshPreset>(loadRefreshPreset())
const customRefreshSeconds = ref(loadCustomRefreshSeconds())

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null as number | null,
  mode: null as string | null
})

const modeMap: Record<string, string> = {
  GENERATE_ANALYSIS: '生成解析',
  GENERATE_ANSWER: '生成答案',
  GENERATE_BOTH: '生成答案+解析'
}

const modeOptions = [
  { label: '生成解析', value: 'GENERATE_ANALYSIS' },
  { label: '生成答案', value: 'GENERATE_ANSWER' },
  { label: '生成答案+解析', value: 'GENERATE_BOTH' }
]

const statusOptions = [
  { label: '排队中', value: 0 },
  { label: '执行中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '完成但有失败', value: 3 },
  { label: '执行异常', value: 4 },
  { label: '已暂停', value: 5 },
  { label: '已取消', value: 6 }
]

const refreshIntervalOptions = [
  { label: '5秒', value: 5 },
  { label: '10秒', value: 10 },
  { label: '30秒', value: 30 },
  { label: '自定义', value: 'custom' }
]

const refreshIntervalMs = computed(() => {
  const seconds = refreshPreset.value === 'custom' ? customRefreshSeconds.value : refreshPreset.value
  return Math.max(1, Number(seconds) || 5) * 1000
})

const statusTagMap: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  0: 'default',
  1: 'info',
  2: 'success',
  3: 'warning',
  4: 'error',
  5: 'warning',
  6: 'default'
}

const columns: DataTableColumns<AiBatchJob> = [
  { title: '任务ID', key: 'id', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 120,
    render(row) {
      return h(NTag, { type: statusTagMap[row.status] || 'default', size: 'small', bordered: false }, () => row.statusText)
    }
  },
  {
    title: '模式',
    key: 'mode',
    width: 140,
    render(row) {
      return modeMap[row.mode] || row.mode
    }
  },
  {
    title: '进度',
    key: 'progressPercent',
    width: 180,
    render(row) {
      return h(NProgress, {
        type: 'line',
        percentage: row.progressPercent || 0,
        processing: row.status === 0 || row.status === 1,
        indicatorPlacement: 'inside'
      })
    }
  },
  { title: '并发', key: 'concurrency', width: 70 },
  {
    title: '数量',
    key: 'counts',
    width: 220,
    render(row) {
      return `提交 ${row.submittedCount} / 成功 ${row.successCount} / 失败 ${row.failCount} / 跳过 ${row.skippedCount}`
    }
  },
  { title: '操作人', key: 'operatorName', width: 100, render: (row) => row.operatorName || '-' },
  { title: '创建时间', key: 'createTime', width: 150, render: (row) => formatTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 280,
    render(row) {
      const actions = [
        h(NButton, { text: true, type: 'primary', onClick: () => openDetail(row.id) }, () => '详情')
      ]
      if (row.status === 0 || row.status === 1) {
        actions.push(h(NButton, { text: true, type: 'warning', onClick: () => handlePause(row.id) }, () => '暂停'))
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleCancel(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '取消'),
          default: () => '确定取消该批量任务吗？'
        }))
      }
      if (row.status === 5) {
        actions.push(h(NButton, { text: true, type: 'success', onClick: () => handleResume(row.id) }, () => '继续'))
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleCancel(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '取消'),
          default: () => '确定取消该批量任务吗？'
        }))
      }
      if ([3, 4].includes(row.status) && row.failCount > 0) {
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleRetryFailed(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'warning' }, () => '重试失败'),
          default: () => '确定重新提交失败题目吗？'
        }))
      }
      if ([2, 3, 4, 6].includes(row.status)) {
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '删除'),
          default: () => '确定删除该任务记录吗？'
        }))
      }
      return h(NSpace, { size: 8 }, () => actions)
    }
  }
]

const failedColumns: DataTableColumns<AiBatchJobItem> = [
  { title: '题目ID', key: 'questionId', width: 100 },
  { title: '状态', key: 'statusText', width: 90 },
  { title: '重试次数', key: 'retryCount', width: 90, render: (row) => `${row.retryCount || 0}次` },
  { title: '错误信息', key: 'errorMsg', ellipsis: { tooltip: true } },
  { title: '时间', key: 'updateTime', width: 150, render: (row) => formatTime(row.updateTime) }
]

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function hasRunningJobs() {
  return tableData.value.some((item) => item.status === 0 || item.status === 1)
}

async function fetchData(silent = false) {
  if (refreshing.value) {
    return
  }
  refreshing.value = true
  if (!silent) {
    loading.value = true
  }
  try {
    const res = await getBatchJobList(query) as any
    tableData.value = res?.list || []
    total.value = res?.total || 0
    if (showDetail.value && currentJob.value) {
      currentJob.value = await getBatchJob(currentJob.value.id) as AiBatchJob
    }
    syncAutoRefresh()
  } catch {
    syncAutoRefresh()
  } finally {
    if (!silent) {
      loading.value = false
    }
    refreshing.value = false
  }
}

async function openDetail(id: number) {
  currentJob.value = await getBatchJob(id) as AiBatchJob
  showDetail.value = true
}

async function handlePause(id: number) {
  await pauseBatchJob(id)
  await fetchData()
}

async function handleResume(id: number) {
  await resumeBatchJob(id)
  message.success('任务已继续')
  await fetchData()
}

async function handleRetryFailed(id: number) {
  await retryFailedBatchJob(id)
  message.success('失败题目已重新提交')
  await fetchData()
  if (currentJob.value?.id === id) {
    currentJob.value = await getBatchJob(id) as AiBatchJob
  }
}

async function handleCancel(id: number) {
  await cancelBatchJob(id)
  message.success('任务已取消')
  await fetchData()
  if (currentJob.value?.id === id) {
    currentJob.value = await getBatchJob(id) as AiBatchJob
  }
}

async function handleDelete(id: number) {
  await deleteBatchJob(id)
  if (currentJob.value?.id === id) {
    showDetail.value = false
    currentJob.value = null
  }
  await fetchData()
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
  query.status = null
  query.mode = null
  query.pageNum = 1
  fetchData()
}

function syncAutoRefresh() {
  if (autoRefresh.value && hasRunningJobs() && !refreshTimer) {
    refreshTimer = setInterval(() => {
      void fetchData(true)
    }, refreshIntervalMs.value)
  }
  if ((!autoRefresh.value || !hasRunningJobs()) && refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

function restartAutoRefresh() {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  syncAutoRefresh()
}

watch([autoRefresh, refreshIntervalMs], restartAutoRefresh)

watch(refreshPreset, (value) => {
  localStorage.setItem(REFRESH_PRESET_KEY, String(value))
})

watch(customRefreshSeconds, (value) => {
  const seconds = Math.max(1, Math.min(3600, Number(value) || 15))
  localStorage.setItem(CUSTOM_REFRESH_SECONDS_KEY, String(seconds))
})

onMounted(() => fetchData())

onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
})
</script>

<style scoped>
.detail-section {
  margin-top: 16px;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid var(--color-primary);
}

.detail-title.error {
  color: var(--color-error);
  border-left-color: var(--color-error);
}

.refresh-label {
  color: #64748b;
  font-size: 13px;
}
</style>
