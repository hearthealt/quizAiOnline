<template>
  <view
    class="swipe-panel"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
    @touchcancel="resetTouch"
  >
    <slot />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";

const props = withDefaults(defineProps<{
  disabled?: boolean;
  minDistance?: number;
  ratio?: number;
}>(), {
  disabled: false,
  minDistance: 60,
  ratio: 1.2
});

const emit = defineEmits<{
  (e: "swipe-left"): void;
  (e: "swipe-right"): void;
}>();

const startX = ref<number | null>(null);
const startY = ref<number | null>(null);

const resetTouch = () => {
  startX.value = null;
  startY.value = null;
};

const handleTouchStart = (event: TouchEvent) => {
  if (props.disabled) return;
  const touch = event.changedTouches?.[0];
  if (!touch) return;
  startX.value = touch.clientX;
  startY.value = touch.clientY;
};

const handleTouchEnd = (event: TouchEvent) => {
  if (props.disabled || startX.value == null || startY.value == null) {
    resetTouch();
    return;
  }
  const touch = event.changedTouches?.[0];
  if (!touch) {
    resetTouch();
    return;
  }

  const deltaX = touch.clientX - startX.value;
  const deltaY = touch.clientY - startY.value;
  const absX = Math.abs(deltaX);
  const absY = Math.abs(deltaY);

  if (absX >= props.minDistance && absX > absY * props.ratio) {
    if (deltaX < 0) {
      emit("swipe-left");
    } else {
      emit("swipe-right");
    }
  }

  resetTouch();
};
</script>

<style scoped>
.swipe-panel {
  display: flex;
  flex-direction: column;
  gap: inherit;
  min-height: 0;
}
</style>
