<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">VIP套餐管理</span>
          <n-button type="primary" @click="openForm()">
            <template #icon><n-icon><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z"/></svg></n-icon></template>
            新增套餐
          </n-button>
        </div>
      </template>

      <n-data-table size="small" :columns="columns" :data="tableData" :loading="loading" :row-key="(row: any) => row.id" striped />
    </n-card>

    <n-modal v-model:show="showForm" preset="card" :title="formData.id ? '编辑套餐' : '新增套餐'" style="width: 520px">
      <n-form ref="formRef" :model="formData" :rules="rules" label-placement="left" label-width="80">
        <n-form-item label="套餐名称" path="name">
          <n-input v-model:value="formData.name" placeholder="请输入套餐名称" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="时长" path="duration" label-width="60">
              <n-input-number v-model:value="formData.duration" :min="1" style="width: 100%">
                <template #suffix>天</template>
              </n-input-number>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="排序" path="sort" label-width="60">
              <n-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="价格" path="price" label-width="60">
              <n-input-number v-model:value="formData.price" :min="0" :precision="2" style="width: 100%">
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="原价" path="originalPrice" label-width="60">
              <n-input-number v-model:value="formData.originalPrice" :min="0" :precision="2" style="width: 100%">
                <template #prefix>¥</template>
              </n-input-number>
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" :rows="3" placeholder="请输入描述" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-switch v-model:value="formData.status" :checked-value="1" :unchecked-value="0">
            <template #checked>启用</template>
            <template #unchecked>禁用</template>
          </n-switch>
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
import { ref, onMounted, h } from 'vue'
import { NButton, NSwitch, NSpace, NPopconfirm, NTag, useMessage } from 'naive-ui'
import { getPlanList, createPlan, updatePlan, deletePlan, togglePlanStatus } from '@/api/vip'
import type { DataTableColumns, FormInst } from 'naive-ui'

const message = useMessage()
const loading = ref(false)
const tableData = ref<any[]>([])
const showForm = ref(false)
const submitting = ref(false)
const formRef = ref<FormInst>()

const defaultForm = () => ({
  id: null as number | null,
  name: '',
  duration: 30,
  price: 0,
  originalPrice: 0,
  description: '',
  sort: 0,
  status: 1
})

const formData = ref(defaultForm())

const rules = {
  name: { required: true, message: '请输入套餐名称', trigger: 'blur' },
  duration: { required: true, type: 'number' as const, message: '请输入时长', trigger: 'blur' },
  price: { required: true, type: 'number' as const, message: '请输入价格', trigger: 'blur' }
}

const columns: DataTableColumns = [
  { title: '套餐名称', key: 'name', width: 120 },
  { title: '时长', key: 'duration', width: 80, render: (row: any) => `${row.duration}天` },
  {
    title: '价格',
    key: 'price',
    width: 180,
    render: (row: any) => h('div', null, [
      h('span', { style: 'font-size:16px;font-weight:600;color:#667eea' }, `¥${row.price}`),
      row.originalPrice > row.price ? h('span', { style: 'font-size:12px;color:#999;text-decoration:line-through;margin-left:8px' }, `¥${row.originalPrice}`) : null,
    ])
  },
  { title: '描述', key: 'description', ellipsis: { tooltip: true } },
  { title: '排序', key: 'sort', width: 60 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render(row: any) {
      return h(NSwitch, {
        value: row.status,
        checkedValue: 1,
        uncheckedValue: 0,
        onUpdateValue: async (val: number) => {
          await togglePlanStatus(row.id, val)
          row.status = val
        }
      })
    }
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
          default: () => '确定删除该套餐？'
        })
      ])
    }
  }
]

function openForm(row?: any) {
  formData.value = row ? { ...row } : defaultForm()
  showForm.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()
  submitting.value = true
  try {
    if (formData.value.id) {
      await updatePlan(formData.value.id, formData.value)
      message.success('更新成功')
    } else {
      await createPlan(formData.value)
      message.success('创建成功')
    }
    showForm.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await deletePlan(id)
  message.success('删除成功')
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPlanList() as any
    tableData.value = res?.list || (Array.isArray(res) ? res : [])
  } finally {
    loading.value = false
  }
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
</style>
