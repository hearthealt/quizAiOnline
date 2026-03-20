<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">管理员管理</span>
          <n-button type="primary" @click="openForm()">
            <template #icon><n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon></template>
            新增管理员
          </n-button>
        </div>
      </template>

      <n-data-table size="small" :columns="columns" :data="tableData" :loading="loading" :row-key="(row: any) => row.id" striped />

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

    <n-modal v-model:show="showForm" preset="card" :title="formData.id ? '编辑管理员' : '新增管理员'" style="width: 480px">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="80">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="formData.username" placeholder="请输入用户名" :disabled="!!formData.id" />
        </n-form-item>
        <n-form-item v-if="!formData.id" label="密码" path="password">
          <n-input v-model:value="formData.password" type="password" show-password-on="click" placeholder="请输入密码" />
        </n-form-item>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="formData.nickname" placeholder="请输入昵称" />
        </n-form-item>
        <n-form-item label="角色" path="role">
          <n-radio-group v-model:value="formData.role">
            <n-radio-button value="admin">管理员</n-radio-button>
            <n-radio-button value="super_admin">超级管理员</n-radio-button>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showForm = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NButton, NTag, NSpace, NPopconfirm, NAvatar, useMessage } from 'naive-ui'
import { getAdminList, createAdmin, updateAdmin, deleteAdmin } from '@/api/admin'
import dayjs from 'dayjs'
import type { DataTableColumns, FormInst } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInst>()

const query = reactive({ pageNum: 1, pageSize: 10 })

const roleMap: Record<string, { text: string; type: 'info' | 'warning' }> = {
  admin: { text: '管理员', type: 'info' },
  super_admin: { text: '超级管理员', type: 'warning' }
}

const defaultForm = () => ({
  id: null as number | null,
  username: '',
  password: '',
  nickname: '',
  role: 'admin'
})

const formData = ref(defaultForm())

const rules: Record<string, any> = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  password: { required: true, message: '请输入密码', trigger: 'blur' },
  role: { required: true, message: '请选择角色', trigger: 'change' }
}

const columns: DataTableColumns = [
  { title: 'ID', key: 'id', width: 60 },
  {
    title: '管理员',
    key: 'username',
    width: 200,
    render(row: any) {
      return h('div', { style: 'display:flex;align-items:center;gap:10px' }, [
        h(NAvatar, { src: row.avatar || undefined, size: 36, round: true, style: 'background:#667eea;color:#fff' }, { fallback: () => (row.nickname || row.username || '').charAt(0).toUpperCase() }),
        h('div', null, [
          h('div', { style: 'font-weight:500' }, row.nickname || row.username),
          h('div', { style: 'font-size:12px;color:#999' }, row.username),
        ]),
      ])
    }
  },
  {
    title: '角色',
    key: 'role',
    width: 120,
    render(row: any) {
      const item = roleMap[row.role] || { text: row.role, type: 'info' }
      return h(NTag, { type: item.type, size: 'small', bordered: false }, () => item.text)
    }
  },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row: any) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'default', size: 'small', bordered: false }, () => row.status === 1 ? '正常' : '禁用')
    }
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 150,
    render: (row: any) => row.createTime ? dayjs(row.createTime).format('YYYY-MM-DD HH:mm') : '-'
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    render(row: any) {
      return h(NSpace, { size: 4 }, () => [
        h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => openForm(row) }, () => '编辑'),
        h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
          trigger: () => h(NButton, { size: 'small', type: 'error', text: true }, () => '删除'),
          default: () => '确定删除该管理员？'
        })
      ])
    }
  }
]

function openForm(row?: any) {
  formData.value = row ? { ...row, password: '' } : defaultForm()
  showForm.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (formData.value.id) {
      const { password, ...rest } = formData.value
      await updateAdmin(formData.value.id, rest)
      message.success('更新成功')
    } else {
      await createAdmin(formData.value)
      message.success('创建成功')
    }
    showForm.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await deleteAdmin(id)
  message.success('删除成功')
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminList(query) as any
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

onMounted(() => fetchData())
</script>

<style scoped>
.page-container {
  min-height: 100%;
}

.main-card {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
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
