<template>
  <div class="convert-container">
    <!-- 左侧：上传区域 -->
    <div class="panel left-panel" :class="{ collapsed: leftPanelCollapsed }">
      <div class="panel-header">
        <span class="panel-title">
          <n-icon size="18" class="panel-icon">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8zm4 18H6V4h7v5h5z"/></svg>
          </n-icon>
          文件上传
        </span>
      </div>

      <div class="panel-content">
        <n-upload
          v-if="!parsing && !parsedQuestions.length"
          :max="1"
          accept=".xlsx,.xls,.csv,.pdf,.txt,.docx"
          :default-upload="false"
          @change="handleFileChange"
          class="upload-area"
        >
          <n-upload-dragger>
            <div class="upload-icon">
              <n-icon size="52">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96M14 13v4h-4v-4H7l5-5 5 5h-3z"/></svg>
              </n-icon>
            </div>
            <div class="upload-text">点击或拖拽文件到此处</div>
            <div class="upload-hint">
              <n-space vertical :size="4">
                <n-tag size="small" type="info" round>xlsx / xls</n-tag>
                <n-tag size="small" type="success" round>csv</n-tag>
                <n-tag size="small" type="warning" round>pdf / docx / txt</n-tag>
              </n-space>
            </div>
          </n-upload-dragger>
        </n-upload>

        <div v-else-if="parsing" class="parsing-state">
          <n-spin size="large" />
          <div class="parsing-text">正在智能解析文件...</div>
        </div>

        <div v-else class="success-state">
          <div class="success-icon">
            <n-icon size="48" color="#52c41a">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2m-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8z"/></svg>
            </n-icon>
          </div>
          <div class="success-text">解析完成！</div>
          <n-text depth="3">共解析 {{ parsedQuestions.length }} 道题目</n-text>
          <n-button @click="handleClear" style="margin-top: 16px;">
            重新上传
          </n-button>
        </div>
      </div>
    </div>

    <!-- 右侧：转换结果 -->
    <div class="panel right-panel">
      <div class="panel-header">
        <div class="header-left">
          <span class="panel-title">
            <n-icon size="18" class="panel-icon">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m-5 14H7v-2h7v2m3-4H7v-2h10v2m0-4H7V7h10v2z"/></svg>
            </n-icon>
            转换结果
          </span>
        </div>
        <n-space v-if="parsedQuestions.length" :size="8" align="center">
          <n-text depth="3" class="question-count">
            <n-icon size="14" color="#52c41a">
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2m-7 14h-2v-4H7v-2h5V7h2v10z"/></svg>
            </n-icon>
            {{ parsedQuestions.length }} 道题
          </n-text>
          <n-divider vertical style="height: 16px; margin: 0;" />
          <n-button text size="small" @click="showImportModal = true" class="action-link">
            <template #icon>
              <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon>
            </template>
            导入题库
          </n-button>
          <n-button text size="small" @click="downloadResult" class="action-link" type="success">
            <template #icon>
              <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg></n-icon>
            </template>
            下载 Excel
          </n-button>
        </n-space>
      </div>

      <!-- 统计信息 -->
      <div v-if="parsedQuestions.length" class="stats-bar">
        <n-space :size="20">
          <div class="stat-item">
            <n-tag type="info" size="small" round>单选</n-tag>
            <span class="stat-value">{{ typeCount('单选') }}</span>
          </div>
          <div class="stat-item">
            <n-tag type="warning" size="small" round>多选</n-tag>
            <span class="stat-value">{{ typeCount('多选') }}</span>
          </div>
          <div class="stat-item">
            <n-tag type="success" size="small" round>判断</n-tag>
            <span class="stat-value">{{ typeCount('判断') }}</span>
          </div>
          <div class="stat-item">
            <n-tag type="default" size="small" round>填空</n-tag>
            <span class="stat-value">{{ typeCount('填空') }}</span>
          </div>
          <n-divider vertical style="height: 20px; margin: 0;" />
          <n-text v-if="warningCount > 0" type="warning" class="warning-text">
            <n-icon size="14"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"/></svg></n-icon>
            {{ warningCount }} 题可能有问题
          </n-text>
        </n-space>
      </div>

      <div v-if="!parsedQuestions.length && !parsing" class="empty-state">
        <div class="empty-icon">
          <n-icon size="64" color="#d9d9d9">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8zm4 18H6V4h7v5h5zm-5-2c.6 0 1-.4 1-1s-.4-1-1-1s-1 .4-1 1s.4 1 1 1m2-4H9v2h6zm0-4H9v2h6z"/></svg>
          </n-icon>
        </div>
        <div class="empty-text">转换结果将显示在这里</div>
        <n-text depth="3">上传文件开始智能解析</n-text>
      </div>

      <!-- 表格预览 -->
      <div v-else class="result-table-wrapper">
        <n-data-table
          :columns="tableColumns"
          :data="parsedQuestions"
          :max-height="9999"
          size="small"
          :row-class-name="rowClassName"
          striped
        />
      </div>
    </div>

    <!-- 导入题库弹窗 -->
    <n-modal v-model:show="showImportModal" preset="dialog" title="导入到题库" positive-text="确认导入" negative-text="取消" @positive-click="handleImport" :loading="importing" style="width: 480px;">
      <template #header>
        <div class="modal-header">
          <n-icon size="20" color="#1890ff" style="margin-right: 8px;">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg>
          </n-icon>
          导入到题库
        </div>
      </template>
      <n-form-item label="选择题库" label-placement="top">
        <n-select
          v-model:value="importBankId"
          :options="bankOptions"
          placeholder="请选择目标题库"
          filterable
          size="large"
        />
      </n-form-item>
      <div class="import-summary">
        <n-space vertical :size="8">
          <n-text depth="2">将把 <n-text type="info" strong>{{ parsedQuestions.length }}</n-text> 道题目导入到所选题库中</n-text>
          <n-text depth="3" type="warning" v-if="warningCount > 0">注意：有 {{ warningCount }} 道题目可能存在数据问题，请检查后再导入</n-text>
        </n-space>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted } from 'vue'
import { NTag, useMessage } from 'naive-ui'
import type { DataTableColumn } from 'naive-ui'
import * as XLSX from 'xlsx'
import * as questionApi from '@/api/question'
import * as bankApi from '@/api/bank'

const message = useMessage()

const parsedQuestions = ref<any[]>([])
const parsing = ref(false)

// 导入相关
const showImportModal = ref(false)
const importBankId = ref<number | null>(null)
const importing = ref(false)
const bankOptions = ref<{ label: string; value: number }[]>([])

// 加载题库列表
onMounted(async () => {
  try {
    const res = await bankApi.getList({ pageNum: 1, pageSize: 1000 }) as any
    bankOptions.value = (res.list || []).map((b: any) => ({
      label: b.name,
      value: b.id
    }))
  } catch {}

  })

// 统计
function typeCount(type: string) {
  return parsedQuestions.value.filter(q => q.type === type).length
}

const warningCount = computed(() => {
  return parsedQuestions.value.filter(q => !q.answer || !q.content || (q.type !== '判断' && q.type !== '填空' && (!q.options || q.options.length === 0))).length
})

function rowClassName(row: any) {
  if (!row.answer || !row.content) return 'warning-row'
  if (row.type !== '判断' && row.type !== '填空' && (!row.options || row.options.length === 0)) return 'warning-row'
  return ''
}

// 表格列定义

const tableColumns: DataTableColumn[] = [
  {
    title: '#',
    key: 'index',
    width: 80,
    render: (_row, index) => index + 1
  },
  {
    title: '题目内容',
    key: 'content',
    ellipsis: { tooltip: true },
    minWidth: 200
  },
  {
    title: '题型',
    key: 'type',
    width: 80,
    render: (row: any) => h(NTag, { size: 'small', type: row.type === '单选' ? 'info' : row.type === '多选' ? 'warning' : row.type === '判断' ? 'success' : 'default' }, { default: () => row.type })
  },
  {
    title: '答案',
    key: 'answer',
    width: 90,
    ellipsis: { tooltip: true }
  },
  {
    title: '选项数',
    key: 'optionCount',
    width: 80,
    render: (row: any) => row.options?.length || 0
  },
  {
    title: '难度',
    key: 'difficulty',
    width: 60
  }
]

async function handleFileChange(opt: any) {
  const file = opt.file?.file
  if (!file) return

  parsing.value = true
  parsedQuestions.value = []

  try {
    message.loading('正在智能解析文件...')
    const res = await questionApi.convertSmartParse(file) as any
    parsedQuestions.value = res.questions || []
    message.success(res.message || `解析完成，共 ${parsedQuestions.value.length} 道题`)
  } catch (e: any) {
    message.error(e.message || '文件解析失败')
  } finally {
    parsing.value = false
  }
}

function handleClear() {
  parsedQuestions.value = []
}

async function handleImport() {
  if (!importBankId.value) {
    message.warning('请选择目标题库')
    return false
  }
  if (parsedQuestions.value.length === 0) {
    message.warning('没有可导入的题目')
    return false
  }

  importing.value = true
  try {
    const res = await questionApi.convertImport(importBankId.value, parsedQuestions.value) as any
    const successCount = res.successCount || 0
    const failCount = res.failCount || 0
    if (failCount > 0) {
      message.warning(`导入完成：成功 ${successCount} 题，失败 ${failCount} 题`)
    } else {
      message.success(`成功导入 ${successCount} 道题目`)
    }
    showImportModal.value = false
  } catch (e: any) {
    message.error(e.message || '导入失败')
    return false
  } finally {
    importing.value = false
  }
}

function downloadResult() {
  if (parsedQuestions.value.length === 0) {
    message.warning('没有可导出的题目')
    return
  }

  const data: any[][] = []

  const maxOptions = Math.max(4, ...parsedQuestions.value.map(q => q.options?.length || 0))
  const headers = ['题目内容', '题型', '正确答案', '解析', '难度']
  for (let i = 0; i < maxOptions; i++) {
    headers.push(`选项${String.fromCharCode(65 + i)}`)
  }
  data.push(headers)

  for (const q of parsedQuestions.value) {
    const row = [
      q.content || '',
      q.type || '',
      q.answer || '',
      q.analysis || '',
      q.difficulty || ''
    ]
    const options = q.options || []
    for (let i = 0; i < maxOptions; i++) {
      row.push(options[i] || '')
    }
    data.push(row)
  }

  const ws = XLSX.utils.aoa_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '题目')
  XLSX.writeFile(wb, '转换结果.xlsx')
}
</script>

<style scoped>
.convert-container {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
  padding: 8px;
  position: relative;
  align-items: stretch;
}

/* 左侧面板 */
.left-panel {
  flex: 0 0 260px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  border: 1px solid #e8eaed;
  transition: all 0.3s ease;
}

.left-panel {
  flex: 0 0 260px;
}

.left-panel.collapsed :deep(.panel-content) {
  display: none;
}

/* 右侧面板 */
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  border: 1px solid #e8eaed;
  min-width: 0;
}

/* 面板头部 */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f2;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f6f8 100%);
}

.panel-header {
  justify-content: space-between;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
  color: #1a1a1a;
  display: flex;
  align-items: center;
  gap: 8px;
}

.panel-icon {
  color: #1890ff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}


/* 面板内容区域 */
.panel-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 上传区域 */
.upload-area {
  flex: 1;
  display: flex;
  padding: 24px;
}

.upload-area :deep(.n-upload-dragger) {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  border: 2px dashed #d9d9d9;
  border-radius: 16px;
  transition: all 0.3s ease;
  background: #fafbfc;
}

.upload-area :deep(.n-upload-dragger:hover) {
  border-color: #1890ff;
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f4ff 100%);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(24, 144, 255, 0.12);
}

.upload-icon {
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.upload-text {
  font-size: 16px;
  color: #333;
  margin-bottom: 16px;
  font-weight: 500;
}

.upload-hint {
  font-size: 12px;
  color: #999;
}

/* 解析状态 */
.parsing-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.parsing-text {
  font-size: 14px;
  color: #999;
}

/* 成功状态 */
.success-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  padding: 40px;
  animation: slideUp 0.4s ease-out;
}

.success-icon {
  animation: scaleIn 0.5s ease-out;
}

.success-text {
  font-size: 18px;
  font-weight: 500;
  color: #52c41a;
}

@keyframes scaleIn {
  from {
    transform: scale(0);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 统计栏 */
.stats-bar {
  padding: 12px 24px;
  border-bottom: 1px solid #f0f0f2;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f6f8 100%);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-value {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.question-count {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.action-link {
  font-size: 13px;
  transition: all 0.2s;
}

.action-link:hover {
  background: #f5f9ff;
  border-radius: 4px;
}

.warning-text {
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 结果表格 */
.result-table-wrapper {
  flex: 1;
  overflow: auto;
  min-height: 0;
  position: relative;
  contain: strict;
}

.result-table-wrapper :deep(.n-data-table) {
  height: auto;
  width: 100%;
}

/* 空状态 */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 60px 40px;
}

.empty-icon {
  animation: fadeIn 0.6s ease-out;
}

.empty-text {
  font-size: 16px;
  font-weight: 500;
  color: #999;
}

/* 警告行样式 */
:deep(.warning-row) {
  background-color: #fffbe6 !important;
}

:deep(.warning-row:hover) {
  background-color: #fff2b8 !important;
}

/* 表格样式优化 */
:deep(.n-data-table) {
  font-size: 13px;
}

:deep(.n-data-table-th) {
  font-weight: 600;
  background: #fafafa;
  color: #333;
  padding: 12px 16px;
}

:deep(.n-data-table .n-data-table-td) {
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f2;
}

:deep(.n-data-table tr:hover .n-data-table-td) {
  background: #f5f9ff;
}

/* 模态框样式 */
.modal-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
}

.import-summary {
  background: #f5f9ff;
  padding: 16px;
  border-radius: 8px;
  margin-top: 8px;
}

/* 滚动条样式 - 隐藏默认滚动条 */
:deep(.n-scrollbar-rail) {
  background: transparent;
}

:deep(.n-scrollbar-rail__scrollbar) {
  background: transparent !important;
  opacity: 0 !important;
}

/* 自定义滚动条 */
.result-table-wrapper {
  &::-webkit-scrollbar {
    width: 8px;
  }

  &::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 4px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
  }
}
</style>
