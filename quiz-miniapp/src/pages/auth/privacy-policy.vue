<template>
  <view class="page-shell legal-page">
    <view class="hero-card glass-card">
      <text class="hero-title">隐私政策</text>
      <text class="hero-subtitle">{{ appName }}</text>
      <text class="hero-meta">更新时间：{{ LEGAL_UPDATED_AT }}</text>
    </view>

    <view v-for="section in sections" :key="section.title" class="section-card glass-card">
      <text class="section-title">{{ section.title }}</text>
      <text v-for="paragraph in section.paragraphs" :key="paragraph" class="section-text">
        {{ paragraph }}
      </text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { LEGAL_DEFAULT_APP_NAME, LEGAL_UPDATED_AT, getPrivacyPolicySections } from "./legal";
import { useAppStore } from "@/stores/app";

const appStore = useAppStore();

const appName = computed(() => appStore.siteConfig.siteName || LEGAL_DEFAULT_APP_NAME);
const sections = computed(() => getPrivacyPolicySections(appName.value));

onMounted(() => {
  void appStore.loadSiteConfig().catch(() => null);
});
</script>

<style lang="scss" scoped>
.legal-page {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.hero-card,
.section-card {
  padding: 26rpx 24rpx;
}

.hero-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: var(--text);
}

.hero-subtitle,
.hero-meta {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  color: var(--muted);
}

.section-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text);
}

.section-text {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  line-height: 1.9;
  color: var(--text-secondary);
}
</style>
