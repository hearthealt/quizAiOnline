<template>
  <view class="app-root">
    <slot />
  </view>
</template>

<script setup lang="ts"></script>

<style lang="scss">
page {
  /* 主题色 */
  --primary: #6366f1;
  --primary-dark: #4f46e5;
  --primary-light: #818cf8;
  --primary-weak: #eef2ff;

  /* 功能色 */
  --success: #10b981;
  --success-weak: #d1fae5;
  --warning: #f59e0b;
  --warning-weak: #fef3c7;
  --danger: #ef4444;
  --danger-weak: #fee2e2;

  /* 背景色 */
  --bg: #f8fafc;
  --bg-page: #f1f5f9;
  --card: #ffffff;

  /* 文字色 */
  --text: #0f172a;
  --text-secondary: #475569;
  --muted: #94a3b8;

  /* 边框 */
  --border: #e2e8f0;
  --border-light: #f1f5f9;

  /* 阴影 */
  --shadow-sm: 0 2rpx 8rpx rgba(15, 23, 42, 0.04);
  --shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
  --shadow-lg: 0 16rpx 40rpx rgba(15, 23, 42, 0.08);

  /* 圆角 */
  --radius-sm: 12rpx;
  --radius: 16rpx;
  --radius-lg: 20rpx;
  --radius-xl: 24rpx;
  --radius-full: 999rpx;

  /* 间距 */
  --space-xs: 8rpx;
  --space-sm: 12rpx;
  --space: 16rpx;
  --space-lg: 20rpx;
  --space-xl: 24rpx;

  background-color: var(--bg);
  color: var(--text);
  font-size: 28rpx;
  line-height: 1.5;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Helvetica Neue", Arial, sans-serif;
}

.app-root {
  min-height: 100vh;
}

/* 通用卡片样式 */
.u-card {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow);
}

/* 通用按钮样式 */
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
</style>
