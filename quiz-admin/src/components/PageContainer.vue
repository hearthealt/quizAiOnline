<template>
  <div class="bento-page">
    <header v-if="showHeader" class="bento-header">
      <div>
        <h2>{{ title }}</h2>
        <span v-if="subtitle" class="subtitle">{{ subtitle }}</span>
      </div>
      <div class="header-actions">
        <slot name="header-actions" />
      </div>
    </header>
    <slot />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  title?: string
  subtitle?: string
  showHeader?: boolean
}>(), {
  showHeader: true
})
</script>
