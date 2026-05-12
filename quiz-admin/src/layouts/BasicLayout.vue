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
          <span class="brand-sub">Workspace</span>
        </div>
        <button class="collapse-btn" @click="collapsed = !collapsed">
          {{ collapsed ? "›" : "‹" }}
        </button>
      </div>

      <div class="menu-wrap">
        <div v-if="!collapsed" class="menu-label">主导航</div>
        <div class="custom-menu">
          <template v-for="item in menuOptions" :key="item.key">
            <!-- 单个菜单项 -->
            <div
              v-if="!item.children"
              class="menu-item"
              :class="{ active: activeKey === item.key }"
              @click="handleMenuSelect(item.key)"
            >
              <component :is="item.icon" class="menu-icon" />
              <span v-if="!collapsed" class="menu-label-text">{{ item.label }}</span>
            </div>

            <!-- 带子菜单的项 -->
            <div v-else class="menu-group">
              <!-- 折叠状态下使用 Dropdown -->
              <n-dropdown
                v-if="collapsed"
                :options="item.children.map((c: any) => ({ label: c.label, key: c.key }))"
                placement="right-start"
                trigger="hover"
                @select="handleMenuSelect"
              >
                <div
                  class="menu-item"
                  :class="{ active: item.children.some((c: any) => c.key === activeKey) }"
                >
                  <component :is="item.icon" class="menu-icon" />
                </div>
              </n-dropdown>

              <!-- 展开状态下正常显示 -->
              <template v-else>
                <div
                  class="menu-item"
                  :class="{ active: item.children.some((c: any) => c.key === activeKey) }"
                  @click="toggleGroup(item.key)"
                >
                  <component :is="item.icon" class="menu-icon" />
                  <span class="menu-label-text">{{ item.label }}</span>
                  <span class="menu-arrow" :class="{ expanded: expandedGroups.includes(item.key) }">›</span>
                </div>
                <div v-if="expandedGroups.includes(item.key)" class="menu-children">
                  <div
                    v-for="child in item.children"
                    :key="child.key"
                    class="menu-item menu-child"
                    :class="{ active: activeKey === child.key }"
                    @click="handleMenuSelect(child.key)"
                  >
                    <component :is="child.icon" class="menu-icon" />
                    <span class="menu-label-text">{{ child.label }}</span>
                  </div>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>

      <div class="sider-foot" :class="{ compact: collapsed }">
        <div class="foot-note">
          <span class="chip-dot" />
          <span v-if="!collapsed">Admin Workspace</span>
        </div>
      </div>
    </aside>

    <main class="shell-main">
      <header class="shell-header">
        <n-breadcrumb>
          <n-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
            {{ item.title }}
          </n-breadcrumb-item>
        </n-breadcrumb>

        <div class="header-actions">
          <n-dropdown :options="userMenuOptions" trigger="click" @select="handleUserMenuSelect">
            <div class="user-chip">
              <n-avatar :src="resolveAssetUrl(adminInfo?.avatar)" :size="34" round>
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
            <n-avatar :src="resolveAssetUrl(profileForm.avatar || adminInfo?.avatar)" :size="68" round>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  type DropdownOption,
  type FormInst,
  type FormRules,
  type UploadCustomRequestOptions,
  useMessage
} from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { changePassword, updateProfile } from '@/api/auth'
import { uploadImage } from '@/api/upload'
import { resolveAssetUrl } from '@/utils/assets'
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
  CloudDownloadOutline,
  WalletOutline,
} from '@vicons/ionicons5'

const collapsed = ref(false)
const expandedGroups = ref<string[]>([])
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const message = useMessage()

function toggleGroup(key: string) {
  const index = expandedGroups.value.indexOf(key)
  if (index > -1) {
    // 如果已经展开，则收起
    expandedGroups.value.splice(index, 1)
  } else {
    // 否则，关闭所有其他的，只展开当前的
    expandedGroups.value = [key]
  }
}

const siteName = computed(() => appStore.siteName || 'Quiz AI')
const siteLogo = computed(() => appStore.siteLogo)
const adminInfo = computed(() => authStore.adminInfo)
const isSuperAdmin = computed(() => adminInfo.value?.role === 'super_admin')
const roleText = computed(() => adminInfo.value?.role === 'super_admin' ? '超级管理员' : '运营管理员')
const activeKey = computed(() => route.path)

const breadcrumbs = computed(() => {
  const items = [{ title: '首页', path: '/dashboard' }]
  const flat = menuOptions.value.flatMap((item: any) => item.children ? [item, ...item.children] : [item])
  const match = flat.find((item: any) => item.key === route.path)
  if (match && match.key !== '/dashboard') {
    if (match.children) {
      items.push({ title: match.label, path: '' })
    } else {
      const parent = menuOptions.value.find((g: any) => g.children?.some((c: any) => c.key === route.path))
      if (parent) items.push({ title: parent.label, path: '' })
      items.push({ title: match.label, path: match.key })
    }
  }
  return items
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
    const updated = await updateProfile({
      nickname: profileForm.value.nickname,
      avatar: profileForm.value.avatar
    }) as any
    authStore.setAdminInfo({
      ...authStore.adminInfo!,
      nickname: updated?.nickname ?? profileForm.value.nickname,
      avatar: updated?.avatar ?? profileForm.value.avatar,
      role: updated?.role ?? authStore.adminInfo!.role
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

const allMenuOptions = [
  { label: '仪表盘', key: '/dashboard', icon: HomeOutline },
  {
    label: '题库管理',
    key: 'question-group',
    icon: FolderOutline,
    children: [
      { label: '分类管理', key: '/category', icon: FolderOutline },
      { label: '题库列表', key: '/bank', icon: BookOutline },
      { label: '题目管理', key: '/question', icon: DocumentTextOutline },
      { label: '题目转换', key: '/bank/convert', icon: SwapHorizontalOutline },
      { label: 'EZTest直连', key: '/eztest/api', icon: CloudDownloadOutline },
    ],
  },
  {
    label: '用户管理',
    key: 'user-group',
    icon: PeopleOutline,
    children: [
      { label: '用户列表', key: '/user', icon: PeopleOutline },
      { label: '活跃分析', key: '/activity/users', icon: StatsChartOutline },
    ],
  },
  {
    label: '记录管理',
    key: 'record-group',
    icon: CreateOutline,
    children: [
      { label: '练习记录', key: '/record/practice', icon: CreateOutline },
      { label: '考试记录', key: '/record/exam', icon: ListOutline },
      { label: '今日答题', key: '/activity/answers', icon: DocumentTextOutline },
    ],
  },
  {
    label: '数据中心',
    key: 'data-group',
    icon: StatsChartOutline,
    children: [
      { label: '收藏管理', key: '/favorite', icon: HeartOutline },
      { label: '错题管理', key: '/wrong', icon: CloseCircleOutline },
      { label: '数据统计', key: '/statistics', icon: StatsChartOutline },
    ],
  },
  {
    label: 'VIP管理',
    key: 'vip-group',
    icon: DiamondOutline,
    children: [
      { label: '套餐配置', key: '/vip/plan', icon: DiamondOutline },
      { label: '订单列表', key: '/vip/order', icon: WalletOutline },
    ],
  },
  {
    label: 'AI配置',
    key: 'ai-group',
    icon: HardwareChipOutline,
    children: [
      { label: '模型配置', key: '/ai/config', icon: HardwareChipOutline },
      { label: '批量任务', key: '/ai/batch', icon: ListOutline },
      { label: '调用日志', key: '/ai/log', icon: DocumentOutline },
    ],
  },
  {
    label: '系统管理',
    key: 'system-group',
    icon: SettingsOutline,
    children: [
      { label: '管理员管理', key: '/system/admin', icon: BuildOutline },
      { label: '系统设置', key: '/system/setting', icon: SettingsOutline },
    ],
  },
]

const adminAllowedKeys = new Set(['/dashboard', 'question-group', '/category', '/bank', '/question', '/eztest/api'])

const menuOptions = computed(() => {
  if (isSuperAdmin.value) {
    return allMenuOptions
  }
  return allMenuOptions
    .map((item: any) => {
      if (!item.children) {
        return adminAllowedKeys.has(item.key) ? item : null
      }
      const children = item.children.filter((child: any) => adminAllowedKeys.has(child.key))
      return children.length && adminAllowedKeys.has(item.key) ? { ...item, children } : null
    })
    .filter(Boolean)
})

async function handleMenuSelect(key: string) {
  if (!key.startsWith('/')) return
  try {
    await router.push(key)
  } catch (e: any) {
    if (e?.type) return
    message.error(e?.message || '页面加载失败，请刷新后重试')
  }
}

onMounted(async () => {
  try {
    await appStore.loadSiteConfig()
  } catch {}

  if (!authStore.infoLoaded && authStore.token) {
    await authStore.fetchInfo()
  }
})
</script>

<style scoped>
.admin-shell {
  display: grid;
  grid-template-columns: auto 1fr;
  min-height: 100vh;
  gap: var(--gap-page);
  padding: var(--gap-page);
  align-items: start;
}

/* ── Sidebar ── */

.shell-sider {
  position: sticky;
  top: var(--gap-page);
  align-self: start;
  width: 170px;
  height: calc(100vh - var(--gap-page) * 2);
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 10px 10px;
  border-radius: var(--radius-lg);
  background: var(--bg-surface);
  color: var(--color-text);
  border: var(--glass-border);
  box-shadow: var(--shadow-card);
  backdrop-filter: var(--glass-blur);
  transition: width 0.25s ease, padding 0.25s ease;
  overflow: hidden;
}

.shell-sider.collapsed {
  width: 88px;
  padding-inline: 0;
}

.sider-top {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 2px 4px 10px;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  background: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(95, 68, 47, 0.08);
  flex-shrink: 0;
  color: var(--color-primary);
}

.brand-glyph {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.logo-img {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.brand-copy {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.brand-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
}

.brand-sub {
  margin-top: 2px;
  font-size: 11px;
  color: var(--color-text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.collapse-btn {
  width: 28px;
  height: 28px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}

.menu-wrap {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding-top: 2px;
  overflow: hidden;
}

.menu-label {
  padding: 0 10px 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.custom-menu {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  margin: 2px 0;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s ease;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 600;
  position: relative;
}

.shell-sider.collapsed .menu-item {
  justify-content: center;
  padding: 8px 0;
}

.menu-item:hover {
  background: rgba(182, 64, 44, 0.04);
  color: var(--color-text);
}

.menu-item.active {
  background: var(--color-primary-fade);
  color: var(--color-text);
}

.menu-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.menu-label-text {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.menu-arrow {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: transform 0.2s ease;
  color: var(--color-text-muted);
}

.menu-arrow.expanded {
  transform: rotate(90deg);
}

.menu-children {
  padding-left: 10px;
  margin-top: 2px;
}

.menu-child {
  font-size: 13px;
}

.sider-foot {
  display: flex;
  justify-content: flex-start;
  padding-top: 6px;
}

.sider-foot.compact {
  justify-content: center;
}

.sider-foot.compact .foot-note {
  width: auto;
  justify-content: center;
}

.foot-note {
  width: 100%;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  font-size: 11px;
  font-weight: 600;
}

.chip-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-success);
  box-shadow: 0 0 8px rgba(58, 165, 109, 0.35);
}

/* ── Main Area ── */

.shell-main {
  display: flex;
  flex-direction: column;
  gap: var(--gap-page);
  min-width: 0;
}

.shell-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 14px 22px;
  border-radius: var(--radius-lg);
  background: var(--bg-header);
  border: var(--glass-border);
  box-shadow: var(--shadow-soft);
  backdrop-filter: var(--glass-blur);
  min-height: 64px;
}

.shell-header :deep(.n-breadcrumb .n-breadcrumb-item__link) {
  color: var(--color-text-secondary);
  font-weight: 600;
  font-size: 13px;
}

.shell-header :deep(.n-breadcrumb .n-breadcrumb-item__separator) {
  color: var(--color-text-muted);
}

.shell-header :deep(.n-breadcrumb .n-breadcrumb-item:last-child .n-breadcrumb-item__link) {
  color: var(--color-text);
  font-weight: 700;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 10px 6px 6px;
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
  font-size: 13px;
  font-weight: 700;
  color: var(--color-text);
}

.user-role {
  font-size: 11px;
  color: var(--color-text-muted);
}

.shell-content {
  min-height: 0;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* ── Responsive ── */

@media (max-width: 1180px) {
  .admin-shell {
    grid-template-columns: 1fr;
    gap: 12px;
    padding: 12px;
  }

  .shell-sider,
  .shell-sider.collapsed {
    position: static;
    top: auto;
    align-self: stretch;
    width: 100%;
    height: auto;
  }
}
</style>
