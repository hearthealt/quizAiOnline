<template>
  <view class="load-more">
    <text v-if="status === 'loading'">加载中...</text>
    <text v-else-if="status === 'nomore'">没有更多了</text>
    <text v-else>上拉加载更多</text>
  </view>
</template>

<script setup lang="ts">
defineProps<{
  status: "more" | "loading" | "nomore";
}>();
</script>

<style lang="scss" scoped>
.load-more {
  padding: 24rpx;
  text-align: center;
  color: #9aa0a6;
  font-size: 24rpx;
}
</style>
