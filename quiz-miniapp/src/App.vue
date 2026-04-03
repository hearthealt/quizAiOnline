<template>
  <view class="app-root">
    <slot />
    <LoginSheet
      :show="loginSheetVisible"
      :reason-text="loginSheetMessage"
      @close="userStore.closeLoginSheet()"
      @success="handleGlobalLoginSuccess"
    />
  </view>
</template>

<script setup lang="ts">
import { onShow } from "@dcloudio/uni-app";
import { storeToRefs } from "pinia";
import LoginSheet from "@/components/LoginSheet.vue";
import { useUserStore } from "@/stores/user";

const userStore = useUserStore();
const { loginSheetVisible, loginSheetMessage } = storeToRefs(userStore);

const handleGlobalLoginSuccess = async () => {
  userStore.closeLoginSheet();
  await userStore.runPendingLoginAction();
};

onShow(() => {
  if (!userStore.token) return;
  userStore.refreshUser().catch(() => {
    // request.ts will handle 401 and logout
  });
});
</script>

<style lang="scss">
page {
  --primary: #c54c2f;
  --primary-dark: #96321d;
  --primary-light: #dd7a59;
  --primary-weak: #f8e2d8;

  --success: #2f8f63;
  --success-weak: #d9f1e6;
  --warning: #d08a1f;
  --warning-weak: #f9ebc8;
  --danger: #d04a36;
  --danger-weak: #f9ddd7;

  --bg: #f6efe6;
  --bg-page: #f2e9df;
  --bg-soft: #fbf7f2;
  --card: rgba(255, 253, 249, 0.92);
  --card-strong: #fffdfa;

  --text: #241711;
  --text-secondary: #5f4b3f;
  --muted: #9a8778;

  --border: rgba(106, 74, 52, 0.12);
  --border-light: rgba(106, 74, 52, 0.08);

  --shadow-sm: 0 8rpx 18rpx rgba(78, 45, 27, 0.05);
  --shadow: 0 18rpx 36rpx rgba(78, 45, 27, 0.08);
  --shadow-lg: 0 28rpx 64rpx rgba(78, 45, 27, 0.1);

  --radius-sm: 14rpx;
  --radius: 18rpx;
  --radius-lg: 24rpx;
  --radius-xl: 30rpx;
  --radius-full: 999rpx;

  --space-xs: 8rpx;
  --space-sm: 14rpx;
  --space: 18rpx;
  --space-lg: 24rpx;
  --space-xl: 30rpx;

  background:
    radial-gradient(circle at top left, rgba(197, 76, 47, 0.12), transparent 28%),
    radial-gradient(circle at 82% 12%, rgba(208, 138, 31, 0.08), transparent 24%),
    linear-gradient(180deg, #f8f2ea 0%, #f4ebe1 100%);
  color: var(--text);
  font-size: 28rpx;
  line-height: 1.5;
  font-family: "Avenir Next", "PingFang SC", "Helvetica Neue", sans-serif;
}

.app-root {
  min-height: 100vh;
}

.u-card {
  background: var(--card);
  border-radius: var(--radius-xl);
  padding: var(--space-lg);
  box-shadow: var(--shadow);
  border: 1rpx solid var(--border);
}

.u-btn {
  height: 72rpx;
  line-height: 72rpx;
  border-radius: var(--radius-full);
  font-size: 26rpx;
  font-weight: 500;
  text-align: center;
  padding: 0 32rpx;
}

.u-btn-primary {
  background: var(--primary);
  color: #ffffff;
}

.u-btn-ghost {
  background: var(--primary-weak);
  color: var(--primary);
}

.u-btn-outline {
  background: transparent;
  border: 2rpx solid var(--border);
  color: var(--text-secondary);
}

.page-shell {
  padding: 28rpx 24rpx 40rpx;
  min-height: 100vh;
}

.glass-card {
  background: var(--card);
  border: 1rpx solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow);
  backdrop-filter: blur(12rpx);
}
</style>
