<template>
  <div class="admin-shell">
    <aside class="shell-sider" :class="{ collapsed }">
      <div class="sider-top">
        <div class="brand-mark">
          <img v-if="siteLogo" class="logo-img" :src="siteLogo" alt="logo" />
          <div v-else class="brand-glyph">Q</div>
        </div>
        <div v-if="!collapsed" class="brand-copy">
          <span class="brand-title">{{ siteName }}</span>
          <span class="brand-sub">Console</span>
        </div>
        <button class="collapse-btn" @click="collapsed = !collapsed">
          {{ collapsed ? "›" : "‹" }}
        </button>
      </div>

      <div v-if="!collapsed" class="sider-panel">
        <div class="panel-label">运营中枢</div>
        <div class="panel-value">{{ siteDescription || "题库、AI、VIP 与用户的统一运营入口" }}</div>
      </div>

      <n-menu
        class="shell-menu"
        :collapsed="collapsed"
        :collapsed-width="88"
        :collapsed-icon-size="22"
        :options="menuOptions"
        :value="activeKey"
        accordion
        @update:value="handleMenuSelect"
      />

      <div class="sider-foot" :class="{ compact: collapsed }">
        <div class="foot-chip">
          <span class="chip-dot" />
          <span v-if="!collapsed">在线后台</span>
        </div>
      </div>
    </aside>

    <main class="shell-main">
      <header class="shell-header">
        <div class="header-copy">
          <span class="header-eyebrow">{{ roleText }}</span>
          <span class="header-title">{{ currentSection }}</span>
        </div>

        <div class="header-actions">
          <div class="site-meta" v-if="siteDescription">
            <span class="meta-dot" />
            <span>{{ siteDescription }}</span>
          </div>

          <n-dropdown :options="userMenuOptions" trigger="click" @select="handleUserMenuSelect">
            <div class="user-chip">
              <n-avatar :src="adminInfo?.avatar" :size="38" round>
                <template #fallback>
                  <div class="avatar-fallback">{{ (adminInfo?.nickname || adminInfo?.username || "A").slice(0, 1) }}</div>
                </template>
              </n-avatar>
              <div class="user-copy">
                <span class="user-name">{{ adminInfo?.nickname || adminInfo?.username || '管理员' }}</span>
                <span class="user-role">{{ roleText }}</span>
              </div>
            </div>
          </n-dropdown>
        </div>
      </header>

      <section class="shell-content">
        <router-view />
      </section>
    </main>

    <n-modal v-model:show="showProfileModal" preset="card" title="个人信息" style="width: 520px;">
      <n-form label-placement="left" label-width="84">
        <n-form-item label="头像">
          <div class="avatar-upload">
            <n-avatar :src="profileForm.avatar || adminInfo?.avatar" :size="68" round>
              <template #fallback>
                <div class="avatar-fallback">{{ (adminInfo?.nickname || adminInfo?.username || "A").slice(0, 1) }}</div>
              </template>
            </n-avatar>
            <n-upload :show-file-list="false" accept="image/*" :custom-request="handleAvatarUpload">
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

    <n-modal v-model:show="showPasswordModal" preset="card" title="修改密码" style="width: 460px;">
      <n-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-placement="left" label-width="84">
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
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  type DropdownOption,
  type FormInst,
  type FormRules,
  NIcon,
  type UploadCustomRequestOptions,
  useMessage
} from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { changePassword } from '@/api/auth'
import { updateAdmin } from '@/api/admin'
import { uploadImage } from '@/api/upload'
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

const siteName = computed(() => appStore.siteName || 'Quiz AI')
const siteLogo = computed(() => appStore.siteLogo)
const siteDescription = computed(() => appStore.siteDescription)
const adminInfo = computed(() => authStore.adminInfo)
const roleText = computed(() => adminInfo.value?.role === 'super_admin' ? '超级管理员' : '运营管理员')
const activeKey = computed(() => route.path)

const currentSection = computed(() => {
  const match = menuOptions.flatMap((item: any) => item.children || item).find((item: any) => item.key === route.path)
  return match?.label || '控制台'
})

const showProfileModal = ref(false)
const profileLoading = ref(false)
const profileForm = ref({ nickname: '', avatar: '' })

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

const userMenuOptions: DropdownOption[] = [
  { label: '个人信息', key: 'profile' },
  { label: '修改密码', key: 'password' },
  { type: 'divider' },
  { label: '退出登录', key: 'logout' },
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
  } catch {
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

  if (!authStore.adminInfo && authStore.token) {
    await authStore.fetchInfo()
  }
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: auto 1fr;
  min-height: 100vh;
  gap: 18px;
  padding: 18px;
}

.shell-sider {
  width: 292px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 18px;
  border-radius: 30px;
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.12), transparent 28%),
    linear-gradient(180deg, #251713 0%, #3a211a 48%, #221411 100%);
  color: #fff7f0;
  box-shadow: 0 20px 60px rgba(55, 27, 17, 0.24);
  transition: width 0.25s ease, padding 0.25s ease;
}

.shell-sider.collapsed {
  width: 96px;
  padding-inline: 10px;
}

.sider-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 18px;
  background: linear-gradient(145deg, rgba(246, 206, 176, 0.2), rgba(182, 64, 44, 0.42));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.12);
  flex-shrink: 0;
}

.brand-glyph {
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.logo-img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.brand-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.brand-title {
  font-size: 15px;
  font-weight: 700;
}

.brand-sub {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(255, 247, 240, 0.56);
}

.collapse-btn {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  color: #fff7f0;
  cursor: pointer;
}

.sider-panel {
  padding: 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255,255,255,0.08);
}

.panel-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.18em;
  color: rgba(255, 247, 240, 0.52);
}

.panel-value {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.65;
  color: rgba(255, 247, 240, 0.82);
}

.shell-menu {
  flex: 1;
  background: transparent;
}

.sider-foot {
  display: flex;
  justify-content: flex-start;
}

.sider-foot.compact {
  justify-content: center;
}

.foot-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255,255,255,0.06);
  font-size: 12px;
  color: rgba(255,247,240,0.72);
}

.chip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #e4b95c;
  box-shadow: 0 0 12px rgba(228, 185, 92, 0.7);
}

.shell-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 22px;
  border-radius: 28px;
  background: rgba(255, 252, 247, 0.72);
  border: 1px solid rgba(95, 68, 47, 0.12);
  box-shadow: var(--admin-shadow-soft);
  backdrop-filter: blur(18px);
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-eyebrow {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--admin-muted);
}

.header-title {
  font-size: 24px;
  font-weight: 750;
  color: var(--admin-text);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.site-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(182, 64, 44, 0.06);
  font-size: 12px;
  color: var(--admin-text-soft);
}

.meta-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--admin-accent);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px 8px 8px;
  min-width: 220px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(95, 68, 47, 0.1);
  cursor: pointer;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #b6402c, #d98958);
}

.user-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--admin-text);
}

.user-role {
  font-size: 12px;
  color: var(--admin-text-soft);
}

.shell-content {
  min-height: 0;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

@media (max-width: 1180px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .shell-sider,
  .shell-sider.collapsed {
    width: 100%;
  }
}
</style>
