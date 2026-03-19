<template>
  <view class="timer">
    <text class="timer-label">剩余</text>
    <text class="timer-value">{{ formatted }}</text>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
  seconds: number;
}>();

const formatted = computed(() => {
  const minutes = Math.floor(props.seconds / 60);
  const secs = props.seconds % 60;
  const mm = String(minutes).padStart(2, "0");
  const ss = String(secs).padStart(2, "0");
  return `${mm}:${ss}`;
});
</script>

<style lang="scss" scoped>
.timer {
  background: #111827;
  color: #ffffff;
  border-radius: 999rpx;
  padding: 8rpx 20rpx;
  display: inline-flex;
  align-items: center;
  gap: 10rpx;
  font-size: 24rpx;
}

.timer-value {
  font-weight: 600;
  font-size: 26rpx;
}
</style>
