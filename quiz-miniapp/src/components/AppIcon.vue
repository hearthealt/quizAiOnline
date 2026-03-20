<template>
  <text class="icon" :style="{ color: color, fontSize: size + 'rpx' }">{{ iconMap[name] || '●' }}</text>
</template>

<script setup lang="ts">
defineProps<{
  name: string
  color?: string
  size?: number
}>()

// 使用Unicode字符作为图标
const iconMap: Record<string, string> = {
  'search': '🔍',
  'edit': '✏️',
  'person': '👤',
  'setting': '⚙️',
  'star': '⭐',
  'gift': '🎁',
  'close': '✕',
  'error': '✗',
  'right': '›',
  'home': '🏠',
  'category': '📚',
  'ai': '🤖',
  'vip': '👑'
}
</script>

<style scoped>
.icon {
  font-style: normal;
  line-height: 1;
}
</style>
