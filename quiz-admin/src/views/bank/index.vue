<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">题库管理</span>
          <n-button type="primary" @click="openDrawer()">
            <template #icon><n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon></template>
            新增题库
          </n-button>
        </div>
      </template>

      <n-space class="search-bar">
        <n-select
          v-model:value="searchParams.categoryId"
          placeholder="选择分类"
          :options="categoryOptions"
          clearable
          style="width: 180px"
        />
        <n-input v-model:value="searchParams.keyword" placeholder="搜索题库名称" clearable style="width: 200px" @keyup.enter="handleSearch">
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
        :row-key="(row: QuestionBank) => row.id"
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

    <!-- 侧边表单 -->
    <n-drawer v-model:show="showDrawer" :width="520">
      <n-drawer-content :title="editingId ? '编辑题库' : '新增题库'" closable>
        <n-form ref="formRef" :model="formValue" :rules="formRules" label-placement="left" label-width="100">
          <n-form-item label="所属分类" path="categoryId">
            <n-select v-model:value="formValue.categoryId" :options="categoryOptions" placeholder="请选择分类" />
          </n-form-item>
          <n-form-item label="名称" path="name">
            <n-input v-model:value="formValue.name" placeholder="请输入题库名称" />
          </n-form-item>
          <n-form-item label="描述" path="description">
            <n-input v-model:value="formValue.description" type="textarea" :rows="3" placeholder="请输入描述" />
          </n-form-item>
          <n-form-item label="封面" path="cover">
            <div class="cover-upload">
              <n-upload
                :max="1"
                accept="image/*"
                :default-upload="false"
                :show-file-list="false"
                @change="(opt: any) => handleCoverUpload(opt.file?.file)"
              >
                <div class="upload-trigger" :class="{ 'has-image': formValue.cover }">
                  <n-image v-if="formValue.cover" :src="formValue.cover" width="120" height="80" object-fit="cover" preview-disabled />
                  <template v-else>
                    <n-icon size="24" color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon>
                    <span>上传封面</span>
                  </template>
                </div>
              </n-upload>
              <n-button v-if="formValue.cover" text type="error" size="small" @click="formValue.cover = ''">移除</n-button>
            </div>
          </n-form-item>
        <n-form-item label="及格分" path="passScore">
          <n-input-number v-model:value="formValue.passScore" :min="0" style="width: 100%">
            <template #suffix>分</template>
          </n-input-number>
        </n-form-item>
        <n-grid :cols="2" :x-gap="16">
          <n-gi v-if="editingId">
              <n-form-item label="排序" path="sort" label-width="80">
                <n-input-number v-model:value="formValue.sort" :min="0" style="width: 100%" />
              </n-form-item>
          </n-gi>
        </n-grid>
        </n-form>
        <template #footer>
          <n-space>
            <n-button @click="showDrawer = false">取消</n-button>
            <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { h, ref, reactive, onMounted } from 'vue'
import { NButton, NPopconfirm, NSpace, NTag, NImage, NPagination, useMessage, type DataTableColumns, type FormRules } from 'naive-ui'
import type { QuestionBank } from '@/types'
import * as bankApi from '@/api/bank'
import * as categoryApi from '@/api/category'
import { uploadImage } from '@/api/upload'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'

const message = useMessage()

const searchParams = reactive({ categoryId: null as number | null, keyword: '' })
const categoryOptions = ref<{ label: string; value: number }[]>([])

const { loading, data, pagination, fetchData, handlePageChange, handlePageSizeChange } = useTable<QuestionBank>(
  (params) => bankApi.getList({ ...searchParams, ...params } as any) as any
)

const showDrawer = ref(false)
const editingId = ref<number | null>(null)
const submitLoading = ref(false)

const defaultForm = { categoryId: null as number | null, name: '', description: '', cover: '', passScore: 60, sort: 0 }
const { formValue, formRef, resetForm, validate } = useForm(defaultForm)

const formRules: FormRules = {
  categoryId: { required: true, type: 'number', message: '请选择分类', trigger: 'change' },
  name: { required: true, message: '请输入名称', trigger: 'blur' },
}

const columns: DataTableColumns<QuestionBank> = [
  {
    title: '分类',
    key: 'categoryId',
    width: 120,
    render(row) {
      const opt = categoryOptions.value.find((c) => c.value === row.categoryId)
      return h(NTag, { size: 'small', bordered: false, type: 'info' }, () => opt?.label || '-')
    },
  },
  { title: '名称', key: 'name', width: 460, ellipsis: { tooltip: true } },
  {
    title: '封面',
    key: 'cover',
    width: 80,
    render(row) { return row.cover ? h('img', { src: row.cover, style: 'width:50px;height:36px;object-fit:cover;border-radius:4px' }) : h(NTag, { size: 'small', bordered: false }, () => '无') }
  },
  { title: '描述', key: 'description', ellipsis: { tooltip: true } },
  { title: '题目数', key: 'questionCount', width: 80, render(row) { return h('span', { style: 'font-weight:500;color:#667eea' }, row.questionCount || 0) } },
  { title: '及格分', key: 'passScore', width: 80 },
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
    width: 120,
    render(row) {
      return h(NSpace, { size: 4 }, () => [
        h(NButton, { text: true, type: 'primary', onClick: () => openDrawer(row) }, () => '编辑'),
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '删除'),
          default: () => '确定删除该题库吗？',
        }),
      ])
    },
  },
]

function openDrawer(row?: QuestionBank) {
  resetForm()
  if (row) {
    editingId.value = row.id
    formValue.value = {
      categoryId: row.categoryId,
      name: row.name,
      description: row.description,
      cover: row.cover,
      passScore: row.passScore,
      sort: row.sort,
    }
  } else {
    editingId.value = null
  }
  showDrawer.value = true
}

async function handleCoverUpload(file: File | null) {
  if (!file) return
  try {
    const url = await uploadImage(file) as unknown as string
    formValue.value.cover = url
    message.success('上传成功')
  } catch {
    message.error('上传失败')
  }
}

async function handleSubmit() {
  if (!(await validate())) return
  submitLoading.value = true
  try {
    if (editingId.value) {
      await bankApi.update(editingId.value, formValue.value)
      message.success('更新成功')
    } else {
      await bankApi.create(formValue.value)
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
    await bankApi.remove(id)
    message.success('删除成功')
    fetchData(searchParams)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

function handleSearch() { pagination.page = 1; fetchData(searchParams) }
function handleReset() {
  searchParams.categoryId = null
  searchParams.keyword = ''
  pagination.page = 1
  fetchData(searchParams)
}

async function loadCategories() {
  const res = (await categoryApi.getAll()) as any
  categoryOptions.value = (res || []).map((c: any) => ({ label: c.name, value: c.id }))
}

onMounted(() => {
  loadCategories()
  fetchData(searchParams)
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

.cover-upload {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.upload-trigger {
  width: 120px;
  height: 80px;
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  cursor: pointer;
  transition: border-color 0.2s;
  background: #fafafa;
  overflow: hidden;
}

.upload-trigger:hover {
  border-color: #667eea;
}

.upload-trigger.has-image {
  border-style: solid;
  background: #fff;
}

.upload-trigger span {
  font-size: 12px;
  color: #999;
}
</style>
