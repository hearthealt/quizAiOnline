<template>
  <view class="empty">
    <view class="empty-image" />
    <text class="empty-title">{{ title }}</text>
    <text v-if="description" class="empty-desc">{{ description }}</text>
    <view v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  title?: string;
  description?: string;
}>();
</script>

<style lang="scss" scoped>
.empty {
  padding: 80rpx 40rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #9aa0a6;
}

.empty-image {
  width: 220rpx;
  height: 220rpx;
  margin-bottom: 24rpx;
  border-radius: 110rpx;
  background: linear-gradient(135deg, #e0e7ff, #f3e8ff);
}

.empty-title {
  font-size: 30rpx;
  color: #5f6368;
}

.empty-desc {
  font-size: 24rpx;
  margin-top: 12rpx;
}

.empty-action {
  margin-top: 24rpx;
}
</style>
