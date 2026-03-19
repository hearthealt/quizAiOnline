<template>
  <view
    class="option"
    :class="{ selected, correct, wrong, disabled }"
    @tap="handleClick"
  >
    <view class="option-label">
      <text>{{ label }}</text>
    </view>
    <view class="option-content">
      <text>{{ content }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
const props = defineProps<{
  label: string;
  content: string;
  selected?: boolean;
  correct?: boolean;
  wrong?: boolean;
  disabled?: boolean;
}>();

const emit = defineEmits<{
  (e: "select"): void;
}>();

const handleClick = () => {
  if (props.disabled) return;
  emit("select");
};
</script>

<style lang="scss" scoped>
.option {
  border: 2rpx solid #e5e7eb;
  border-radius: 18rpx;
  padding: 18rpx;
  display: flex;
  gap: 16rpx;
  background: #ffffff;
}

.option-label {
  width: 52rpx;
  height: 52rpx;
  border-radius: 26rpx;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #6b7280;
}

.option-content {
  flex: 1;
  font-size: 26rpx;
  color: #374151;
  line-height: 1.5;
}

.option.selected {
  border-color: #93c5fd;
  background: #eff6ff;
}

.option.correct {
  border-color: #86efac;
  background: #ecfdf5;
}

.option.wrong {
  border-color: #fca5a5;
  background: #fef2f2;
}

.option.disabled {
  opacity: 0.6;
}
</style>
