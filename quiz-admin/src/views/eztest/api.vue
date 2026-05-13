<template>
  <PageContainer title="EZTest直连" :subtitle="'共 ' + total + ' 个任务'">
    <template #header-actions>
      <n-space>
        <n-button type="primary" @click="openExportDialog">
          <template #icon>
            <n-icon><CloudDownloadOutline /></n-icon>
          </template>
          新建导出
        </n-button>
        <n-button :loading="jobLoading" @click="fetchJobs()">
          <template #icon>
            <n-icon><RefreshOutline /></n-icon>
          </template>
          刷新
        </n-button>
        <n-button
          v-if="isSuperAdmin"
          type="error"
          :disabled="checkedJobKeys.length === 0"
          @click="handleBatchDelete"
        >
          批量删除 {{ checkedJobKeys.length ? `(${checkedJobKeys.length})` : '' }}
        </n-button>
      </n-space>
    </template>

    <div class="ez-workspace">
      <DataTableSection>
        <template #search>
          <n-select v-model:value="query.status" :options="statusOptions" clearable placeholder="任务状态" style="width: 150px" />
          <n-button type="primary" @click="handleSearch">筛选</n-button>
          <n-button @click="handleReset">重置</n-button>
          <n-button :loading="jobLoading" @click="fetchJobs()">刷新</n-button>
          <span v-if="runningCount" class="refresh-note">运行中任务每 5 秒刷新</span>
        </template>

        <AdminDataTable
          :columns="jobColumns"
          :data="jobs"
          :loading="jobLoading"
          :pagination="false"
          :row-key="(row: EztestJob) => row.id"
          :checked-row-keys="checkedJobKeys"
          @update:checked-row-keys="(keys: any) => (checkedJobKeys = keys)"
          striped
          size="small"
        />

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
    </div>

    <n-modal
      v-model:show="showExportDialog"
      preset="card"
      title="新建导出任务"
      class="ez-export-modal"
      style="width: min(900px, calc(100vw - 32px))"
    >
      <div class="export-dialog">
        <div class="export-dialog-head">
          <n-input v-model:value="form.permit" placeholder="准考证号或手机号" clearable />
          <n-button type="primary" :loading="sessionLoading" @click="handleLoadSessions">获取题库列表</n-button>
        </div>

        <div class="export-options">
          <div class="option-line">
            <span>导出</span>
            <n-checkbox v-model:checked="form.exportXlsx">XLSX</n-checkbox>
            <n-checkbox v-model:checked="form.exportPdfWithAnswers">含答案PDF</n-checkbox>
            <n-checkbox v-model:checked="form.exportPdfWithoutAnswers">不含答案PDF</n-checkbox>
          </div>
          <div class="option-line import-line">
            <n-checkbox v-model:checked="autoImport">导出后导入题库</n-checkbox>
            <n-select
              v-model:value="form.importBankId"
              :disabled="!autoImport"
              :options="bankOptions"
              filterable
              clearable
              placeholder="选择题库，可不选"
            />
            <n-select
              v-if="autoImport && !form.importBankId"
              v-model:value="form.importCategoryId"
              :options="categoryOptions"
              filterable
              placeholder="选择分类"
            />
          </div>
        </div>

        <section class="session-picker">
          <div class="session-picker-head">
            <strong>题库列表</strong>
            <span v-if="sessions.length">已选 {{ selectedSessionIds.length }}/{{ sessions.length }}</span>
          </div>
          <AdminDataTable
            :columns="sessionColumns"
            :data="sessions"
            :loading="sessionLoading"
            :row-key="(row: EztestSession) => row.sessionId"
            v-model:checked-row-keys="selectedSessionIds"
            striped
            size="small"
            max-height="300"
          />
        </section>

        <footer class="dialog-footer">
          <span>{{ selectedSessionIds.length ? `将导出 ${selectedSessionIds.length} 个题库` : '请选择题库后提交任务' }}</span>
          <n-space>
            <n-button @click="showExportDialog = false">取消</n-button>
            <n-button type="primary" :disabled="!selectedSessionIds.length" :loading="submitting" @click="handleCreateJob">
              提交导出任务
            </n-button>
          </n-space>
        </footer>
      </div>
    </n-modal>

    <n-modal v-model:show="showDetail" preset="card" title="任务详情" style="width: 860px">
      <div v-if="currentJob" class="detail-overview">
        <div>
          <span>任务 #{{ currentJob.id }}</span>
          <strong>{{ currentJob.statusText }}</strong>
        </div>
        <n-progress
          type="line"
          :percentage="currentJob.progressPercent || 0"
          :processing="currentJob.status === 0 || currentJob.status === 1"
          indicator-placement="inside"
        />
      </div>

      <div v-if="currentJob" class="detail-stats">
        <div><span>题库进度</span><strong>{{ currentJob.completedCount }}/{{ currentJob.sessionCount }}</strong></div>
        <div><span>原始题数</span><strong>{{ currentJob.rawCount }}</strong></div>
        <div><span>导出题数</span><strong>{{ currentJob.exportedCount }}</strong></div>
        <div><span>去重</span><strong>{{ currentJob.duplicateCount }}</strong></div>
        <div><span>导入新增</span><strong>{{ currentJob.importCreateCount }}</strong></div>
        <div><span>导入更新</span><strong>{{ currentJob.importUpdateCount }}</strong></div>
        <div><span>导入失败</span><strong>{{ currentJob.importFailCount }}</strong></div>
      </div>

      <div class="detail-block">
        <div class="detail-title-row">
          <div class="detail-title">产物文件</div>
          <n-button
            v-if="currentJob && canImportJob(currentJob)"
            size="small"
            type="primary"
            secondary
            @click="openImportDialog(currentJob)"
          >
            导入题库
          </n-button>
        </div>
        <AdminDataTable
          class="file-table"
          :columns="fileColumns"
          :data="jobFiles"
          :loading="fileLoading"
          size="small"
          remote
          :pagination="filePagination"
        />
      </div>

      <div v-if="currentJob?.errorMsg" class="detail-block">
        <div class="detail-title error">错误信息</div>
        <n-code :code="currentJob.errorMsg" language="text" word-wrap />
      </div>

      <div class="detail-block">
        <div class="detail-title">运行日志</div>
        <n-code :code="currentJob?.logs || '-'" language="text" word-wrap />
      </div>
    </n-modal>

    <n-modal
      v-model:show="showImportDialog"
      preset="card"
      title="导入题库"
      class="ez-import-modal"
      style="width: min(560px, calc(100vw - 32px))"
    >
      <n-form label-placement="top">
        <n-form-item label="所属题库">
          <n-select
            v-model:value="manualImport.importBankId"
            :options="bankOptions"
            filterable
            clearable
            placeholder="可不选，不选则按题库名称匹配或创建"
          />
        </n-form-item>
        <n-form-item v-if="!manualImport.importBankId" label="所属分类">
          <n-select
            v-model:value="manualImport.importCategoryId"
            :options="categoryOptions"
            filterable
            placeholder="未选题库时必须选择分类"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showImportDialog = false">取消</n-button>
          <n-button type="primary" :loading="importingJob" @click="handleImportJob">导入</n-button>
        </n-space>
      </template>
    </n-modal>

  </PageContainer>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { NButton, NPopconfirm, NProgress, NSpace, NTag, useDialog, useMessage, type DataTableColumns } from 'naive-ui'
import { CloudDownloadOutline, RefreshOutline } from '@vicons/ionicons5'
import dayjs from 'dayjs'
import * as eztestApi from '@/api/eztest'
import * as bankApi from '@/api/bank'
import * as categoryApi from '@/api/category'
import { useAuthStore } from '@/stores/auth'
import DataTableSection from '@/components/DataTableSection.vue'

interface EztestSession {
  sessionId: string
  name: string
  statusText: string
  time: string
}

interface EztestFile {
  id: number
  jobId: number
  fileTypeText: string
  fileName: string
  fileSize: number
  createTime: string
}

interface EztestJob {
  id: number
  status: number
  statusText: string
  progressPercent: number
  progressText: string
  permitMasked: string
  sessionNames: string[]
  sessionCount: number
  completedCount: number
  rawCount: number
  exportedCount: number
  duplicateCount: number
  importCreateCount: number
  importUpdateCount: number
  importFailCount: number
  importable: number
  errorMsg: string
  logs: string
  createTime: string
  files: EztestFile[]
}

const message = useMessage()
const dialog = useDialog()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.adminInfo?.role === 'super_admin')
const sessions = ref<EztestSession[]>([])
const selectedSessionIds = ref<string[]>([])
const sessionLoading = ref(false)
const submitting = ref(false)
const jobLoading = ref(false)
const jobs = ref<EztestJob[]>([])
const checkedJobKeys = ref<number[]>([])
const total = ref(0)
const showExportDialog = ref(false)
const showDetail = ref(false)
const showImportDialog = ref(false)
const currentJob = ref<EztestJob | null>(null)
const jobFiles = ref<EztestFile[]>([])
const fileLoading = ref(false)
const importJobTarget = ref<EztestJob | null>(null)
const autoImport = ref(false)
const importingJob = ref(false)
const bankOptions = ref<{ label: string; value: number }[]>([])
const categoryOptions = ref<{ label: string; value: number }[]>([])
let refreshTimer: ReturnType<typeof setInterval> | null = null

const form = reactive({
  permit: '',
  exportXlsx: true,
  exportPdfWithAnswers: true,
  exportPdfWithoutAnswers: true,
  importBankId: null as number | null,
  importCategoryId: null as number | null
})

const manualImport = reactive({
  importBankId: null as number | null,
  importCategoryId: null as number | null
})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: null as number | null
})

const fileQuery = reactive({
  pageNum: 1,
  pageSize: 5,
  total: 0
})

const runningCount = computed(() => jobs.value.filter((item) => item.status === 0 || item.status === 1).length)

const statusOptions = [
  { label: '排队中', value: 0 },
  { label: '执行中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '完成但有失败', value: 3 },
  { label: '执行异常', value: 4 }
]

const statusTagMap: Record<number, 'default' | 'info' | 'success' | 'warning' | 'error'> = {
  0: 'default',
  1: 'info',
  2: 'success',
  3: 'warning',
  4: 'error'
}

const sessionColumns: DataTableColumns<EztestSession> = [
  { type: 'selection' },
  { title: '题库名称', key: 'name', ellipsis: { tooltip: true } },
  { title: '状态', key: 'statusText', width: 100 },
  { title: '时间', key: 'time', minWidth: 220, ellipsis: { tooltip: true } }
]

const jobColumns = computed<DataTableColumns<EztestJob>>(() => [
  ...(isSuperAdmin.value ? [{ type: 'selection' as const }] : []),
  {
    title: '状态',
    key: 'status',
    width: 120,
    render(row) {
      return h(NTag, { type: statusTagMap[row.status] || 'default', size: 'small', bordered: false }, () => row.statusText)
    }
  },
  {
    title: '进度',
    key: 'progressPercent',
    width: 170,
    render(row) {
      return h(NProgress, { type: 'line', percentage: row.progressPercent || 0, processing: row.status === 0 || row.status === 1, indicatorPlacement: 'inside' })
    }
  },
  { title: '账号', key: 'permitMasked', width: 120 },
  { title: '题库数', key: 'sessionCount', width: 80 },
  {
    title: '题数',
    key: 'counts',
    width: 180,
    render(row) {
      return `原始 ${row.rawCount || 0} / 导出 ${row.exportedCount || 0}`
    }
  },
  {
    title: '导入',
    key: 'import',
    width: 180,
    render(row) {
      return `新增 ${row.importCreateCount || 0} / 更新 ${row.importUpdateCount || 0} / 失败 ${row.importFailCount || 0}`
    }
  },
  { title: '创建时间', key: 'createTime', width: 150, render: (row) => formatTime(row.createTime) },
  {
    title: '操作',
    key: 'actions',
    width: 230,
    render(row) {
      const actions = [
        h(NButton, { text: true, type: 'primary', onClick: () => openDetail(row.id) }, () => '详情')
      ]
      if (canImportJob(row)) {
        actions.push(h(NButton, { text: true, type: 'primary', onClick: () => openImportDialog(row) }, () => '导入题库'))
      }
      if (isSuperAdmin.value && [2, 3, 4].includes(row.status)) {
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '删除'),
          default: () => '确定删除该任务和产物文件吗？'
        }))
      }
      return h(NSpace, { size: 8 }, () => actions)
    }
  }
])

const fileColumns: DataTableColumns<EztestFile> = [
  { title: '类型', key: 'fileTypeText', width: 130 },
  { title: '文件名', key: 'fileName', ellipsis: { tooltip: true } },
  { title: '大小', key: 'fileSize', width: 100, render: (row) => formatSize(row.fileSize) },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render(row) {
      return h(NButton, { text: true, type: 'primary', onClick: () => handleDownload(row) }, () => '下载')
    }
  }
]

const filePagination = reactive({
  page: 1,
  pageSize: 5,
  itemCount: 0,
  showSizePicker: false,
  onUpdatePage(page: number) {
    fileQuery.pageNum = page
    fetchJobFiles()
  }
})

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}

function formatSize(value?: number) {
  const size = Number(value || 0)
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

async function loadProfile() {
  const res = await eztestApi.getProfile() as any
  form.permit = res?.permit || ''
}

async function saveProfile() {
  await eztestApi.saveProfile({ permit: form.permit })
}

async function openExportDialog() {
  showExportDialog.value = true
  sessions.value = []
  selectedSessionIds.value = []
  autoImport.value = false
  form.importBankId = null
  form.importCategoryId = null
  await loadProfile()
}

async function handleLoadSessions() {
  if (!form.permit.trim()) {
    message.warning('请输入准考证号或手机号')
    return
  }
  sessionLoading.value = true
  try {
    await saveProfile()
    sessions.value = await eztestApi.listSessions({
      permit: form.permit
    }) as EztestSession[]
    selectedSessionIds.value = sessions.value.map((item) => item.sessionId)
    message.success(`已获取 ${sessions.value.length} 个题库`)
  } finally {
    sessionLoading.value = false
  }
}

async function handleCreateJob() {
  if (!selectedSessionIds.value.length) {
    message.warning('请选择题库')
    return
  }
  if (autoImport.value && !form.importBankId && !form.importCategoryId) {
    message.warning('未选择题库时，请选择分类')
    return
  }
  submitting.value = true
  try {
    await saveProfile()
    const res = await eztestApi.createJob({
      permit: form.permit,
      sessionIds: selectedSessionIds.value,
      exportXlsx: form.exportXlsx,
      exportPdfWithAnswers: form.exportPdfWithAnswers,
      exportPdfWithoutAnswers: form.exportPdfWithoutAnswers,
      importBankId: autoImport.value ? form.importBankId : null,
      importCategoryId: autoImport.value && !form.importBankId ? form.importCategoryId : null
    }) as any
    message.success(`任务已提交：${res.jobId}`)
    showExportDialog.value = false
    await fetchJobs()
    startAutoRefresh()
  } finally {
    submitting.value = false
  }
}

async function fetchJobs(silent = false) {
  if (!silent) jobLoading.value = true
  try {
    const res = await eztestApi.getJobList(query) as any
    jobs.value = res.list || []
    total.value = res.total || 0
    checkedJobKeys.value = checkedJobKeys.value.filter((id) => jobs.value.some((job) => job.id === id))
    if (!jobs.value.length && total.value > 0 && query.pageNum > 1) {
      query.pageNum = Math.max(1, Math.ceil(total.value / query.pageSize))
      return fetchJobs(silent)
    }
    if (showDetail.value && currentJob.value) {
      currentJob.value = await eztestApi.getJob(currentJob.value.id) as EztestJob
      await fetchJobFiles(true)
    }
    syncRefresh()
  } finally {
    if (!silent) jobLoading.value = false
  }
}

async function openDetail(id: number) {
  currentJob.value = await eztestApi.getJob(id) as EztestJob
  fileQuery.pageNum = 1
  await fetchJobFiles()
  showDetail.value = true
}

async function fetchJobFiles(silent = false) {
  if (!currentJob.value) {
    jobFiles.value = []
    fileQuery.total = 0
    filePagination.itemCount = 0
    return
  }
  if (!silent) fileLoading.value = true
  try {
    const res = await eztestApi.getJobFiles(currentJob.value.id, {
      pageNum: fileQuery.pageNum,
      pageSize: fileQuery.pageSize
    }) as any
    jobFiles.value = res.list || []
    fileQuery.total = res.total || 0
    if (!jobFiles.value.length && fileQuery.total > 0 && fileQuery.pageNum > 1) {
      fileQuery.pageNum = Math.max(1, Math.ceil(fileQuery.total / fileQuery.pageSize))
      return fetchJobFiles(silent)
    }
    filePagination.page = fileQuery.pageNum
    filePagination.itemCount = fileQuery.total
  } finally {
    if (!silent) fileLoading.value = false
  }
}

function canImportJob(job: EztestJob) {
  return [2, 3].includes(job.status) && Number(job.importable || 0) === 1
}

function openImportDialog(job: EztestJob) {
  importJobTarget.value = job
  manualImport.importBankId = null
  manualImport.importCategoryId = null
  showImportDialog.value = true
}

async function handleImportJob() {
  if (!importJobTarget.value) {
    return
  }
  if (!manualImport.importBankId && !manualImport.importCategoryId) {
    message.warning('未选择题库时，请选择分类')
    return
  }
  importingJob.value = true
  try {
    const updated = await eztestApi.importJob(importJobTarget.value.id, {
      importBankId: manualImport.importBankId,
      importCategoryId: manualImport.importBankId ? null : manualImport.importCategoryId
    }) as EztestJob
    message.success('导入完成')
    showImportDialog.value = false
    if (currentJob.value?.id === updated.id) {
      currentJob.value = updated
    }
    await Promise.all([loadBanks(), fetchJobs(true)])
  } finally {
    importingJob.value = false
  }
}

async function handleDelete(id: number) {
  await eztestApi.deleteJob(id)
  checkedJobKeys.value = checkedJobKeys.value.filter((item) => item !== id)
  if (currentJob.value?.id === id) {
    currentJob.value = null
    jobFiles.value = []
    fileQuery.pageNum = 1
    fileQuery.total = 0
    filePagination.page = 1
    filePagination.itemCount = 0
    showDetail.value = false
  }
  await fetchJobs()
}

function handleBatchDelete() {
  dialog.warning({
    title: '批量删除',
    content: `确定删除选中的 ${checkedJobKeys.value.length} 个 EZTest 任务和产物文件吗？执行中任务不能删除。`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      await eztestApi.batchDeleteJobs(checkedJobKeys.value)
      if (currentJob.value && checkedJobKeys.value.includes(currentJob.value.id)) {
        currentJob.value = null
        jobFiles.value = []
        fileQuery.pageNum = 1
        fileQuery.total = 0
        filePagination.page = 1
        filePagination.itemCount = 0
        showDetail.value = false
      }
      checkedJobKeys.value = []
      message.success('批量删除成功')
      await fetchJobs()
    }
  })
}

async function handleDownload(file: EztestFile) {
  const blob = await eztestApi.downloadFile(file.jobId, file.id) as Blob
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = file.fileName
  a.click()
  URL.revokeObjectURL(url)
}

async function loadBanks() {
  const res = await bankApi.getList({ pageNum: 1, pageSize: 1000 }) as any
  bankOptions.value = (res.list || []).map((item: any) => ({ label: item.name, value: item.id }))
}

async function loadCategories() {
  const res = await categoryApi.getAll() as any
  categoryOptions.value = (res || []).map((item: any) => ({ label: item.name, value: item.id }))
}

function handleSearch() {
  query.pageNum = 1
  fetchJobs()
}

function handleReset() {
  query.status = null
  query.pageNum = 1
  fetchJobs()
}

function handlePageChange(page: number) {
  query.pageNum = page
  fetchJobs()
}

function handlePageSizeChange(size: number) {
  query.pageSize = size
  query.pageNum = 1
  fetchJobs()
}

function hasRunningJobs() {
  return jobs.value.some((item) => item.status === 0 || item.status === 1)
}

function startAutoRefresh() {
  if (!refreshTimer) {
    refreshTimer = setInterval(() => fetchJobs(true), 5000)
  }
}

function syncRefresh() {
  if (hasRunningJobs()) {
    startAutoRefresh()
  } else if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(async () => {
  await Promise.all([loadBanks(), loadCategories(), loadProfile(), fetchJobs()])
})

onBeforeUnmount(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.ez-workspace {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.refresh-note {
  color: var(--color-text-muted);
  font-size: 13px;
}

.export-dialog {
  display: grid;
  gap: 12px;
}

.export-dialog-head {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) auto;
  gap: 10px;
  align-items: center;
}

.export-options {
  display: grid;
  gap: 10px;
  padding: 10px 0;
  border-block: 1px solid rgba(95, 68, 47, 0.08);
}

.option-line {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  min-width: 0;
}

.option-line > span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.import-line {
  display: grid;
  grid-template-columns: 128px minmax(180px, 1fr) minmax(160px, 0.8fr);
  align-items: center;
}

.session-picker {
  min-height: 180px;
}

.session-picker-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.session-picker-head span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding-top: 2px;
}

.dialog-footer span {
  color: var(--color-text-muted);
  font-size: 13px;
}

.detail-overview {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-fade);
}

.detail-overview span {
  display: block;
  color: var(--color-text-muted);
  font-size: 12px;
}

.detail-overview strong {
  display: block;
  color: var(--color-text);
  font-size: 18px;
  margin-top: 4px;
}

.detail-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.detail-stats div {
  padding: 12px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(95, 68, 47, 0.1);
  background: rgba(255, 255, 255, 0.48);
}

.detail-stats span {
  display: block;
  color: var(--color-text-muted);
  font-size: 12px;
}

.detail-stats strong {
  display: block;
  color: var(--color-text);
  font-size: 18px;
  margin-top: 6px;
}

.detail-block {
  margin-top: 16px;
}

.file-table {
  min-height: 270px;
}

:deep(.file-table .n-data-table-wrapper) {
  min-height: 210px;
}

.detail-title {
  font-weight: 700;
  margin-bottom: 8px;
  padding-left: 8px;
  border-left: 3px solid var(--color-primary);
}

.detail-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.detail-title-row .detail-title {
  margin-bottom: 0;
}

.detail-title.error {
  color: var(--color-error);
  border-left-color: var(--color-error);
}

@media (max-width: 980px) {
  .export-dialog-head,
  .import-line,
  .detail-stats {
    grid-template-columns: 1fr;
  }

  .detail-overview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .export-dialog {
    gap: 10px;
  }

  .export-dialog-head {
    gap: 8px;
  }

  .export-dialog-head .n-button {
    width: 100%;
  }

  .export-options {
    gap: 8px;
    padding: 8px 0;
  }

  .option-line {
    gap: 8px 12px;
  }

  .option-line > span {
    flex: 0 0 100%;
  }

  .import-line {
    gap: 8px;
  }

  .import-line > .n-checkbox {
    width: 100%;
  }

  .session-picker {
    min-height: 0;
  }

  .session-picker-head {
    margin-bottom: 6px;
  }

  .dialog-footer {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
    padding-top: 0;
  }

  .dialog-footer .n-space {
    width: 100%;
    display: grid !important;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px !important;
  }

  .dialog-footer .n-space-item,
  .dialog-footer .n-button {
    width: 100%;
  }

  .detail-overview {
    gap: 10px;
    padding: 12px;
  }

  .detail-overview strong,
  .detail-stats strong {
    font-size: 16px;
  }

  .detail-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    margin-top: 10px;
  }

  .detail-stats div {
    padding: 10px;
    border-radius: 10px;
  }

  .detail-block {
    margin-top: 14px;
  }

  .detail-title-row {
    align-items: stretch;
  }

  .file-table {
    min-height: 0;
  }

  :deep(.file-table .n-data-table-wrapper) {
    min-height: 0;
  }
}
</style>
