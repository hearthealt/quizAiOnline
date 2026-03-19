<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <view class="hero-text">
          <text class="hero-title">{{ greeting }}，{{ nickname }}</text>
          <text class="hero-sub" v-if="isLogin">坚持学习，进步看得见</text>
          <text class="hero-sub" v-else>登录后同步学习数据</text>
        </view>
        <view class="hero-badge" v-if="isLogin">
          <text class="badge-label">今日答题</text>
          <text class="badge-value">{{ home?.studyStats?.todayAnswered || 0 }}</text>
        </view>
      </view>

      <view class="hero-search" @tap="goSearch">
        <text class="search-icon">搜</text>
        <text class="search-placeholder">搜索题库、题目...</text>
      </view>
    </view>

    <view class="stats-grid" v-if="isLogin">
      <view class="stat-card">
        <text class="stat-value">{{ home?.studyStats?.totalDays || 0 }}</text>
        <text class="stat-label">学习天数</text>
      </view>
      <view class="stat-card">
        <text class="stat-value">{{ home?.studyStats?.totalAnswered || 0 }}</text>
        <text class="stat-label">累计答题</text>
      </view>
      <view class="stat-card highlight">
        <text class="stat-value">{{ home?.studyStats?.correctRate || 0 }}%</text>
        <text class="stat-label">正确率</text>
      </view>
    </view>
    <view class="stats-guest" v-else>
      <view class="guest-card">
        <text class="guest-title">登录后查看学习统计</text>
        <text class="guest-desc">记录你的学习天数、答题数量和正确率</text>
      </view>
    </view>

    <view class="daily-card" v-if="home?.dailyQuestion">
      <view class="daily-icon">题</view>
      <view class="daily-left">
        <text class="daily-tag">每日一题</text>
        <text class="daily-content">{{ home?.dailyQuestion?.content }}</text>
      </view>
      <button class="daily-btn" @tap="goPracticeDaily">去挑战</button>
    </view>
    <view class="daily-card" v-else>
      <view class="daily-icon">题</view>
      <view class="daily-left">
        <text class="daily-tag">每日一题</text>
        <text class="daily-empty">暂无题目</text>
      </view>
    </view>

    <view class="section-header">
      <text class="section-title">热门题库</text>
      <text class="section-link" @tap="goBankList">全部</text>
    </view>
    <view class="bank-list">
      <view
        v-for="bank in home?.hotBanks || []"
        :key="bank.id"
        class="bank-item"
        @tap="goBankDetail(bank.id)"
      >
        <image
          v-if="bank.cover"
          class="bank-cover"
          :src="resolveAssetUrl(bank.cover)"
          mode="aspectFill"
        />
        <view v-else class="bank-cover" />
        <view class="bank-info">
          <text class="bank-name">{{ bank.name }}</text>
          <text class="bank-meta">
            {{ bank.questionCount || 0 }} 题 · {{ formatCount(bank.practiceCount) }} 人练习
          </text>
        </view>
        <button class="bank-btn" @tap.stop="goBankDetail(bank.id)">练习</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onShow } from "@dcloudio/uni-app"
import { getHomeIndex, type HomeVO } from "@/api/home";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const home = ref<HomeVO>();
const userStore = useUserStore();
const appStore = useAppStore();
const nickname = ref("同学");
const greeting = ref("你好");
const isLogin = ref(false);

const fetchHome = async () => {
  await appStore.loadSiteConfig();
  home.value = await getHomeIndex();
  isLogin.value = userStore.isLogin;
  nickname.value = isLogin.value ? userStore.userInfo?.nickname || "同学" : "同学";
  updateGreeting();
};

const updateGreeting = () => {
  const hour = new Date().getHours();
  if (hour < 5) {
    greeting.value = "夜深了";
  } else if (hour < 9) {
    greeting.value = "早安";
  } else if (hour < 12) {
    greeting.value = "上午好";
  } else if (hour < 14) {
    greeting.value = "中午好";
  } else if (hour < 18) {
    greeting.value = "下午好";
  } else if (hour < 22) {
    greeting.value = "晚上好";
  } else {
    greeting.value = "夜深了";
  }
};

const goBankList = () => uni.navigateTo({ url: "/pages/bank/list" });
const goSearch = () => uni.navigateTo({ url: "/pages/search/index" });
const goBankDetail = (id: number) =>
  uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
const goPracticeDaily = () => {
  if (!home.value?.dailyQuestion) return;
  uni.navigateTo({
    url: `/pages/bank/detail?id=${home.value.dailyQuestion.bankId}`
  });
};

onShow(fetchHome);
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.hero {
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-light));
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space);
  color: #ffffff;
}

.hero-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space);
}

.hero-text {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.hero-title {
  font-size: 32rpx;
  font-weight: 700;
}

.hero-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
}

.hero-badge {
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius);
  padding: var(--space-sm) var(--space);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  align-items: center;
  min-width: 110rpx;
  backdrop-filter: blur(8rpx);
}

.badge-label {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.85);
}

.badge-value {
  font-size: 28rpx;
  font-weight: 700;
}

.hero-search {
  background: #ffffff;
  border-radius: var(--radius);
  padding: var(--space) var(--space-lg);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.search-icon {
  font-size: 22rpx;
  background: var(--primary-weak);
  color: var(--primary);
  padding: 6rpx 14rpx;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.search-placeholder {
  color: var(--muted);
  font-size: 26rpx;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
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

.stat-card {
  background: var(--card);
  border-radius: var(--radius);
  padding: var(--space) var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  box-shadow: var(--shadow-sm);
  text-align: center;
}

.stat-value {
  font-size: 30rpx;
  font-weight: 700;
}

.stat-label {
  font-size: 22rpx;
  color: var(--muted);
}

.stat-card.highlight .stat-value {
  color: var(--primary);
}

.daily-card {
  background: var(--warning-weak);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  display: flex;
  align-items: center;
  gap: var(--space);
}

.daily-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: var(--radius);
  background: #fde68a;
  color: #b45309;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 700;
}

.daily-left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  min-width: 0;
}

.daily-tag {
  font-size: 22rpx;
  color: var(--warning);
  font-weight: 600;
}

.daily-content {
  font-size: 26rpx;
  color: #78350f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.daily-empty {
  font-size: 26rpx;
  color: var(--muted);
}

.daily-btn {
  background: var(--warning);
  color: #ffffff;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: var(--radius-full);
  padding: 0 24rpx;
  font-size: 24rpx;
  font-weight: 500;
  flex-shrink: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
}

.section-link {
  font-size: 24rpx;
  color: var(--muted);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: var(--space);
}

.bank-item {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space);
  display: flex;
  gap: var(--space);
  align-items: center;
  box-shadow: var(--shadow);
}

.bank-cover {
  width: 80rpx;
  height: 80rpx;
  border-radius: var(--radius);
  background: var(--primary-weak);
  overflow: hidden;
  flex-shrink: 0;
}

.bank-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  min-width: 0;
}

.bank-name {
  font-size: 28rpx;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta {
  font-size: 22rpx;
  color: var(--muted);
}

.bank-btn {
  background: var(--primary-weak);
  color: var(--primary);
  height: 56rpx;
  line-height: 56rpx;
  border-radius: var(--radius-full);
  padding: 0 24rpx;
  font-size: 24rpx;
  font-weight: 500;
  flex-shrink: 0;
}
</style>
