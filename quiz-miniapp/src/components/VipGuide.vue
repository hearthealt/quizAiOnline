<template>
  <view v-if="show" class="vip-mask" @tap="close">
    <view class="vip-card" @tap.stop>
      <text class="vip-title">VIP专享内容</text>
      <text class="vip-desc">开通VIP解锁完整解析、错题加速练习等权益</text>
      <view class="vip-actions">
        <button class="vip-btn ghost" @tap="close">稍后</button>
        <button class="vip-btn" @tap="goVip">立即开通</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  show: boolean;
}>();

const emit = defineEmits<{
  (e: "close"): void;
}>();

const close = () => emit("close");

const goVip = () => {
  emit("close");
  uni.navigateTo({ url: "/pages/vip/index" });
};
</script>

<style lang="scss" scoped>
.vip-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.vip-card {
  width: 560rpx;
  background: #ffffff;
  border-radius: 24rpx;
  padding: 32rpx;
}

.vip-title {
  font-size: 32rpx;
  font-weight: 700;
  margin-bottom: 12rpx;
}

.vip-desc {
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.6;
}

.vip-actions {
  display: flex;
  justify-content: flex-end;
  gap: 16rpx;
  margin-top: 24rpx;
}

.vip-btn {
  background: #1a73e8;
  color: #ffffff;
  font-size: 26rpx;
  padding: 0 32rpx;
  border-radius: 999rpx;
  height: 64rpx;
  line-height: 64rpx;
}

.vip-btn.ghost {
  background: #f3f4f6;
  color: #4b5563;
}
</style>
