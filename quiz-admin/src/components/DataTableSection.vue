<template>
  <div
    class="data-table-section"
    :class="{
      'data-table-section--has-search': hasSearch,
      'data-table-section--has-pagination': hasPagination
    }"
  >
    <div v-if="hasSearch" class="search-bar">
      <slot name="search" />
    </div>
    <slot />
    <footer v-if="hasPagination" class="pagination-area">
      <slot name="pagination" />
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed, useSlots } from 'vue'

const slots = useSlots()
const hasSearch = computed(() => Boolean(slots.search))
const hasPagination = computed(() => Boolean(slots.pagination))
</script>
