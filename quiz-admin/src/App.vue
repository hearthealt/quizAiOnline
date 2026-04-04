<template>
  <n-config-provider :theme-overrides="themeOverrides">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <div class="admin-app">
            <router-view />
          </div>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import type { GlobalThemeOverrides } from 'naive-ui'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#b6402c',
    primaryColorHover: '#c64b36',
    primaryColorPressed: '#8e2a19',
    primaryColorSuppl: '#d66a55',
    borderRadius: '18px',
    borderColor: 'rgba(95, 68, 47, 0.14)',
    bodyColor: '#f5f0e8',
    cardColor: 'rgba(255, 252, 247, 0.88)',
    modalColor: '#fffdfa',
    popoverColor: '#fffdfa',
    tableColor: 'rgba(255, 252, 247, 0.88)',
    textColorBase: '#23170f',
    textColor1: '#23170f',
    textColor2: '#6d5949',
    textColor3: '#9f8c7c',
    successColor: '#2f8f63',
    warningColor: '#c07b22',
    errorColor: '#c24832'
  },
  Card: {
    borderRadius: '24px',
    color: 'rgba(255, 252, 247, 0.88)'
  },
  Layout: {
    color: 'transparent',
    siderColor: 'transparent',
    headerColor: 'transparent'
  },
  Menu: {
    borderRadius: '16px',
    itemColorHover: 'rgba(182, 64, 44, 0.04)',
    itemColorActive: 'rgba(182, 64, 44, 0.08)',
    itemTextColor: '#6d5949',
    itemTextColorActive: '#8e2a19',
    itemTextColorHover: '#23170f',
    groupTextColor: '#9f8c7c',
    arrowColor: 'rgba(95, 68, 47, 0.55)'
  }
}
</script>

<style scoped>
.admin-app {
  min-height: 100vh;
}
</style>
