<template>
  <view class="page">
    <view class="card">
      <text class="title">开通成功</text>
      <text class="desc">感谢支持，解析已解锁</text>
      <button class="primary-btn" @tap="backHome">返回首页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
const backHome = () => {
  uni.switchTab({ url: "/pages/index/index" });
};
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 40rpx;
  text-align: center;
  box-shadow: 0 12rpx 40rpx rgba(15, 23, 42, 0.08);
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

.desc {
  font-size: 24rpx;
  color: #6b7280;
  margin: 16rpx 0 24rpx;
}

.primary-btn {
  background: #1a73e8;
  color: #ffffff;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
}
</style>
