<template>
  <view class="page-shell mine-page">
    <view class="profile-card glass-card" @tap="openLogin">
      <view class="profile-top">
        <image v-if="user?.avatar" class="avatar" :src="resolveAssetUrl(user.avatar)" mode="aspectFill" />
        <view v-else class="avatar avatar-fallback">Q</view>
        <view class="profile-copy">
          <text class="profile-name">{{ user?.nickname || "点击登录" }}</text>
          <text class="profile-desc" v-if="isLogin">{{ user?.phone || "已登录，资料可在这里维护" }}</text>
          <text class="profile-desc" v-else>登录后同步学习进度、收藏与答疑记录</text>
        </view>
        <view class="profile-arrow">›</view>
      </view>

      <view class="vip-ribbon" :class="{ active: user?.isVip === 1 }" @tap.stop="goVip">
        <view class="ribbon-copy">
          <text class="ribbon-title">功能说明</text>
          <text class="ribbon-desc">查看完整解析、学习记录与学习助手的页面说明</text>
        </view>
        <text class="ribbon-action">查看</text>
      </view>
    </view>

    <view class="stats-board glass-card" v-if="isLogin">
      <view class="board-item">
        <text class="board-label">学习天数</text>
        <text class="board-value">{{ stats?.totalDays || 0 }}</text>
      </view>
      <view class="board-item">
        <text class="board-label">答题总数</text>
        <text class="board-value">{{ stats?.totalAnswered || 0 }}</text>
      </view>
      <view class="board-item accent">
        <text class="board-label">正确率</text>
        <text class="board-value">{{ stats?.correctRate || 0 }}%</text>
      </view>
    </view>

    <view class="menu-block glass-card">
      <view class="menu-item" @tap="goRecord">
        <text class="menu-tag">记录</text>
        <view class="menu-copy">
          <text class="menu-title">学习记录</text>
          <text class="menu-desc">查看练习与考试轨迹</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goWrong">
        <text class="menu-tag">错题</text>
        <view class="menu-copy">
          <text class="menu-title">我的错题</text>
          <text class="menu-desc">重新补齐易错知识点</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goFavorite">
        <text class="menu-tag">收藏</text>
        <view class="menu-copy">
          <text class="menu-title">我的收藏</text>
          <text class="menu-desc">沉淀重点题目与笔记</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="menu-block glass-card">
      <view class="menu-item" @tap="goProfile">
        <text class="menu-tag">资料</text>
        <view class="menu-copy">
          <text class="menu-title">编辑资料</text>
          <text class="menu-desc">完善昵称、头像与手机号</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @tap="goSettings">
        <text class="menu-tag">设置</text>
        <view class="menu-copy">
          <text class="menu-title">系统设置</text>
          <text class="menu-desc">管理偏好和页面选项</text>
        </view>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="footer">
      <view class="footer-links">
        <text class="footer-link" @tap="openServiceAgreement">《用户服务协议》</text>
        <text class="footer-divider">|</text>
        <text class="footer-link" @tap="openPrivacyPolicy">《隐私政策》</text>
      </view>
      <text class="footer-name" v-if="siteName">{{ siteName }}</text>
      <text class="footer-meta" v-if="icpNumber">{{ icpNumber }}</text>
    </view>

    <LoginSheet
      :show="showLogin"
      @close="handleLoginClose"
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
import { useLoginSheet } from "@/composables/useLoginSheet";
import { resolveAssetUrl } from "@/utils/assets";
import { openAgreementPage } from "@/utils/privacy";

const userStore = useUserStore();
const appStore = useAppStore();
const user = computed(() => userStore.userInfo);
const stats = ref<any>(null);
const isLogin = computed(() => userStore.isLogin);
const siteName = computed(() => appStore.siteConfig.siteName || "");
const icpNumber = computed(() => appStore.siteConfig.icpNumber || "");
const { showLogin, requestLogin, handleLoginSuccess, handleLoginClose } = useLoginSheet("请先登录");

const fetchStats = async () => {
  if (!userStore.isLogin) return;
  stats.value = await getStudyStats();
};

const requireLogin = (action: () => void) => {
  requestLogin(action, "请先登录");
};

const openLogin = () => {
  if (!userStore.token || !userStore.userInfo?.id) {
    requestLogin(null, "请先登录");
    return;
  }
  uni.navigateTo({ url: "/pages/mine/profile" });
};
const goProfile = () => requireLogin(() => uni.navigateTo({ url: "/pages/mine/profile" }));
const goRecord = () => requireLogin(() => uni.navigateTo({ url: "/pages/record/index" }));
const goFavorite = () => requireLogin(() => uni.navigateTo({ url: "/pages/favorite/index" }));
const goWrong = () => requireLogin(() => uni.navigateTo({ url: "/pages/wrong/index" }));
const goVip = () => requireLogin(() => uni.navigateTo({ url: "/pages/vip/index" }));
const goSettings = () => uni.navigateTo({ url: "/pages/mine/settings" });
const openServiceAgreement = () => openAgreementPage("service");
const openPrivacyPolicy = () => openAgreementPage("privacy");

onShow(() => {
  if (userStore.isLogin) {
    void userStore.refreshUser().catch(() => null);
  }
  void appStore.loadSiteConfig().catch(() => null);
  void fetchStats().catch(() => null);
});
</script>

<style lang="scss" scoped>
.mine-page {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.profile-card {
  padding: 24rpx;
}

.profile-top {
  display: flex;
  align-items: center;
  gap: 18rpx;
}

.avatar {
  width: 100rpx;
  height: 100rpx;
  border-radius: 30rpx;
  overflow: hidden;
  background: var(--primary-weak);
  flex-shrink: 0;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 38rpx;
  font-weight: 700;
  color: var(--primary-dark);
}

.profile-copy {
  flex: 1;
  min-width: 0;
}

.profile-name {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
}

.profile-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.profile-arrow {
  font-size: 36rpx;
  color: var(--muted);
}

.vip-ribbon {
  margin-top: 20rpx;
  padding: 20rpx;
  border-radius: 24rpx;
  background: linear-gradient(145deg, #2b1b15, #4c2a20);
  color: #fff6ee;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.vip-ribbon.active {
  background: linear-gradient(145deg, #342014, #654227);
}

.ribbon-copy {
  flex: 1;
}

.ribbon-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.ribbon-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: rgba(255, 246, 238, 0.68);
}

.ribbon-action {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.16);
  font-size: 22rpx;
  font-weight: 700;
}

.stats-board {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  padding: 20rpx;
}

.board-item {
  padding: 18rpx 14rpx;
  border-radius: 22rpx;
  background: rgba(255,255,255,0.56);
}

.board-item.accent {
  background: linear-gradient(135deg, rgba(197, 76, 47, 0.12), rgba(221, 122, 89, 0.08));
}

.board-label {
  display: block;
  font-size: 20rpx;
  color: var(--muted);
}

.board-value {
  display: block;
  margin-top: 10rpx;
  font-size: 32rpx;
  font-weight: 800;
}

.menu-block {
  overflow: hidden;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx 22rpx;
  border-bottom: 1rpx solid var(--border-light);
}

.menu-item:last-child {
  border-bottom: none;
}

.menu-tag {
  width: 68rpx;
  height: 68rpx;
  border-radius: 20rpx;
  background: var(--primary-weak);
  color: var(--primary-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.menu-copy {
  flex: 1;
  min-width: 0;
}

.menu-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.menu-desc {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.menu-arrow {
  font-size: 28rpx;
  color: var(--muted);
}

.footer {
  text-align: center;
  padding: 24rpx 0 32rpx;
}

.footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-bottom: 12rpx;
}

.footer-link,
.footer-divider {
  font-size: 22rpx;
  color: var(--primary);
}

.footer-divider {
  color: var(--muted);
}

.footer-name {
  display: block;
  font-size: 24rpx;
  color: var(--muted);
}

.footer-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--muted);
}
</style>
