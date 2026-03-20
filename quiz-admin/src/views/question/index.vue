<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">题目管理</span>
          <n-space>
            <n-button type="primary" @click="openDrawer()">
              <template #icon><n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon></template>
              新增题目
            </n-button>
            <n-button @click="showImportModal = true">批量导入</n-button>
            <n-button :disabled="checkedKeys.length === 0" @click="handleBatchDelete">
              批量删除 {{ checkedKeys.length ? `(${checkedKeys.length})` : '' }}
            </n-button>
            <n-button type="warning" @click="showAiModal = true">AI批量生成</n-button>
          </n-space>
        </div>
      </template>

      <n-space class="search-bar">
        <n-select v-model:value="searchParams.bankId" placeholder="选择题库" :options="bankOptions" clearable style="width: 200px" />
        <n-select v-model:value="searchParams.type" placeholder="题目类型" :options="typeOptions" clearable style="width: 140px" />
        <n-input v-model:value="searchParams.keyword" placeholder="搜索题目内容" clearable style="width: 200px" @keyup.enter="handleSearch">
          <template #prefix><n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14"/></svg></n-icon></template>
        </n-input>
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>

      <n-data-table
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="false"
        :row-key="(row: Question) => row.id"
        :checked-row-keys="checkedKeys"
        @update:checked-row-keys="(keys: any) => (checkedKeys = keys)"
        size="small"
        striped
      />

      <div class="pagination-wrap">
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="pagination.pageSizes"
          :show-size-picker="pagination.showSizePicker"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </n-card>

    <!-- 新增/编辑抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="600">
      <n-drawer-content :title="editingId ? '编辑题目' : '新增题目'" closable>
        <n-form ref="formRef" :model="formValue" :rules="formRules" label-placement="left" label-width="80">
          <n-form-item label="所属题库" path="bankId">
            <n-select v-model:value="formValue.bankId" :options="bankOptions" placeholder="请选择题库" />
          </n-form-item>
          <n-form-item label="题型" path="type">
            <n-select v-model:value="formValue.type" :options="typeOptions" placeholder="请选择题型" />
          </n-form-item>
          <n-form-item label="题目内容" path="content">
            <n-input v-model:value="formValue.content" type="textarea" :rows="4" placeholder="请输入题目内容" />
          </n-form-item>

          <template v-if="formValue.type === 1 || formValue.type === 2 || formValue.type === 3">
            <n-form-item label="选项">
              <div class="options-list">
                <div v-for="(opt, index) in formOptions" :key="index" class="option-item">
                  <n-tag :bordered="false" size="small" :type="getOptionTagType(opt.label)">{{ opt.label }}</n-tag>
                  <n-input v-model:value="opt.content" placeholder="选项内容" style="flex: 1" />
                  <n-button v-if="formOptions.length > 2" text type="error" @click="formOptions.splice(index, 1)">
                    <n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 6.41L17.59 5L12 10.59L6.41 5L5 6.41L10.59 12L5 17.59L6.41 19L12 13.41L17.59 19L19 17.59L13.41 12z"/></svg></n-icon>
                  </n-button>
                </div>
                <n-button v-if="formValue.type !== 3 && formOptions.length < 8" dashed size="small" @click="addOption" style="width: 100%">
                  + 添加选项
                </n-button>
              </div>
            </n-form-item>
          </template>

          <n-form-item label="答案" path="answer">
            <template v-if="formValue.type === 2">
              <n-checkbox-group v-model:value="multiAnswer">
                <n-space>
                  <n-checkbox v-for="opt in formOptions" :key="opt.label" :value="opt.label">{{ opt.label }}</n-checkbox>
                </n-space>
              </n-checkbox-group>
            </template>
            <template v-else>
              <n-input v-model:value="formValue.answer" placeholder="请输入答案" />
            </template>
          </n-form-item>
          <n-form-item label="解析" path="analysis">
            <n-input v-model:value="formValue.analysis" type="textarea" :rows="3" placeholder="请输入解析（可选）" />
          </n-form-item>
          <n-form-item label="难度" path="difficulty">
            <n-radio-group v-model:value="formValue.difficulty">
              <n-radio-button :value="1">简单</n-radio-button>
              <n-radio-button :value="2">中等</n-radio-button>
              <n-radio-button :value="3">困难</n-radio-button>
            </n-radio-group>
          </n-form-item>
          <n-form-item v-if="editingId" label="排序" path="sort">
            <n-input-number v-model:value="formValue.sort" :min="0" style="width: 100%" />
          </n-form-item>
        </n-form>
        <template #footer>
          <n-space>
            <n-button @click="showDrawer = false">取消</n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>

    <!-- 导入弹窗 -->
    <n-modal v-model:show="showImportModal" preset="card" title="批量导入题目" style="width: 480px">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="所属题库">
          <n-select v-model:value="importBankId" :options="bankOptions" placeholder="请选择题库" />
        </n-form-item>
        <n-form-item label="Excel文件">
          <n-upload :max="1" accept=".xlsx,.xls" :default-upload="false" @change="(opt: any) => (importFile = opt.file?.file || null)">
            <n-upload-dragger>
              <div style="padding: 20px 0">
                <n-icon size="32" color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M9 16h6v-6h4l-7-7l-7 7h4zm-4 2h14v2H5z"/></svg></n-icon>
                <div style="color: #666; margin-top: 8px">点击或拖拽文件到此处</div>
              </div>
            </n-upload-dragger>
          </n-upload>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="space-between" style="width: 100%">
          <n-button text type="primary" @click="handleDownloadTemplate">下载模板</n-button>
          <n-space>
            <n-button @click="showImportModal = false">取消</n-button>
            <n-button type="primary" :loading="importLoading" @click="handleImport">导入</n-button>
          </n-space>
        </n-space>
      </template>
    </n-modal>

    <!-- AI生成弹窗 -->
    <n-modal v-model:show="showAiModal" preset="card" title="AI批量生成解析" style="width: 480px">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="生成模式">
          <n-radio-group v-model:value="aiMode">
            <n-space vertical>
              <n-radio value="GENERATE_ANALYSIS">仅生成解析</n-radio>
              <n-radio value="GENERATE_ANSWER">仅生成答案</n-radio>
              <n-radio value="GENERATE_BOTH">生成答案+解析</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="题库范围">
          <n-select v-model:value="aiBankId" :options="bankOptions" placeholder="全部题库" clearable />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showAiModal = false">取消</n-button>
          <n-button type="primary" :loading="aiLoading" @click="handleAiGenerate">开始生成</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, ref, reactive, onMounted, watch } from 'vue'
import { NButton, NPopconfirm, NSpace, NTag, NPagination, useMessage, useDialog, type DataTableColumns, type FormRules } from 'naive-ui'
import type { Question } from '@/types'
import * as questionApi from '@/api/question'
import * as bankApi from '@/api/bank'
import * as aiApi from '@/api/ai'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const message = useMessage()
const dialog = useDialog()

const typeOptions = [
  { label: '单选', value: 1 },
  { label: '多选', value: 2 },
  { label: '判断', value: 3 },
  { label: '填空', value: 4 },
]
const typeMap: Record<number, { label: string; type: 'success' | 'info' | 'warning' | 'error' }> = {
  1: { label: '单选', type: 'success' },
  2: { label: '多选', type: 'info' },
  3: { label: '判断', type: 'warning' },
  4: { label: '填空', type: 'error' },
}
const difficultyMap: Record<number, { label: string; type: 'success' | 'warning' | 'error' }> = {
  1: { label: '简单', type: 'success' },
  2: { label: '中等', type: 'warning' },
  3: { label: '困难', type: 'error' },
}

const searchParams = reactive({ bankId: null as number | null, type: null as number | null, keyword: '' })
const bankOptions = ref<{ label: string; value: number }[]>([])
const checkedKeys = ref<number[]>([])

const { loading, data, pagination, fetchData, handlePageChange, handlePageSizeChange } = useTable<Question>(
  (params) => questionApi.getList({ ...searchParams, ...params } as any) as any
)

const showDrawer = ref(false)
const editingId = ref<number | null>(null)
const submitLoading = ref(false)
const formOptions = ref<{ label: string; content: string }[]>([])
const multiAnswer = ref<string[]>([])

const defaultForm = { bankId: null as number | null, type: 1, content: '', answer: '', analysis: '', difficulty: 1, sort: 0 }
const { formValue, formRef, resetForm, validate } = useForm(defaultForm)

const formRules: FormRules = {
  bankId: { required: true, type: 'number', message: '请选择题库', trigger: 'change' },
  type: { required: true, type: 'number', message: '请选择题型', trigger: 'change' },
  content: { required: true, message: '请输入题目内容', trigger: 'blur' },
}

const showImportModal = ref(false)
const importBankId = ref<number | null>(null)
const importFile = ref<File | null>(null)
const importLoading = ref(false)

const showAiModal = ref(false)
const aiMode = ref('GENERATE_ANALYSIS')
const aiBankId = ref<number | null>(null)
const aiLoading = ref(false)

const labels = 'ABCDEFGH'.split('')

function getOptionTagType(label: string) {
  const types = ['success', 'info', 'warning', 'error'] as const
  return types[labels.indexOf(label) % 4]
}

function initOptions(type: number) {
  if (type === 3) {
    formOptions.value = [{ label: 'A', content: '正确' }, { label: 'B', content: '错误' }]
  } else if (type === 1 || type === 2) {
    formOptions.value = labels.slice(0, 4).map((l) => ({ label: l, content: '' }))
  } else {
    formOptions.value = []
  }
}

function addOption() {
  const next = labels[formOptions.value.length]
  if (next) formOptions.value.push({ label: next, content: '' })
}

watch(() => formValue.value.type, (t) => {
  if (showDrawer.value && !editingId.value) initOptions(t)
})

const columns: DataTableColumns<Question> = [
  { type: 'selection' },
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '题库',
    key: 'bankId',
    width: 140,
    render(row) {
      const opt = bankOptions.value.find((b) => b.value === row.bankId)
      return h(NTag, { size: 'small', bordered: false, type: 'info' }, () => opt?.label || '-')
    },
  },
  {
    title: '题型',
    key: 'type',
    width: 80,
    render(row) {
      const t = typeMap[row.type]
      return t ? h(NTag, { type: t.type, size: 'small', bordered: false }, () => t.label) : row.type
    },
  },
  { title: '题目内容', key: 'content', ellipsis: { tooltip: true } },
  { title: '答案', key: 'answer', width: 100, ellipsis: { tooltip: true } },
  {
    title: '难度',
    key: 'difficulty',
    width: 80,
    render(row) {
      const d = difficultyMap[row.difficulty]
      return d ? h(NTag, { type: d.type, size: 'small', bordered: false }, () => d.label) : row.difficulty
    },
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small', bordered: false }, () => row.status === 1 ? '启用' : '禁用')
    },
  },
  {
    title: '操作',
    key: 'actions',
    width: 180,
    render(row) {
      return h(NSpace, { size: 4 }, () => [
        h(NButton, { text: true, type: 'primary', onClick: () => openDrawer(row) }, () => '编辑'),
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '删除'),
          default: () => '确定删除该题目吗？',
        }),
        h(NButton, { text: true, type: 'warning', onClick: () => handleAiSingle(row.id) }, () => 'AI解析'),
      ])
    },
  },
]

async function openDrawer(row?: Question) {
  resetForm()
  multiAnswer.value = []
  if (row) {
    editingId.value = row.id
    try {
      const detail = (await questionApi.getDetail(row.id)) as any
      formValue.value = {
        bankId: detail.bankId,
        type: detail.type,
        content: detail.content,
        answer: detail.answer,
        analysis: detail.analysis || '',
        difficulty: detail.difficulty,
        sort: detail.sort,
      }
      if (detail.options && detail.options.length > 0) {
        formOptions.value = detail.options.map((opt: any) => ({ label: opt.label, content: opt.content }))
      } else {
        initOptions(detail.type)
      }
      if (detail.type === 2 && detail.answer) {
        multiAnswer.value = detail.answer.split(',').map((s: string) => s.trim())
      }
    } catch {
      message.error('获取题目详情失败')
      return
    }
  } else {
    editingId.value = null
    initOptions(1)
  }
  showDrawer.value = true
}

async function handleSubmit() {
  if (!(await validate())) return
  submitLoading.value = true
  try {
    const payload: Record<string, any> = { ...formValue.value }
    if (formValue.value.type === 2) {
      payload.answer = multiAnswer.value.sort().join(',')
    }
    if (formOptions.value.length > 0) {
      payload.options = formOptions.value
    }
    if (editingId.value) {
      await questionApi.update(editingId.value, payload)
      message.success('更新成功')
    } else {
      await questionApi.create(payload)
      message.success('创建成功')
    }
    showDrawer.value = false
    fetchData(searchParams)
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await questionApi.remove(id)
    message.success('删除成功')
    fetchData(searchParams)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

function handleBatchDelete() {
  dialog.warning({
    title: '批量删除',
    content: `确定删除选中的 ${checkedKeys.value.length} 道题目吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await questionApi.batchDelete(checkedKeys.value)
        message.success('删除成功')
        checkedKeys.value = []
        fetchData(searchParams)
      } catch (e: any) {
        message.error(e.message || '删除失败')
      }
    },
  })
}

async function handleImport() {
  if (!importBankId.value) { message.warning('请选择题库'); return }
  if (!importFile.value) { message.warning('请选择文件'); return }
  importLoading.value = true
  try {
    await questionApi.importExcel(importBankId.value, importFile.value)
    message.success('导入成功')
    showImportModal.value = false
    fetchData(searchParams)
  } catch (e: any) {
    message.error(e.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

async function handleDownloadTemplate() {
  try {
    const res = await questionApi.downloadTemplate() as any
    const blob = new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '题目导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    message.error('下载模板失败')
  }
}

async function handleAiSingle(questionId: number) {
  try {
    await aiApi.generate({ questionId, mode: 'GENERATE_ANALYSIS' })
    message.success('AI解析生成任务已提交')
  } catch (e: any) {
    message.error(e.message || '生成失败')
  }
}

async function handleAiGenerate() {
  aiLoading.value = true
  try {
    await aiApi.batchGenerate({ mode: aiMode.value, bankId: aiBankId.value })
    message.success('AI批量生成任务已提交')
    showAiModal.value = false
  } catch (e: any) {
    message.error(e.message || '生成失败')
  } finally {
    aiLoading.value = false
  }
}

function handleSearch() { pagination.page = 1; fetchData(searchParams) }
function handleReset() {
  searchParams.bankId = null
  searchParams.type = null
  searchParams.keyword = ''
  pagination.page = 1
  fetchData()
}

async function loadBanks() {
  const res = (await bankApi.getList({ pageNum: 1, pageSize: 999 })) as any
  bankOptions.value = (res?.list || []).map((b: any) => ({ label: b.name, value: b.id }))
}

onMounted(() => {
  loadBanks()
  fetchData()
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

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.option-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
