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
    itemColorHover: 'rgba(255,255,255,0.06)',
    itemColorActive: 'rgba(243, 215, 200, 0.22)',
    itemTextColor: 'rgba(255,255,255,0.7)',
    itemTextColorActive: '#fff7f0',
    itemTextColorHover: '#fff7f0',
    arrowColor: 'rgba(255,255,255,0.65)'
  }
}
</script>

<style scoped>
.admin-app {
  min-height: 100vh;
}
</style>
