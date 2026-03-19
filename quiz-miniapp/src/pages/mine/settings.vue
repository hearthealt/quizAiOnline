<template>
  <view class="page">
    <view class="card">
      <text class="title">答题设置</text>
      <view class="setting-item">
        <text>答对自动下一题</text>
        <switch :checked="autoNext" @change="toggleAuto" />
      </view>
    </view>

    <view class="card">
      <button class="ghost-btn" @tap="clearCache">清除缓存</button>
      <button class="danger-btn" @tap="doLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useAppStore } from "@/stores/app";
import { useUserStore } from "@/stores/user";
import { updateSettings } from "@/api/user";

const appStore = useAppStore();
const userStore = useUserStore();

const autoNext = computed(() => appStore.settings.autoNext);

const toggleAuto = async (e: any) => {
  appStore.setSettings({ autoNext: e.detail.value });
  await updateSettings(appStore.settings);
};

const clearCache = () => {
  uni.clearStorageSync();
  uni.showToast({ title: "已清除", icon: "none" });
};

const doLogout = () => {
  userStore.logout();
  uni.switchTab({ url: "/pages/mine/index" });
};
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 12rpx 40rpx rgba(15, 23, 42, 0.08);
}

.title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  font-size: 26rpx;
}

.ghost-btn {
  background: #f3f4f6;
  color: #4b5563;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}

.danger-btn {
  background: #fee2e2;
  color: #b91c1c;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 16rpx;
}
</style>
