<template>
  <PageContainer title="分类管理">
    <template #header-actions>
      <n-space>
        <n-button v-if="isSuperAdmin" :disabled="checkedKeys.length === 0" @click="handleBatchStatus(1)">
          批量启用 {{ checkedKeys.length ? `(${checkedKeys.length})` : '' }}
        </n-button>
        <n-button v-if="isSuperAdmin" :disabled="checkedKeys.length === 0" @click="handleBatchStatus(0)">
          批量禁用 {{ checkedKeys.length ? `(${checkedKeys.length})` : '' }}
        </n-button>
        <n-popconfirm v-if="isSuperAdmin" @positive-click="handleBatchDelete">
          <template #trigger>
            <n-button type="error" :disabled="checkedKeys.length === 0">
              批量删除 {{ checkedKeys.length ? `(${checkedKeys.length})` : '' }}
            </n-button>
          </template>
          确定删除选中的 {{ checkedKeys.length }} 个分类吗？
        </n-popconfirm>
        <n-button type="primary" @click="openModal()">
          <template #icon><n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon></template>
          新增分类
        </n-button>
      </n-space>
    </template>

    <DataTableSection>
      <AdminDataTable
        size="small"
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="false"
        :row-key="(row: Category) => row.id"
        :checked-row-keys="checkedKeys"
        @update:checked-row-keys="(keys: any) => (checkedKeys = keys)"
        striped
      />
      <template #pagination>
        <n-pagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="pagination.pageSizes"
          :show-size-picker="pagination.showSizePicker"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </template>
    </DataTableSection>

    <n-modal v-model:show="showModal" preset="card" :title="editingId ? '编辑分类' : '新增分类'" style="width: 480px">
      <n-form ref="formRef" :model="formValue" :rules="formRules" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name">
          <n-input v-model:value="formValue.name" placeholder="请输入分类名称" />
        </n-form-item>
        <n-form-item label="图标" path="icon">
          <div class="icon-upload">
            <n-upload
              :max="1"
              accept="image/*"
              :default-upload="false"
              :show-file-list="false"
              @change="(opt: any) => handleIconUpload(opt.file?.file)"
            >
              <div class="upload-trigger" :class="{ 'has-image': formValue.icon }">
                <n-image v-if="formValue.icon" :src="formValue.icon" width="80" height="80" object-fit="contain" preview-disabled />
                <template v-else>
                  <n-icon size="24" color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon>
                  <span>上传图标</span>
                </template>
              </div>
            </n-upload>
            <n-button v-if="formValue.icon" text type="error" size="small" @click="formValue.icon = ''">移除</n-button>
          </div>
        </n-form-item>
        <n-form-item v-if="editingId" label="排序" path="sort">
          <n-input-number v-model:value="formValue.sort" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item v-if="isSuperAdmin" label="状态" path="status">
          <n-switch v-model:value="formValue.status" size="small" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showModal = false">取消</n-button>
          <n-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, h, ref, onMounted } from 'vue'
import { NButton, NSwitch, NPopconfirm, NSpace, NImage, NPagination, NTag, useMessage, type DataTableColumns, type FormRules } from 'naive-ui'
import type { Category } from '@/types'
import * as categoryApi from '@/api/category'
import { uploadImage } from '@/api/upload'
import { useTable } from '@/composables/useTable'
import { useForm } from '@/composables/useForm'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'

const message = useMessage()
const authStore = useAuthStore()
const isSuperAdmin = computed(() => authStore.adminInfo?.role === 'super_admin')
const { loading, data, pagination, fetchData, handlePageChange, handlePageSizeChange } = useTable<Category>(
  (params) => categoryApi.getList(params) as any
)

const showModal = ref(false)
const editingId = ref<number | null>(null)
const submitLoading = ref(false)
const checkedKeys = ref<number[]>([])

const defaultForm = { name: '', icon: '', sort: 0, status: 1 }
const { formValue, formRef, resetForm, validate } = useForm(defaultForm)

const formRules: FormRules = {
  name: { required: true, message: '请输入分类名称', trigger: 'blur' },
}

const columns = computed<DataTableColumns<Category>>(() => [
  ...(isSuperAdmin.value ? [{ type: 'selection' as const }] : []),
  { title: '名称', key: 'name', ellipsis: { tooltip: true } },
  { title: '图标', key: 'icon', width: 80, render(row) { return row.icon ? h('img', { src: row.icon, style: 'width:40px;height:40px;object-fit:contain;border-radius:6px;background:#f5f5f5;padding:4px' }) : h(NTag, { size: 'small', bordered: false }, () => '无') } },
  { title: '排序', key: 'sort', width: 80 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row) {
      if (!isSuperAdmin.value) {
        return h(NTag, { size: 'small', bordered: false, type: row.status === 1 ? 'success' : 'default' }, () => row.status === 1 ? '启用' : '禁用')
      }
      return h(NSwitch, {
        value: row.status,
        checkedValue: 1,
        uncheckedValue: 0,
        size: 'small',
        onUpdateValue: (val: number) => handleToggleStatus(row.id, val),
      })
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 200,
    render(row) { return dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss') },
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render(row) {
      const actions = [
        h(NButton, { text: true, type: 'primary', onClick: () => openModal(row) }, () => '编辑')
      ]
      if (isSuperAdmin.value) {
        actions.push(
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { text: true, type: 'error' }, () => '删除'),
          default: () => '确定删除该分类吗？',
        })
        )
      }
      return h(NSpace, { size: 4 }, () => actions)
    },
  },
])

function openModal(row?: Category) {
  resetForm()
  if (row) {
    editingId.value = row.id
    formValue.value = { name: row.name, icon: row.icon, sort: row.sort, status: row.status }
  } else {
    editingId.value = null
  }
  showModal.value = true
}

async function handleIconUpload(file: File | null) {
  if (!file) return
  try {
    const url = await uploadImage(file) as unknown as string
    formValue.value.icon = url
    message.success('上传成功')
  } catch {
    message.error('上传失败')
  }
}

async function handleSubmit() {
  if (!(await validate())) return
  submitLoading.value = true
  try {
    const payload: Record<string, any> = { ...formValue.value }
    if (!isSuperAdmin.value) {
      delete payload.status
    }
    if (editingId.value) {
      await categoryApi.update(editingId.value, payload)
      message.success('更新成功')
    } else {
      await categoryApi.create(payload)
      message.success('创建成功')
    }
    showModal.value = false
    fetchData()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleToggleStatus(id: number, status: number) {
  try {
    await categoryApi.toggleStatus(id, status)
    message.success('状态更新成功')
    fetchData()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

async function handleBatchStatus(status: number) {
  try {
    await categoryApi.batchToggleStatus(checkedKeys.value, status)
    message.success(status === 1 ? '批量启用成功' : '批量禁用成功')
    checkedKeys.value = []
    fetchData()
  } catch (e: any) {
    message.error(e.message || '批量操作失败')
    fetchData()
  }
}

async function handleBatchDelete() {
  try {
    await categoryApi.batchDelete(checkedKeys.value)
    message.success('批量删除成功')
    checkedKeys.value = []
    fetchData()
  } catch (e: any) {
    message.error(e.message || '批量删除失败')
    fetchData()
  }
}

async function handleDelete(id: number) {
  try {
    await categoryApi.remove(id)
    message.success('删除成功')
    fetchData()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

onMounted(() => fetchData())
</script>

<style scoped>
.icon-upload {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.upload-trigger {
  width: 80px;
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
  border-color: var(--color-primary);
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
