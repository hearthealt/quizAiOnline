<template>
  <view class="page">
    <view class="hero-card">
      <view class="hero-badge">说明</view>
      <text class="hero-title">功能说明</text>

      <view class="user-info">
        <image v-if="avatar" class="user-avatar" :src="resolveAssetUrl(avatar)" mode="aspectFill" />
        <view v-else class="user-avatar placeholder">Q</view>
        <view class="user-detail">
          <text class="user-name">{{ nickname }}</text>
          <text class="user-status">{{ headerDescription }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">页面用途</text>
      </view>
      <view class="info-list">
        <view v-for="item in featureList" :key="item.title" class="info-item">
          <text class="info-title">{{ item.title }}</text>
          <text class="info-desc">{{ item.desc }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">使用提示</text>
      </view>
      <view class="tips-card">
        <text class="tip-line">1. 答题后可先查看当前页面展示的结果与说明内容。</text>
        <text class="tip-line">2. 学习记录、错题整理和学习助手入口以页面实际开放内容为准。</text>
        <text class="tip-line">3. 如需进一步了解使用方式，可从首页或我的页面再次进入查看。</text>
      </view>
    </view>

    <view v-if="practiceManagerContact" class="section">
      <view class="section-header">
        <text class="section-title">练习管理员</text>
      </view>
      <view class="contact-card">
        <text class="contact-copy">如需进一步说明，可联系练习管理员：</text>
        <text class="contact-value">{{ practiceManagerContact }}</text>
      </view>
    </view>

    <view class="bottom-bar">
      <button class="secondary-btn" @tap="goHome">返回首页</button>
      <button class="primary-btn" @tap="goBack">我知道了</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { computed } from "vue";
import { useAppStore } from "@/stores/app";
import { useUserStore } from "@/stores/user";
import { resolveAssetUrl } from "@/utils/assets";

const userStore = useUserStore();
const appStore = useAppStore();

const nickname = computed(() => userStore.userInfo?.nickname || "微信用户");
const avatar = computed(() => userStore.userInfo?.avatar || "");
const practiceManagerContact = computed(() => appStore.siteConfig.practiceManagerContact || "");
const headerDescription = computed(() =>
  userStore.isLogin
    ? "当前页面用于展示完整解析、学习记录与辅导能力的功能说明。"
    : "登录后可同步学习进度，并查看对应页面的功能说明。"
);

const featureList = [
  {
    title: "完整解析",
    desc: "展示题目结果页、练习记录页和错题整理页中的解析说明与阅读入口。"
  },
  {
    title: "学习记录",
    desc: "用于查看练习、考试和收藏内容，便于后续复盘与整理。"
  },
  {
    title: "学习助手",
    desc: "用于说明学习助手页的使用方式、对话记录与页面能力展示。"
  }
];

const goBack = () => {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
    return;
  }
  uni.switchTab({ url: "/pages/index/index" });
};

const goHome = () => {
  uni.switchTab({ url: "/pages/index/index" });
};

onShow(() => {
  void appStore.loadSiteConfig().catch(() => null);
  if (userStore.isLogin) {
    void userStore.refreshUser().catch(() => null);
  }
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #eef4ff 0%, #ffffff 100%);
  padding: 24rpx 24rpx 180rpx;
}

.hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 28rpx;
  padding: 28rpx;
  background: linear-gradient(135deg, #1d4ed8, #0f172a);
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.18);
}

.hero-badge {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  min-width: 88rpx;
  height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.18);
  color: #eff6ff;
  font-size: 22rpx;
  font-weight: 700;
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #ffffff;
  margin-bottom: 26rpx;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 20rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.1);
}

.user-avatar {
  width: 92rpx;
  height: 92rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  flex-shrink: 0;
}

.user-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 34rpx;
  font-weight: 700;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-name {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: #ffffff;
}

.user-status {
  display: block;
  margin-top: 8rpx;
  font-size: 23rpx;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.82);
}

.section {
  margin-top: 24rpx;
  background: #ffffff;
  border-radius: 22rpx;
  padding: 24rpx;
  box-shadow: 0 10rpx 28rpx rgba(15, 23, 42, 0.06);
}

.section-header {
  margin-bottom: 18rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #0f172a;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.info-item {
  padding: 18rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid #e2e8f0;
}

.info-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #0f172a;
}

.info-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 24rpx;
  line-height: 1.7;
  color: #475569;
}

.tips-card {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  padding: 18rpx;
  border-radius: 18rpx;
  background: #eff6ff;
  color: #1e3a8a;
}

.tip-line {
  font-size: 24rpx;
  line-height: 1.7;
}

.contact-card {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  padding: 18rpx;
  border-radius: 18rpx;
  background: #f8fafc;
  border: 1rpx solid #dbeafe;
}

.contact-copy {
  font-size: 24rpx;
  line-height: 1.7;
  color: #475569;
}

.contact-value {
  font-size: 26rpx;
  line-height: 1.8;
  color: #1d4ed8;
  font-weight: 600;
  word-break: break-all;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx calc(28rpx + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, rgba(255, 255, 255, 0), #ffffff 44%);
}

.primary-btn,
.secondary-btn {
  flex: 1;
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.primary-btn {
  background: #1d4ed8;
  color: #ffffff;
}

.secondary-btn {
  background: #e2e8f0;
  color: #0f172a;
}
</style>
