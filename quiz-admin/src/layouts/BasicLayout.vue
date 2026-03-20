<template>
  <n-layout has-sider style="height: 100vh">
    <n-layout-sider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="240"
      :collapsed="collapsed"
      show-trigger
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <div class="logo" :class="{ 'logo-collapsed': collapsed }">
        <img v-if="siteLogo" class="logo-img" :src="siteLogo" alt="logo" />
        <span v-if="!collapsed">{{ siteName }}</span>
      </div>
      <n-menu
        :collapsed="collapsed"
        :collapsed-width="64"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="activeKey"
        accordion
        @update:value="handleMenuSelect"
      />
    </n-layout-sider>
    <n-layout>
      <n-layout-header bordered class="header">
        <div class="header-left">
          <span class="header-name">{{ siteName }}</span>
          <span v-if="siteDescription" class="header-desc">{{ siteDescription }}</span>
        </div>
        <div class="header-right">
          <n-dropdown :options="userMenuOptions" trigger="click" @select="handleUserMenuSelect">
            <div class="user-info">
              <n-avatar :src="adminInfo?.avatar" :size="32" round>
                <template #fallback>
                  <n-icon size="20"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4"/></svg></n-icon>
                </template>
              </n-avatar>
              <div class="user-detail">
                <span class="user-name">{{ adminInfo?.nickname || adminInfo?.username || '管理员' }}</span>
                <span class="user-role">{{ roleText }}</span>
              </div>
              <n-icon size="16" color="#999"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6l-6-6z"/></svg></n-icon>
            </div>
          </n-dropdown>
        </div>
      </n-layout-header>
      <n-layout-content class="main-content">
        <router-view />
      </n-layout-content>
    </n-layout>

    <!-- 个人信息弹窗 -->
    <n-modal v-model:show="showProfileModal" preset="card" title="个人信息" style="width: 500px;">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="头像">
          <div class="avatar-upload">
            <n-avatar :src="profileForm.avatar || adminInfo?.avatar" :size="64" round>
              <template #fallback>
                <n-icon size="32"><svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4"/></svg></n-icon>
              </template>
            </n-avatar>
            <n-upload
              :show-file-list="false"
              accept="image/*"
              :custom-request="handleAvatarUpload"
            >
              <n-button size="small">更换头像</n-button>
            </n-upload>
          </div>
        </n-form-item>
        <n-form-item label="用户名">
          <n-input :value="adminInfo?.username" disabled />
        </n-form-item>
        <n-form-item label="昵称">
          <n-input v-model:value="profileForm.nickname" placeholder="请输入昵称" />
        </n-form-item>
        <n-form-item label="角色">
          <n-tag :type="adminInfo?.role === 'super_admin' ? 'error' : 'info'" size="small">{{ roleText }}</n-tag>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showProfileModal = false">取消</n-button>
          <n-button type="primary" :loading="profileLoading" @click="handleSaveProfile">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 修改密码弹窗 -->
    <n-modal v-model:show="showPasswordModal" preset="card" title="修改密码" style="width: 450px;">
      <n-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-placement="left" label-width="80">
        <n-form-item label="原密码" path="oldPassword">
          <n-input v-model:value="passwordForm.oldPassword" type="password" show-password-on="click" placeholder="请输入原密码" />
        </n-form-item>
        <n-form-item label="新密码" path="newPassword">
          <n-input v-model:value="passwordForm.newPassword" type="password" show-password-on="click" placeholder="请输入新密码" />
        </n-form-item>
        <n-form-item label="确认密码" path="confirmPassword">
          <n-input v-model:value="passwordForm.confirmPassword" type="password" show-password-on="click" placeholder="请再次输入新密码" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPasswordModal = false">取消</n-button>
          <n-button type="primary" :loading="passwordLoading" @click="handleChangePassword">确认修改</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-layout>
</template>

<script setup lang="ts">
import {computed, h, onMounted, ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  type DropdownOption,
  type FormInst,
  type FormRules,
  NIcon,
  type UploadCustomRequestOptions,
  useMessage
} from 'naive-ui'
import {useAuthStore} from '@/stores/auth'
import {useAppStore} from '@/stores/app'
import {changePassword} from '@/api/auth'
import {updateAdmin} from '@/api/admin'
import {uploadImage} from '@/api/upload'
import {
  BookOutline,
  BuildOutline,
  CloseCircleOutline,
  CreateOutline,
  DiamondOutline,
  DocumentOutline,
  DocumentTextOutline,
  FolderOutline,
  HardwareChipOutline,
  HeartOutline,
  HomeOutline,
  ListOutline,
  PeopleOutline,
  SettingsOutline,
  StatsChartOutline,
  SwapHorizontalOutline,
  WalletOutline,
} from '@vicons/ionicons5'

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const message = useMessage()

const siteName = computed(() => appStore.siteName)
const siteLogo = computed(() => appStore.siteLogo)
const siteDescription = computed(() => appStore.siteDescription)
const adminInfo = computed(() => authStore.adminInfo)
const roleText = computed(() => adminInfo.value?.role === 'super_admin' ? '超级管理员' : '普通管理员')

const activeKey = computed(() => route.path)

// 个人信息弹窗
const showProfileModal = ref(false)
const profileLoading = ref(false)
const profileForm = ref({ nickname: '', avatar: '' })

// 修改密码弹窗
const showPasswordModal = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInst | null>(null)
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordRules: FormRules = {
  oldPassword: { required: true, message: '请输入原密码', trigger: 'blur' },
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_rule, value) => value === passwordForm.value.newPassword, message: '两次密码不一致', trigger: 'blur' }
  ]
}

// 用户下拉菜单
const userMenuOptions: DropdownOption[] = [
  { label: '个人信息', key: 'profile', icon: () => h(NIcon, null, { default: () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', innerHTML: '<path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4s-4 1.79-4 4s1.79 4 4 4m0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4"/>' }) }) },
  { label: '修改密码', key: 'password', icon: () => h(NIcon, null, { default: () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', innerHTML: '<path fill="currentColor" d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2m-6 9c-1.1 0-2-.9-2-2s.9-2 2-2s2 .9 2 2s-.9 2-2 2M9 8V6c0-1.66 1.34-3 3-3s3 1.34 3 3v2z"/>' }) }) },
  { type: 'divider' },
  { label: '退出登录', key: 'logout', icon: () => h(NIcon, null, { default: () => h('svg', { xmlns: 'http://www.w3.org/2000/svg', viewBox: '0 0 24 24', innerHTML: '<path fill="currentColor" d="m17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4z"/>' }) }) },
]

function handleUserMenuSelect(key: string) {
  if (key === 'profile') {
    profileForm.value = { 
      nickname: adminInfo.value?.nickname || '', 
      avatar: adminInfo.value?.avatar || '' 
    }
    showProfileModal.value = true
  } else if (key === 'password') {
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showPasswordModal.value = true
  } else if (key === 'logout') {
    authStore.logout()
  }
}

async function handleAvatarUpload({ file, onFinish, onError }: UploadCustomRequestOptions) {
  try {
    profileForm.value.avatar = await uploadImage(file.file as File) as unknown as string
    message.success('头像上传成功')
    onFinish()
  } catch (e) {
    message.error('上传失败')
    onError()
  }
}

async function handleSaveProfile() {
  if (!adminInfo.value?.id) return
  profileLoading.value = true
  try {
    await updateAdmin(adminInfo.value.id, { 
      nickname: profileForm.value.nickname, 
      avatar: profileForm.value.avatar 
    })
    // 更新本地信息
    authStore.setAdminInfo({
      ...authStore.adminInfo!,
      nickname: profileForm.value.nickname,
      avatar: profileForm.value.avatar
    })
    message.success('保存成功')
    showProfileModal.value = false
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    profileLoading.value = false
  }
}

async function handleChangePassword() {
  try {
    await passwordFormRef.value?.validate()
  } catch {
    return
  }
  passwordLoading.value = true
  try {
    await changePassword({ oldPassword: passwordForm.value.oldPassword, newPassword: passwordForm.value.newPassword })
    message.success('密码修改成功，请重新登录')
    showPasswordModal.value = false
    authStore.logout()
  } catch (e: any) {
    message.error(e.message || '修改失败')
  } finally {
    passwordLoading.value = false
  }
}

function renderIcon(icon: any) {
  return () => h(NIcon, null, { default: () => h(icon) })
}

const menuOptions = [
  { label: '仪表盘', key: '/dashboard', icon: renderIcon(HomeOutline) },
  {
    label: '题库管理',
    key: 'question-group',
    icon: renderIcon(FolderOutline),
    children: [
      { label: '分类管理', key: '/category', icon: renderIcon(FolderOutline) },
      { label: '题库列表', key: '/bank', icon: renderIcon(BookOutline) },
      { label: '题目管理', key: '/question', icon: renderIcon(DocumentTextOutline) },
      { label: '题目转换', key: '/bank/convert', icon: renderIcon(SwapHorizontalOutline) },
    ],
  },
  {
    label: '用户管理',
    key: 'user-group',
    icon: renderIcon(PeopleOutline),
    children: [
      { label: '用户列表', key: '/user', icon: renderIcon(PeopleOutline) },
    ],
  },
  {
    label: '记录管理',
    key: 'record-group',
    icon: renderIcon(CreateOutline),
    children: [
      { label: '练习记录', key: '/record/practice', icon: renderIcon(CreateOutline) },
      { label: '考试记录', key: '/record/exam', icon: renderIcon(ListOutline) },
    ],
  },
  {
    label: '数据中心',
    key: 'data-group',
    icon: renderIcon(StatsChartOutline),
    children: [
      { label: '收藏管理', key: '/favorite', icon: renderIcon(HeartOutline) },
      { label: '错题管理', key: '/wrong', icon: renderIcon(CloseCircleOutline) },
      { label: '数据统计', key: '/statistics', icon: renderIcon(StatsChartOutline) },
    ],
  },
  {
    label: 'VIP管理',
    key: 'vip-group',
    icon: renderIcon(DiamondOutline),
    children: [
      { label: '套餐配置', key: '/vip/plan', icon: renderIcon(DiamondOutline) },
      { label: '订单列表', key: '/vip/order', icon: renderIcon(WalletOutline) },
    ],
  },
  {
    label: 'AI配置',
    key: 'ai-group',
    icon: renderIcon(HardwareChipOutline),
    children: [
      { label: '模型配置', key: '/ai/config', icon: renderIcon(HardwareChipOutline) },
      { label: '调用日志', key: '/ai/log', icon: renderIcon(DocumentOutline) },
    ],
  },
  {
    label: '系统管理',
    key: 'system-group',
    icon: renderIcon(SettingsOutline),
    children: [
      { label: '管理员管理', key: '/system/admin', icon: renderIcon(BuildOutline) },
      { label: '系统设置', key: '/system/setting', icon: renderIcon(SettingsOutline) },
    ],
  },
]

function handleMenuSelect(key: string) {
  router.push(key)
}

onMounted(async () => {
  try {
    await appStore.loadSiteConfig()
  } catch {}
  
  // 如果 adminInfo 为空但有 token，尝试获取用户信息
  if (!authStore.adminInfo && authStore.token) {
    await authStore.fetchInfo()
  }
})
</script>

<style scoped>
.header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-name {
  font-weight: 600;
  font-size: 16px;
}

.header-desc {
  font-size: 12px;
  color: #94a3b8;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f5f5;
}

.user-detail {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.user-role {
  font-size: 12px;
  color: #999;
}

.main-content {
  padding: 12px;
  background: #f5f7f9;
  min-height: calc(100vh - 56px);
  overflow: auto;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  gap: 8px;
  padding: 0 12px;
}

.logo-collapsed {
  font-size: 22px;
}

.logo-img {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  object-fit: contain;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}
</style>
