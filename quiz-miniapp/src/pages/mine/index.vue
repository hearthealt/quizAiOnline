<template>
  <view class="page">
    <view class="profile-card">
      <view class="profile-top" @tap="openLogin">
        <image
          v-if="user?.avatar"
          class="avatar"
          :src="resolveAssetUrl(user.avatar)"
          mode="aspectFill"
        />
        <view v-else class="avatar placeholder">?</view>
        <view class="info">
          <text class="name">{{ user?.nickname || "未登录" }}</text>
          <text class="level" v-if="isLogin">
            {{ user?.isVip === 1 ? "VIP会员" : "普通用户" }}
          </text>
          <text class="level hint" v-else>点击登录同步学习进度</text>
        </view>
      <button class="icon-btn" @tap.stop="goSettings">设置</button>
      </view>
      <view class="profile-actions">
        <button class="ghost-btn" @tap="goProfile">编辑资料</button>
        <button class="ghost-btn" @tap="goRecord">学习记录</button>
      </view>
    </view>

    <view class="vip-card">
      <view class="vip-left">
        <text class="vip-title">
          {{ user?.isVip === 1 && user?.vipExpireTime ? "续费 VIP 会员" : "开通 VIP 会员" }}
        </text>
        <text class="vip-sub" v-if="user?.isVip === 1 && user?.vipExpireTime">
          到期 {{ user.vipExpireTime }}
        </text>
        <text class="vip-sub" v-else>畅享全站题库与 AI 智能解析</text>
      </view>
      <button class="vip-btn" @tap="goVip">
        {{ user?.isVip === 1 && user?.vipExpireTime ? "立即续费" : "立即开通" }}
      </button>
    </view>

    <view class="stats-card" v-if="isLogin">
      <view class="stat-item">
        <text class="stat-value">{{ stats?.totalDays || 0 }}</text>
        <text class="stat-label">学习天数</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ stats?.totalAnswered || 0 }}</text>
        <text class="stat-label">累计答题</text>
      </view>
      <view class="stat-item highlight">
        <text class="stat-value">{{ stats?.correctRate || 0 }}%</text>
        <text class="stat-label">正确率</text>
      </view>
    </view>
    <view class="stats-guest" v-else>
      <view class="guest-card">
        <text class="guest-title">登录后查看学习统计</text>
        <text class="guest-desc">你的学习天数、答题数量与正确率会显示在这里</text>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item" @tap="goWrong">
        <view class="menu-icon danger">错</view>
        <text>我的错题</text>
      </view>
      <view class="menu-item" @tap="goFavorite">
        <view class="menu-icon warn">藏</view>
        <text>我的收藏</text>
      </view>
      <view class="menu-item" @tap="goVip">
        <view class="menu-icon primary">V</view>
        <text>会员中心</text>
      </view>
      <view class="menu-item" @tap="goSettings">
        <view class="menu-icon settings">设</view>
        <text>设置中心</text>
      </view>
    </view>

    <view class="site-footer" v-if="siteName || siteDescription || icpNumber || copyright">
      <text class="site-name" v-if="siteName">{{ siteName }}</text>
      <text class="site-desc" v-if="siteDescription">{{ siteDescription }}</text>
      <text class="site-meta" v-if="icpNumber">{{ icpNumber }}</text>
      <text class="site-meta" v-if="copyright">{{ copyright }}</text>
    </view>

    <LoginSheet
      :show="showLogin"
      @close="showLogin = false"
      @success="handleLoginSuccess"
    />
  </view>
</template>

<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";
import { getStudyStats } from "@/api/user";
import LoginSheet from "@/components/LoginSheet.vue";
import { resolveAssetUrl } from "@/utils/assets";

const userStore = useUserStore();
const appStore = useAppStore();
const user = computed(() => userStore.userInfo);
const stats = ref<any>(null);
const showLogin = ref(false);
const pendingAction = ref<null | (() => void | Promise<void>)>(null);
const isLogin = computed(() => userStore.isLogin);
const siteName = computed(() => appStore.siteConfig.siteName || "");
const siteDescription = computed(() => appStore.siteConfig.siteDescription || "");
const icpNumber = computed(() => appStore.siteConfig.icpNumber || "");
const copyright = computed(() => appStore.siteConfig.copyright || "");

const fetchStats = async () => {
  if (!userStore.isLogin) return;
  stats.value = await getStudyStats();
};

const requireLogin = (action: () => void | Promise<void>) => {
  if (userStore.isLogin) {
    action();
    return;
  }
  pendingAction.value = action;
  showLogin.value = true;
};

const handleLoginSuccess = () => {
  showLogin.value = false;
  fetchStats();
  const action = pendingAction.value;
  pendingAction.value = null;
  if (action) action();
};

const openLogin = () => {
  if (!userStore.isLogin) showLogin.value = true;
};

const goProfile = () => requireLogin(() => { void uni.navigateTo({ url: "/pages/mine/profile" }); });
const goRecord = () => requireLogin(() => { void uni.navigateTo({ url: "/pages/record/index" }); });
const goFavorite = () => requireLogin(() => { void uni.navigateTo({ url: "/pages/favorite/index" }); });
const goWrong = () => requireLogin(() => { void uni.navigateTo({ url: "/pages/wrong/index" }); });
const goVip = () => requireLogin(() => { void uni.navigateTo({ url: "/pages/vip/index" }); });
const goSettings = () => uni.navigateTo({ url: "/pages/mine/settings" });

onShow(() => {
  void appStore.loadSiteConfig();
  fetchStats();
  if (userStore.isLogin && pendingAction.value) {
    const action = pendingAction.value;
    pendingAction.value = null;
    action();
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.profile-card {
  background: var(--card);
  border-radius: var(--radius-xl);
  padding: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space);
  box-shadow: var(--shadow);
}

.profile-top {
  display: flex;
  align-items: center;
  gap: var(--space);
}

.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 44rpx;
  background: var(--primary-weak);
  overflow: hidden;
  flex-shrink: 0;
}

.avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--border);
  color: var(--muted);
  font-size: 32rpx;
}

.info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  min-width: 0;
}

.name {
  font-size: 30rpx;
  font-weight: 700;
}

.level {
  font-size: 24rpx;
  color: var(--muted);
}

.level.hint {
  color: var(--muted);
}

.icon-btn {
  padding: 0 var(--space-lg);
  height: 52rpx;
  border-radius: var(--radius-full);
  background: var(--primary-weak);
  color: var(--primary);
  font-size: 24rpx;
  line-height: 52rpx;
  text-align: center;
  flex-shrink: 0;
}

.profile-actions {
  display: flex;
  gap: var(--space-sm);
}

.ghost-btn {
  flex: 1;
  background: var(--primary-weak);
  color: var(--primary);
  height: 64rpx;
  line-height: 64rpx;
  border-radius: var(--radius-full);
  font-size: 26rpx;
  font-weight: 500;
}

.vip-card {
  background: linear-gradient(135deg, #111827, #1e293b);
  color: #ffffff;
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space);
}

.vip-left {
  flex: 1;
  min-width: 0;
}

.vip-title {
  font-size: 28rpx;
  font-weight: 700;
}

.vip-sub {
  margin-top: 6rpx;
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.7);
}

.vip-btn {
  background: var(--warning);
  color: #1f2937;
  height: 64rpx;
  line-height: 64rpx;
  border-radius: var(--radius-full);
  padding: 0 var(--space-xl);
  font-size: 26rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.stats-card {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  display: flex;
  justify-content: space-around;
  box-shadow: var(--shadow);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  align-items: center;
}

.stat-value {
  font-size: 30rpx;
  font-weight: 700;
}

.stat-label {
  font-size: 22rpx;
  color: var(--muted);
}

.stat-item.highlight .stat-value {
  color: var(--primary);
}

.stats-guest {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow);
}

.guest-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
}

.guest-title {
  font-size: 26rpx;
  font-weight: 600;
}

.guest-desc {
  font-size: 24rpx;
  color: var(--muted);
}

.menu-card {
  background: var(--card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: var(--space);
  padding: var(--space) var(--space-lg);
  font-size: 28rpx;
  border-bottom: 1rpx solid var(--border-light);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  width: 48rpx;
  height: 48rpx;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 22rpx;
  font-weight: 600;
}

.menu-icon.danger {
  background: var(--danger);
}

.menu-icon.warn {
  background: var(--warning);
}

.menu-icon.info {
  background: #3b82f6;
}

.menu-icon.primary {
  background: var(--primary);
}

.menu-icon.settings {
  background: var(--border);
  color: var(--text-secondary);
}

.site-footer {
  margin-top: var(--space-xs);
  padding: var(--space) var(--space-sm);
  text-align: center;
  color: var(--muted);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.site-name {
  font-size: 24rpx;
  font-weight: 600;
  color: var(--text-secondary);
}

.site-desc {
  font-size: 22rpx;
  color: var(--muted);
}

.site-meta {
  font-size: 20rpx;
  color: var(--muted);
}
</style>
