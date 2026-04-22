<template>
  <div class="page-container">
    <n-card :bordered="false" size="small" class="main-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">用户管理</span>
          <n-text depth="3">共 {{ pagination.itemCount }} 位用户</n-text>
        </div>
      </template>

      <n-space class="search-bar">
        <n-input v-model:value="searchParams.keyword" placeholder="搜索昵称/手机号" clearable style="width: 200px" @keyup.enter="handleSearch">
          <template #prefix><n-icon color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5A6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5S14 7.01 14 9.5S11.99 14 9.5 14"/></svg></n-icon></template>
        </n-input>
        <n-select
          v-model:value="searchParams.status"
          placeholder="用户状态"
          :options="[{ label: '正常', value: 1 }, { label: '禁用', value: 0 }]"
          clearable
          style="width: 140px"
        />
        <n-button type="primary" @click="handleSearch">搜索</n-button>
        <n-button @click="handleReset">重置</n-button>
      </n-space>

      <n-data-table
        :columns="columns"
        :data="data"
        :loading="loading"
        :pagination="false"
        :row-key="(row: User) => row.id"
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

    <!-- 用户详情抽屉 -->
    <n-drawer v-model:show="showDrawer" :width="550">
      <n-drawer-content title="用户详情" closable>
        <template v-if="detailUser">
          <div class="user-profile">
            <n-avatar :src="resolveAssetUrl(detailUser.avatar)" :size="72" round />
            <div class="profile-info">
              <div class="profile-name">
                {{ detailUser.nickname }}
                <n-tag v-if="detailUser.isVip === 1" type="warning" size="small" round>VIP</n-tag>
              </div>
              <div class="profile-phone">{{ detailUser.phone }}</div>
              <div class="vip-actions" v-if="detailUser.id">
                <n-space>
                  <n-button v-if="detailUser.isVip !== 1" type="success" size="small" @click="openVipModal(detailUser, 'open')">
                    开通 VIP
                  </n-button>
                  <template v-else>
                    <n-button type="warning" size="small" @click="openVipModal(detailUser, 'renew')">
                      续费 VIP
                    </n-button>
                    <n-button type="error" size="small" ghost @click="cancelVip(detailUser)">
                      取消 VIP
                    </n-button>
                  </template>
                </n-space>
              </div>
            </div>
          </div>

          <n-descriptions :column="2" label-placement="left" bordered class="detail-desc">
            <n-descriptions-item label="用户ID">{{ detailUser.id }}</n-descriptions-item>
            <n-descriptions-item label="状态">
              <n-tag :type="detailUser.status === 1 ? 'success' : 'error'" size="small" bordered>
                {{ detailUser.status === 1 ? '正常' : '禁用' }}
              </n-tag>
            </n-descriptions-item>
            <n-descriptions-item v-if="detailUser.isVip === 1" label="VIP到期">
              {{ isPermVip(detailUser.vipExpireTime) ? '永久有效' : dayjs(detailUser.vipExpireTime).format('YYYY-MM-DD HH:mm') }}
            </n-descriptions-item>
            <n-descriptions-item label="最后登录">
              {{ detailUser.lastLoginTime ? dayjs(detailUser.lastLoginTime).format('YYYY-MM-DD HH:mm') : '-' }}
            </n-descriptions-item>
            <n-descriptions-item label="注册时间">
              {{ dayjs(detailUser.createTime).format('YYYY-MM-DD HH:mm') }}
            </n-descriptions-item>
          </n-descriptions>

          <div class="section-title">最近答题记录</div>
          <n-data-table
            :columns="recordColumns"
            :data="records"
            :loading="recordLoading"
            size="small"
            :pagination="{ pageSize: 5 }"
          />
          
          <div class="section-title">AI对话记录</div>
          <div v-if="chatLoading" style="text-align:center;padding:20px">
            <n-spin size="small" />
          </div>
          <div v-else-if="chatHistory.length" class="chat-summary">
            <div class="chat-stat">
              <span class="stat-num">{{ chatHistory.length }}</span>
              <span class="stat-label">条对话</span>
            </div>
            <n-button type="primary" size="small" @click="showChatModal = true">
              查看对话记录
            </n-button>
          </div>
          <n-empty v-else description="暂无对话记录" size="small" />
        </template>
      </n-drawer-content>
    </n-drawer>
    
    <!-- AI 对话记录弹窗 -->
    <n-modal v-model:show="showChatModal" preset="card" title="AI 对话记录" style="width: 600px; max-width: 90vw;">
      <div class="chat-modal-content">
        <div v-for="msg in chatHistory" :key="msg.id" :class="['chat-bubble-wrap', msg.role, { deleted: msg.deleted === 1 }]">
          <div class="chat-bubble">
            <div class="bubble-content">{{ msg.content }}</div>
            <div class="bubble-meta">
              <span class="bubble-time">{{ dayjs(msg.createTime).format('YYYY-MM-DD HH:mm') }}</span>
              <span v-if="msg.deleted === 1" class="deleted-tag">用户已清理</span>
            </div>
          </div>
        </div>
      </div>
    </n-modal>

    <!-- VIP 设置弹窗 -->
    <n-modal v-model:show="showVipModal" preset="card" :title="vipAction === 'open' ? '开通 VIP' : '续费 VIP'" style="width: 450px;">
      <n-form label-placement="left" label-width="90px">
        <n-form-item label="会员时长">
          <n-radio-group v-model:value="vipForm.days">
            <n-space>
              <n-radio :value="30">30天</n-radio>
              <n-radio :value="90">90天</n-radio>
              <n-radio :value="180">180天</n-radio>
              <n-radio :value="365">365天</n-radio>
              <n-radio :value="-1">永久</n-radio>
            </n-space>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="到期时间">
          <n-text v-if="vipForm.days === -1" type="success">永久有效</n-text>
          <n-text v-else>{{ computedExpireDisplay }}</n-text>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showVipModal = false">取消</n-button>
          <n-button type="primary" @click="confirmSetVip">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { h, ref, reactive, computed, onMounted } from 'vue'
import { NButton, NSwitch, NTag, NAvatar, NSpace, useMessage, useDialog, type DataTableColumns } from 'naive-ui'
import type { User } from '@/types'
import * as userApi from '@/api/user'
import { useTable } from '@/composables/useTable'
import { resolveAssetUrl } from '@/utils/assets'
import dayjs from 'dayjs'

const message = useMessage()
const dialog = useDialog()
const searchParams = reactive({ keyword: '', status: null as number | null })

const { loading, data, pagination, fetchData, handlePageChange, handlePageSizeChange } = useTable<User>(
  (params) => userApi.getList({ ...searchParams, ...params }) as any
)

const showDrawer = ref(false)
const detailUser = ref<User | null>(null)
const records = ref<any[]>([])
const recordLoading = ref(false)
const chatHistory = ref<any[]>([])
const chatLoading = ref(false)
const showChatModal = ref(false)
const showVipModal = ref(false)
const vipAction = ref<'open' | 'renew'>('open')
const vipForm = ref({ days: 30 })
const currentUserId = ref<number>(0)

const columns: DataTableColumns<User> = [
  {
    title: '用户',
    key: 'nickname',
    width: 200,
    render(row) {
      return h('div', { style: 'display:flex;align-items:center;gap:8px' }, [
        h(NAvatar, { src: resolveAssetUrl(row.avatar), size: 36, round: true }),
        h('div', null, [
          h('div', { style: 'font-weight:500' }, row.nickname),
          h('div', { style: 'font-size:12px;color:#999' }, row.phone),
        ]),
      ])
    },
  },
  {
    title: 'VIP状态',
    key: 'isVip',
    width: 100,
    render(row) {
      return h(NTag, { type: row.isVip === 1 ? 'warning' : 'default', size: 'small', bordered: false }, () => row.isVip === 1 ? 'VIP会员' : '普通用户')
    },
  },
  {
    title: '账号状态',
    key: 'status',
    width: 100,
    render(row) {
      return h(NSwitch, {
        value: row.status,
        checkedValue: 1,
        uncheckedValue: 0,
        onUpdateValue: (val: number) => handleToggleStatus(row.id, val),
      })
    },
  },
  {
    title: '注册时间',
    key: 'createTime',
    width: 170,
    render(row) { return dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss') },
  },
  {
    title: '操作',
    key: 'actions',
    width: 100,
    render(row) {
      return h(NButton, { text: true, type: 'primary', onClick: () => openDetail(row) }, () => '查看详情')
    },
  },
]

const recordColumns: DataTableColumns = [
  { title: '题库', key: 'bankName', ellipsis: { tooltip: true } },
  { title: '得分', key: 'score', width: 70, render(row: any) { return row.score ?? '-' } },
  { title: '正确率', key: 'correctRate', width: 80, render(row: any) { return `${row.correctRate || 0}%` } },
  { title: '时间', key: 'createTime', width: 140, render(row: any) { return dayjs(row.createTime).format('MM-DD HH:mm') } },
]

async function handleToggleStatus(id: number, status: number) {
  try {
    await userApi.updateStatus(id, status)
    message.success('状态更新成功')
    fetchData(searchParams)
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

async function openDetail(user: User) {
  detailUser.value = user
  currentUserId.value = user.id
  showDrawer.value = true
  recordLoading.value = true
  chatLoading.value = true
  try {
    const res = (await userApi.getRecords(user.id, { pageNum: 1, pageSize: 10 })) as any
    records.value = res?.list || []
  } catch {
    records.value = []
  } finally {
    recordLoading.value = false
  }
  try {
    chatHistory.value = (await userApi.getChatHistory(user.id)) || []
  } catch {
    chatHistory.value = []
  } finally {
    chatLoading.value = false
  }
}

function isPermVip(expireTime: string | null) {
  return expireTime && dayjs(expireTime).year() >= 2099
}

function openVipModal(user: User, action: 'open' | 'renew') {
  currentUserId.value = user.id
  vipAction.value = action
  vipForm.value = { days: 30 }
  showVipModal.value = true
}

function getExpireTime(): Date {
  if (vipForm.value.days === -1) {
    return dayjs('2099-12-31 23:59:59').toDate()
  }
  let base = dayjs()
  if (vipAction.value === 'renew' && detailUser.value?.vipExpireTime) {
    const expiry = dayjs(detailUser.value.vipExpireTime)
    if (expiry.isAfter(base) && !isPermVip(detailUser.value.vipExpireTime)) {
      base = expiry
    }
  }
  return base.add(vipForm.value.days, 'day').toDate()
}

const computedExpireDisplay = computed(() => {
  return dayjs(getExpireTime()).format('YYYY-MM-DD HH:mm')
})

function cancelVip(user: User) {
  dialog.warning({
    title: '取消 VIP',
    content: `确定取消 ${user.nickname} 的 VIP 会员吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await userApi.setVip(user.id, 0, null)
        message.success('已取消 VIP')
        if (detailUser.value) {
          detailUser.value.isVip = 0
          detailUser.value.vipExpireTime = null as any
        }
        fetchData(searchParams)
      } catch (e: any) {
        message.error(e.message || '操作失败')
      }
    }
  })
}

async function confirmSetVip() {
  try {
    const expireTime = getExpireTime()
    await userApi.setVip(currentUserId.value, 1, expireTime)
    message.success(vipAction.value === 'open' ? 'VIP 开通成功' : 'VIP 续费成功')
    showVipModal.value = false
    if (detailUser.value) {
      detailUser.value.isVip = 1
      detailUser.value.vipExpireTime = dayjs(expireTime).format('YYYY-MM-DD HH:mm:ss') as any
    }
    fetchData(searchParams)
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

function handleSearch() { pagination.page = 1; fetchData(searchParams) }
function handleReset() {
  searchParams.keyword = ''
  searchParams.status = null
  pagination.page = 1
  fetchData(searchParams)
}

onMounted(() => fetchData(searchParams))
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

.user-profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 20px;
}

.profile-info {
  color: #fff;
}

.profile-name {
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-phone {
  font-size: 14px;
  opacity: 0.8;
  margin-top: 4px;
}

.vip-actions {
  margin-top: 12px;
}

.detail-desc {
  margin-bottom: 20px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
  margin-top: 20px;
  padding-left: 8px;
  border-left: 3px solid #667eea;
}

.chat-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.chat-stat {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-num {
  font-size: 24px;
  font-weight: 600;
  color: #667eea;
}

.stat-label {
  font-size: 13px;
  color: #666;
}

.chat-modal-content {
  max-height: 60vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

.chat-modal-content::-webkit-scrollbar {
  display: none; /* Chrome/Safari */
}

.chat-bubble-wrap {
  display: flex;
}

.chat-bubble-wrap.user {
  justify-content: flex-end;
}

.chat-bubble-wrap.assistant {
  justify-content: flex-start;
}

.chat-bubble {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  position: relative;
}

.chat-bubble-wrap.user .chat-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-bubble-wrap.assistant .chat-bubble {
  background: #f0f2f5;
  color: #333;
  border-bottom-left-radius: 4px;
}

.bubble-content {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.bubble-time {
  font-size: 11px;
  opacity: 0.7;
}

.chat-bubble-wrap.user .bubble-meta {
  justify-content: flex-end;
}

.deleted-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: rgba(255, 100, 100, 0.2);
  color: #ff6464;
}

.chat-bubble-wrap.user .deleted-tag {
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.9);
}

.chat-bubble-wrap.deleted .chat-bubble {
  opacity: 0.6;
}
</style>
