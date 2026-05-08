<template>
  <div class="data-table-section">
    <div v-if="$slots.search" class="search-bar">
      <slot name="search" />
    </div>
    <slot />
    <footer v-if="$slots.pagination" class="pagination-area">
      <slot name="pagination" />
    </footer>
  </div>
</template>
