<template>
  <div class="bento-page">
    <header v-if="showHeader && hasHeaderActions" class="bento-header bento-header--has-actions">
      <div class="header-actions">
        <slot name="header-actions" />
      </div>
    </header>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

withDefaults(defineProps<{
  showHeader?: boolean
}>(), {
  showHeader: true
})

const slots = useSlots()
const hasHeaderActions = computed(() => Boolean(slots['header-actions']))
</script>
