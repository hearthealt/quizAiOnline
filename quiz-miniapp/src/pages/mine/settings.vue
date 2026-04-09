<template>
  <view class="page">
    <view class="card">
      <text class="title">协议与隐私</text>
      <view class="setting-link" @tap="openServiceAgreement">
        <view class="link-copy">
          <text class="link-title">用户服务协议</text>
          <text class="link-desc">查看服务规则、账号使用和用户信息说明</text>
        </view>
        <text class="link-arrow">›</text>
      </view>
      <view class="setting-link" @tap="openPrivacyPolicy">
        <view class="link-copy">
          <text class="link-title">隐私政策</text>
          <text class="link-desc">查看手机号、头像、昵称和学习记录的收集与使用规则</text>
        </view>
        <text class="link-arrow">›</text>
      </view>
      <view class="setting-item status-item">
        <text>当前授权状态</text>
        <text class="status-chip" :class="{ active: consentAccepted }">
          {{ consentAccepted ? "已同意" : "未确认" }}
        </text>
      </view>
    </view>

    <view v-if="isLogin" class="card">
      <text class="title">答题设置</text>
      <view class="setting-item">
        <text>答对自动下一题</text>
        <switch :checked="autoNext" @change="toggleAuto" />
      </view>
    </view>

    <view class="card">
      <button class="ghost-btn" :class="{ compact: !isLogin }" @tap="clearCache">清除缓存</button>
      <button v-if="isLogin" class="danger-btn" @tap="doLogout">退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { useAppStore } from "@/stores/app";
import { useUserStore } from "@/stores/user";
import { updateSettings } from "@/api/user";
import { hasLegalConsent, openAgreementPage } from "@/utils/privacy";

const appStore = useAppStore();
const userStore = useUserStore();
const consentAccepted = ref(hasLegalConsent());
const isLogin = computed(() => userStore.isLogin);

const autoNext = computed(() => appStore.settings.autoNext);

const toggleAuto = async (e: any) => {
  appStore.setSettings({ autoNext: e.detail.value });
  await updateSettings(appStore.settings);
};

const clearCache = () => {
  uni.clearStorageSync();
  consentAccepted.value = hasLegalConsent();
  uni.showToast({ title: "已清除", icon: "none" });
};

const doLogout = () => {
  userStore.logout();
  uni.switchTab({ url: "/tab-pages/mine/index" });
};

const openServiceAgreement = () => {
  openAgreementPage("service");
};

const openPrivacyPolicy = () => {
  openAgreementPage("privacy");
};

onShow(() => {
  consentAccepted.value = hasLegalConsent();
});
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

.setting-link {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid rgba(15, 23, 42, 0.06);
}

.setting-link:last-of-type {
  border-bottom: 0;
}

.link-copy {
  flex: 1;
  min-width: 0;
}

.link-title {
  display: block;
  font-size: 27rpx;
  font-weight: 600;
  color: #111827;
}

.link-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #6b7280;
}

.link-arrow {
  font-size: 30rpx;
  color: #9ca3af;
}

.status-item {
  margin-top: 12rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid rgba(15, 23, 42, 0.06);
}

.status-chip {
  min-width: 108rpx;
  height: 48rpx;
  line-height: 48rpx;
  text-align: center;
  border-radius: 999rpx;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 22rpx;
  font-weight: 600;
}

.status-chip.active {
  background: #e8f6ee;
  color: #1f8a55;
}

.ghost-btn {
  background: #f3f4f6;
  color: #4b5563;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}

.ghost-btn.compact {
  margin-bottom: 0;
}

.danger-btn {
  background: #fee2e2;
  color: #b91c1c;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 16rpx;
}
</style>
