<template>
  <view class="page-shell home-page">
    <view class="hero glass-card">
      <view class="hero-top">
        <view class="user-info" @tap="goMine">
          <image
            v-if="userStore.userInfo?.avatar"
            class="avatar"
            :src="resolveAssetUrl(userStore.userInfo.avatar)"
            mode="aspectFill"
          />
          <view v-else class="avatar avatar-fallback">Q</view>
          <view class="user-copy">
            <text class="user-greeting">{{ greeting }}，{{ nickname }}</text>
            <text class="user-tip">{{ isLogin ? "学习状态已同步到云端" : "登录后同步学习进度与AI记录" }}</text>
          </view>
        </view>
        <view class="search-btn" @tap="goSearch">检索</view>
      </view>

      <view class="hero-body">
        <view class="hero-stats">
          <view class="stat-chip">
            <text class="chip-label">学习天数</text>
            <text class="chip-value">{{ homeData?.studyStats?.totalDays || 0 }}</text>
          </view>
          <view class="stat-chip">
            <text class="chip-label">答题总数</text>
            <text class="chip-value">{{ homeData?.studyStats?.totalAnswered || 0 }}</text>
          </view>
          <view class="stat-chip accent">
            <text class="chip-label">正确率</text>
            <text class="chip-value">{{ homeData?.studyStats?.correctRate || 0 }}%</text>
          </view>
        </view>

        <view class="daily-card" v-if="homeData?.dailyQuestion" @tap="goPracticeDaily">
          <view class="daily-head">
            <text class="daily-tag">今日问题</text>
            <text class="daily-link">去挑战</text>
          </view>
          <text class="daily-content">{{ homeData.dailyQuestion.content }}</text>
        </view>
      </view>
    </view>

    <view class="quick-strip">
      <view class="quick-item glass-card" @tap="goBankList">
        <text class="quick-icon">题</text>
        <text class="quick-title">全部题库</text>
        <text class="quick-desc">快速进入练习</text>
      </view>
      <view class="quick-item glass-card" @tap="goWrong">
        <text class="quick-icon">错</text>
        <text class="quick-title">错题重练</text>
        <text class="quick-desc">重新补齐薄弱点</text>
      </view>
      <view class="quick-item glass-card" @tap="goFavorite">
        <text class="quick-icon">藏</text>
        <text class="quick-title">收藏题目</text>
        <text class="quick-desc">保留重点难点</text>
      </view>
      <view class="quick-item glass-card" @tap="goRecord">
        <text class="quick-icon">记</text>
        <text class="quick-title">学习记录</text>
        <text class="quick-desc">查看练习轨迹</text>
      </view>
    </view>

    <view class="section-head">
      <view>
        <text class="section-title">热门题库</text>
        <text class="section-sub">从高频练习题库里直接开始</text>
      </view>
      <text class="section-link" @tap="goBankList">查看全部</text>
    </view>

    <view class="bank-stack">
      <view class="bank-card glass-card" v-for="bank in homeData?.hotBanks || []" :key="bank.id" @tap="goBankDetail(bank.id)">
        <image v-if="bank.cover" class="bank-cover" :src="resolveAssetUrl(bank.cover)" mode="aspectFill" />
        <view v-else class="bank-cover placeholder">题库</view>
        <view class="bank-copy">
          <text class="bank-name">{{ bank.name }}</text>
          <text class="bank-meta">{{ bank.questionCount || 0 }}题 · {{ formatCount(bank.practiceCount) }}人练习</text>
        </view>
        <view class="bank-action">开始</view>
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
.home-page {
  display: flex;
  flex-direction: column;
  gap: 22rpx;
}

.hero {
  padding: 28rpx;
  background:
    linear-gradient(145deg, rgba(39, 22, 16, 0.95), rgba(88, 44, 30, 0.92)),
    radial-gradient(circle at top left, rgba(255,255,255,0.12), transparent 26%);
  color: #fff8f1;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16rpx;
  min-width: 0;
  flex: 1;
}

.avatar {
  width: 86rpx;
  height: 86rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.18);
  flex-shrink: 0;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34rpx;
  font-weight: 700;
}

.user-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.user-greeting {
  font-size: 34rpx;
  font-weight: 700;
  display: block;
}

.user-tip {
  font-size: 22rpx;
  color: rgba(255, 248, 241, 0.68);
}

.search-btn {
  padding: 14rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.12);
  font-size: 22rpx;
  color: #fff8f1;
  flex-shrink: 0;
}

.hero-body {
  margin-top: 26rpx;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
}

.stat-chip {
  padding: 18rpx 16rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.08);
}

.stat-chip.accent {
  background: linear-gradient(135deg, rgba(208, 138, 31, 0.22), rgba(221, 122, 89, 0.18));
}

.chip-label {
  display: block;
  font-size: 20rpx;
  color: rgba(255, 248, 241, 0.6);
}

.chip-value {
  display: block;
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 800;
}

.daily-card {
  margin-top: 18rpx;
  padding: 20rpx 22rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.08);
  border: 1rpx solid rgba(255,255,255,0.08);
}

.daily-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.daily-tag {
  font-size: 20rpx;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(255, 248, 241, 0.56);
}

.daily-link {
  font-size: 22rpx;
  color: #ffd9c8;
}

.daily-content {
  display: block;
  margin-top: 14rpx;
  font-size: 28rpx;
  line-height: 1.7;
}

.quick-strip {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.quick-item {
  padding: 22rpx;
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.quick-icon {
  width: 60rpx;
  height: 60rpx;
  border-radius: 18rpx;
  background: var(--primary-weak);
  color: var(--primary-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 700;
}

.quick-title {
  font-size: 28rpx;
  font-weight: 700;
}

.quick-desc {
  font-size: 22rpx;
  color: var(--muted);
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16rpx;
  padding: 6rpx 4rpx 0;
}

.section-title {
  display: block;
  font-size: 32rpx;
  font-weight: 700;
}

.section-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.section-link {
  font-size: 24rpx;
  color: var(--primary);
}

.bank-stack {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bank-card {
  padding: 18rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.bank-cover {
  width: 92rpx;
  height: 92rpx;
  border-radius: 24rpx;
  overflow: hidden;
  background: var(--primary-weak);
  flex-shrink: 0;
}

.bank-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-dark);
  font-size: 22rpx;
  font-weight: 700;
}

.bank-copy {
  flex: 1;
  min-width: 0;
}

.bank-name {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.bank-action {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: var(--primary);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
}
</style>
