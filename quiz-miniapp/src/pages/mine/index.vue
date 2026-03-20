<template>
  <view class="page">
    <!-- 用户卡片 -->
    <view class="user-card" @tap="openLogin">
      <image v-if="user?.avatar" class="avatar" :src="resolveAssetUrl(user.avatar)" mode="aspectFill" />
      <view v-else class="avatar">👤</view>
      <view class="user-info">
        <text class="user-name">{{ user?.nickname || "点击登录" }}</text>
        <view class="user-badge" v-if="isLogin && user?.isVip === 1">
          <text>👑 VIP会员</text>
        </view>
        <text class="user-tip" v-else-if="!isLogin">登录同步学习进度</text>
      </view>
      <text class="arrow">›</text>
    </view>

    <!-- 学习数据 -->
    <view class="stats-card" v-if="isLogin">
      <view class="stat-item">
        <text class="stat-num">{{ stats?.totalDays || 0 }}</text>
        <text class="stat-label">学习天数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num">{{ stats?.totalAnswered || 0 }}</text>
        <text class="stat-label">答题数</text>
      </view>
      <view class="stat-item">
        <text class="stat-num primary">{{ stats?.correctRate || 0 }}%</text>
        <text class="stat-label">正确率</text>
      </view>
    </view>

    <!-- VIP卡片 -->
    <view class="vip-card" @tap="goVip">
      <view class="vip-icon">👑</view>
      <view class="vip-info">
        <text class="vip-title">{{ user?.isVip === 1 ? '续费VIP会员' : '开通VIP会员' }}</text>
        <text class="vip-desc">{{ user?.isVip === 1 && user?.vipExpireTime ? '到期 ' + user.vipExpireTime : '畅享全部题库与AI解析' }}</text>
      </view>
      <text class="vip-btn">{{ user?.isVip === 1 ? '续费' : '开通' }}</text>
    </view>

    <!-- 功能菜单 -->
    <view class="menu-card">
      <view class="menu-item" @tap="goRecord">
        <text class="menu-icon">📊</text>
        <text class="menu-text">学习记录</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goWrong">
        <text class="menu-icon">❌</text>
        <text class="menu-text">我的错题</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goFavorite">
        <text class="menu-icon">⭐</text>
        <text class="menu-text">我的收藏</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="menu-card">
      <view class="menu-item" @tap="goProfile">
        <text class="menu-icon">👤</text>
        <text class="menu-text">编辑资料</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goSettings">
        <text class="menu-icon">⚙️</text>
        <text class="menu-text">设置</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <!-- 底部版权 -->
    <view class="footer" v-if="siteName">
      <text class="footer-name">{{ siteName }}</text>
      <text class="footer-meta" v-if="icpNumber">{{ icpNumber }}</text>
    </view>

    <LoginSheet :show="showLogin" @close="showLogin = false" @success="handleLoginSuccess" />
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
const pendingAction = ref<null | (() => void)>(null);
const isLogin = computed(() => userStore.isLogin);
const siteName = computed(() => appStore.siteConfig.siteName || "");
const icpNumber = computed(() => appStore.siteConfig.icpNumber || "");

const fetchStats = async () => {
  if (!userStore.isLogin) return;
  stats.value = await getStudyStats();
};

const requireLogin = (action: () => void) => {
  if (userStore.isLogin) { action(); return; }
  pendingAction.value = action;
  showLogin.value = true;
};

const handleLoginSuccess = () => {
  showLogin.value = false;
  fetchStats();
  if (pendingAction.value) { pendingAction.value(); pendingAction.value = null; }
};

const openLogin = () => { if (!userStore.isLogin) showLogin.value = true; };
const goProfile = () => requireLogin(() => uni.navigateTo({ url: "/pages/mine/profile" }));
const goRecord = () => requireLogin(() => uni.navigateTo({ url: "/pages/record/index" }));
const goFavorite = () => requireLogin(() => uni.navigateTo({ url: "/pages/favorite/index" }));
const goWrong = () => requireLogin(() => uni.navigateTo({ url: "/pages/wrong/index" }));
const goVip = () => requireLogin(() => uni.navigateTo({ url: "/pages/vip/index" }));
const goSettings = () => uni.navigateTo({ url: "/pages/mine/settings" });

onShow(() => {
  appStore.loadSiteConfig();
  fetchStats();
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  background: var(--bg);
  min-height: 100vh;
}

.user-card {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 24rpx;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 34rpx;
  font-weight: 600;
  color: #fff;
  display: block;
}

.user-badge {
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.2);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #fef3c7;
}

.user-tip {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.8);
  margin-top: 8rpx;
}

.arrow {
  font-size: 36rpx;
  color: rgba(255, 255, 255, 0.6);
}

.stats-card {
  background: var(--card);
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  justify-content: space-around;
  margin-bottom: 24rpx;
  box-shadow: var(--shadow-sm);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.stat-num {
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text);
}

.stat-num.primary {
  color: var(--primary);
}

.stat-label {
  font-size: 24rpx;
  color: var(--muted);
}

.vip-card {
  background: linear-gradient(135deg, #1f2937, #374151);
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.vip-icon {
  font-size: 40rpx;
}

.vip-info {
  flex: 1;
}

.vip-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #fef3c7;
  display: block;
}

.vip-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 4rpx;
}

.vip-btn {
  padding: 16rpx 32rpx;
  background: #f59e0b;
  color: #1f2937;
  border-radius: 32rpx;
  font-size: 26rpx;
  font-weight: 600;
}

.menu-card {
  background: var(--card);
  border-radius: 20rpx;
  overflow: hidden;
  margin-bottom: 24rpx;
  box-shadow: var(--shadow-sm);
}

.menu-item {
  display: flex;
  align-items: center;
  padding: 28rpx 24rpx;
  border-bottom: 1rpx solid var(--border-light);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-icon {
  font-size: 32rpx;
  margin-right: 20rpx;
}

.menu-text {
  flex: 1;
  font-size: 28rpx;
  color: var(--text);
}

.menu-arrow {
  font-size: 28rpx;
  color: var(--muted);
}

.footer {
  text-align: center;
  padding: 40rpx 0;
}

.footer-name {
  font-size: 24rpx;
  color: var(--muted);
  display: block;
}

.footer-meta {
  font-size: 22rpx;
  color: var(--muted);
  margin-top: 8rpx;
}
</style>
