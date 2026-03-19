<template>
  <view class="analysis">
    <view class="analysis-header">
      <text class="analysis-title">题目解析</text>
      <text v-if="isVip" class="analysis-tag">VIP</text>
    </view>
    <view v-if="!isVip" class="vip-tip">
      <text class="vip-text">开通VIP解锁完整解析</text>
      <button class="vip-btn" @tap="goVip">立即开通</button>
    </view>
    <view class="analysis-content" :class="{ blur: !isVip }">
      <text v-if="analysis">{{ analysis }}</text>
      <text v-else>暂无解析</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  analysis?: string;
  isVip?: boolean;
}>();

const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};
</script>

<style lang="scss" scoped>
.analysis {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.analysis-title {
  font-size: 30rpx;
  font-weight: 600;
}

.analysis-tag {
  font-size: 22rpx;
  color: #d97706;
  background: #fef3c7;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}

.analysis-content {
  font-size: 26rpx;
  color: #4b5563;
  line-height: 1.6;
}

.analysis-content.blur {
  filter: blur(8rpx);
}

.vip-tip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 16rpx;
  border-radius: 16rpx;
  background: #fffbeb;
  border: 2rpx solid #fde68a;
}

.vip-text {
  font-size: 24rpx;
  color: #92400e;
}

.vip-btn {
  background: #f59e0b;
  color: #ffffff;
  border-radius: 999rpx;
  padding: 0 28rpx;
  height: 56rpx;
  line-height: 56rpx;
  font-size: 24rpx;
}
</style>
