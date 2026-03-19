<template>
  <view class="sheet">
    <view class="sheet-header">
      <text class="sheet-title">答题卡</text>
      <text class="sheet-sub">已答 {{ answered }}/{{ total }}</text>
    </view>
    <view class="sheet-grid">
      <view
        v-for="idx in total"
        :key="idx"
        class="sheet-item"
        :class="{ done: answeredSet.has(idx - 1) }"
        @tap="emit('select', idx - 1)"
      >
        <text>{{ idx }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  total: number;
  answered: number;
  answeredSet: Set<number>;
}>();

const emit = defineEmits<{
  (e: "select", index: number): void;
}>();
</script>

<style lang="scss" scoped>
.sheet {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.sheet-title {
  font-size: 30rpx;
  font-weight: 600;
}

.sheet-sub {
  font-size: 24rpx;
  color: #6b7280;
}

.sheet-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12rpx;
}

.sheet-item {
  height: 64rpx;
  border-radius: 12rpx;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #6b7280;
  font-size: 24rpx;
}

.sheet-item.done {
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 600;
}
</style>
