<template>
  <view class="page">
    <!-- 顶部用户区 -->
    <view class="header">
      <view class="user-info" @tap="goMine">
        <image v-if="userStore.userInfo?.avatar" class="avatar" :src="resolveAssetUrl(userStore.userInfo.avatar)" mode="aspectFill" />
        <view v-else class="avatar">👤</view>
        <view class="user-text">
          <text class="user-name">{{ greeting }}，{{ nickname }}</text>
          <text class="user-tip">{{ isLogin ? '今天也要加油哦' : '登录同步学习进度' }}</text>
        </view>
      </view>
      <view class="search-btn" @tap="goSearch">🔍</view>
    </view>

    <!-- 学习数据卡片 -->
    <view class="stats-card">
      <view class="stat-item">
        <text class="stat-num">{{ homeData?.studyStats?.totalDays || 0 }}</text>
        <text class="stat-label">学习天数</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-num">{{ homeData?.studyStats?.totalAnswered || 0 }}</text>
        <text class="stat-label">答题总数</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-num primary">{{ homeData?.studyStats?.correctRate || 0 }}%</text>
        <text class="stat-label">正确率</text>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="quick-grid">
      <view class="quick-item" @tap="goBankList">
        <view class="quick-icon" style="background: #fee2e2;">📚</view>
        <text class="quick-text">题库</text>
      </view>
      <view class="quick-item" @tap="goWrong">
        <view class="quick-icon" style="background: #fef3c7;">❌</view>
        <text class="quick-text">错题</text>
      </view>
      <view class="quick-item" @tap="goFavorite">
        <view class="quick-icon" style="background: #dbeafe;">⭐</view>
        <text class="quick-text">收藏</text>
      </view>
      <view class="quick-item" @tap="goRecord">
        <view class="quick-icon" style="background: #d1fae5;">📊</view>
        <text class="quick-text">记录</text>
      </view>
    </view>

    <!-- 每日一题 -->
    <view class="daily-card" v-if="homeData?.dailyQuestion" @tap="goPracticeDaily">
      <view class="daily-header">
        <text class="daily-tag">📝 每日一题</text>
        <text class="daily-action">去挑战 →</text>
      </view>
      <text class="daily-content">{{ homeData.dailyQuestion.content }}</text>
    </view>

    <!-- 热门题库 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">🔥 热门题库</text>
        <text class="section-more" @tap="goBankList">更多</text>
      </view>
      <view class="bank-list">
        <view class="bank-item" v-for="bank in homeData?.hotBanks || []" :key="bank.id" @tap="goBankDetail(bank.id)">
          <image v-if="bank.cover" class="bank-cover" :src="resolveAssetUrl(bank.cover)" mode="aspectFill" />
          <view v-else class="bank-cover placeholder">📖</view>
          <view class="bank-info">
            <text class="bank-name">{{ bank.name }}</text>
            <text class="bank-meta">{{ bank.questionCount || 0 }}题 · {{ formatCount(bank.practiceCount) }}人练习</text>
          </view>
          <view class="bank-btn">开始</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { useSharedHomeCache } from "@/composables/useHomeCache";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const { homeData, loadHomeData } = useSharedHomeCache();
const userStore = useUserStore();
const appStore = useAppStore();

const isLogin = computed(() => userStore.isLogin);
const nickname = computed(() => isLogin.value ? userStore.userInfo?.nickname || "同学" : "同学");
const greeting = ref("你好");

const updateGreeting = () => {
  const hour = new Date().getHours();
  if (hour < 6) greeting.value = "夜深了";
  else if (hour < 12) greeting.value = "早上好";
  else if (hour < 18) greeting.value = "下午好";
  else greeting.value = "晚上好";
};

const goSearch = () => uni.navigateTo({ url: "/pages/search/index" });
const goBankList = () => uni.navigateTo({ url: "/pages/bank/list" });
const goMine = () => uni.switchTab({ url: "/pages/mine/index" });
const goWrong = () => uni.navigateTo({ url: "/pages/wrong/index" });
const goFavorite = () => uni.navigateTo({ url: "/pages/favorite/index" });
const goRecord = () => uni.navigateTo({ url: "/pages/record/index" });
const goBankDetail = (id: number) => uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
const goPracticeDaily = () => {
  if (!homeData.value?.dailyQuestion) return;
  uni.navigateTo({ url: `/pages/bank/detail?id=${homeData.value.dailyQuestion.bankId}` });
};

onShow(async () => {
  await appStore.loadSiteConfig();
  await loadHomeData();
  updateGreeting();
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  background: var(--bg);
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.avatar {
  width: 80rpx;
  height: 80rpx;
  border-radius: 50%;
  background: var(--primary-weak);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.user-text {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.user-name {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text);
}

.user-tip {
  font-size: 24rpx;
  color: var(--muted);
}

.search-btn {
  width: 72rpx;
  height: 72rpx;
  background: var(--card);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  box-shadow: var(--shadow-sm);
}

.stats-card {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 24rpx;
  padding: 32rpx;
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-bottom: 24rpx;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.stat-num {
  font-size: 40rpx;
  font-weight: 700;
  color: #fff;
}

.stat-num.primary {
  color: #fef3c7;
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.85);
}

.stat-divider {
  width: 1rpx;
  height: 60rpx;
  background: rgba(255, 255, 255, 0.3);
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin-bottom: 24rpx;
}

.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 0;
  background: var(--card);
  border-radius: 16rpx;
  box-shadow: var(--shadow-sm);
}

.quick-icon {
  width: 72rpx;
  height: 72rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.quick-text {
  font-size: 24rpx;
  color: var(--text-secondary);
}

.daily-card {
  background: var(--card);
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 24rpx;
  box-shadow: var(--shadow);
  border-left: 6rpx solid var(--primary);
}

.daily-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.daily-tag {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--primary);
}

.daily-action {
  font-size: 24rpx;
  color: var(--primary);
}

.daily-content {
  font-size: 28rpx;
  color: var(--text-secondary);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.section {
  margin-bottom: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text);
}

.section-more {
  font-size: 24rpx;
  color: var(--muted);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bank-item {
  background: var(--card);
  border-radius: 16rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  box-shadow: var(--shadow-sm);
}

.bank-cover {
  width: 88rpx;
  height: 88rpx;
  border-radius: 12rpx;
  background: var(--primary-weak);
  flex-shrink: 0;
}

.bank-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.bank-info {
  flex: 1;
  min-width: 0;
}

.bank-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta {
  font-size: 22rpx;
  color: var(--muted);
  margin-top: 8rpx;
  display: block;
}

.bank-btn {
  padding: 12rpx 28rpx;
  background: var(--primary);
  color: #fff;
  border-radius: 32rpx;
  font-size: 24rpx;
  font-weight: 500;
}
</style>
