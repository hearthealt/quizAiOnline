<template>
  <view class="page">
    <view class="layout">
      <scroll-view class="sidebar" scroll-y>
        <view
          v-for="item in categories"
          :key="item.id"
          class="side-item"
          :class="{ active: item.id === activeId }"
          @tap="selectCategory(item.id)"
        >
          <view class="side-bar" />
          <image
            v-if="item.icon"
            class="side-icon"
            :src="resolveAssetUrl(item.icon)"
            mode="aspectFit"
          />
          <text class="side-text">{{ item.name }}</text>
        </view>
      </scroll-view>

      <scroll-view class="content" scroll-y>
        <view class="content-header">
          <text class="content-title">{{ activeName }}</text>
          <text class="content-sub">精选题库 · 即刻开练</text>
        </view>
        <view class="bank-grid">
          <view
            v-for="bank in banks"
            :key="bank.id"
            class="bank-card"
            @tap="goDetail(bank.id)"
          >
            <image
              v-if="bank.cover"
              class="bank-cover"
              :src="resolveAssetUrl(bank.cover)"
              mode="aspectFill"
            />
            <view v-else class="bank-cover" />
            <view class="bank-body">
              <text class="bank-name">{{ bank.name }}</text>
              <view class="bank-meta">
                <text>{{ bank.questionCount || 0 }} 题</text>
                <text>{{ formatCount(bank.practiceCount) }} 人练习</text>
              </view>
              <view class="bank-action">
                <text>开始练习</text>
              </view>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app"
import { getBanksByCategory, getCategories, type BankItem, type Category } from "@/api/category";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const categories = ref<Category[]>([]);
const activeId = ref<number>(0);
const activeName = ref("题库");
const banks = ref<BankItem[]>([]);

const fetchCategories = async () => {
  categories.value = await getCategories();
  if (categories.value.length > 0) {
    activeId.value = categories.value[0].id;
    activeName.value = categories.value[0].name;
    fetchBanks(activeId.value);
  }
};

const fetchBanks = async (categoryId: number) => {
  const res = await getBanksByCategory(categoryId, 1, 20);
  banks.value = res.list || [];
};

const selectCategory = (id: number) => {
  if (activeId.value === id) return;
  activeId.value = id;
  const current = categories.value.find((item) => item.id === id);
  activeName.value = current?.name || "题库";
  fetchBanks(id);
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
};

onShow(fetchCategories);
</script>

<style lang="scss" scoped>
.page {
  padding: 0;
}

.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 200rpx;
  height: 100vh;
  background: var(--card);
  border-right: 1rpx solid var(--border-light);
  padding: var(--space) 0;
  box-sizing: border-box;
}

.side-item {
  position: relative;
  padding: 24rpx 16rpx 24rpx 20rpx;
  color: var(--text-secondary);
  font-size: 26rpx;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  transition: all 0.2s;
}

.side-item.active {
  color: var(--primary);
  font-weight: 600;
  background: var(--primary-weak);
}

.side-bar {
  position: absolute;
  left: 0;
  top: 50%;
  width: 6rpx;
  height: 40rpx;
  border-radius: var(--radius-full);
  background: transparent;
  transform: translateY(-50%);
}

.side-item.active .side-bar {
  background: var(--primary);
}

.side-text {
  flex: 1;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-icon {
  width: 36rpx;
  height: 36rpx;
  border-radius: var(--radius-sm);
  flex-shrink: 0;
}

.content {
  flex: 1;
  height: 100vh;
  padding: var(--space-lg);
  background: var(--bg);
  box-sizing: border-box;
}

.content-header {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  margin-bottom: var(--space);
}

.content-title {
  font-size: 30rpx;
  font-weight: 700;
}

.content-sub {
  font-size: 22rpx;
  color: var(--muted);
}

.bank-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-sm);
}

.bank-card {
  background: var(--card);
  border-radius: var(--radius-lg);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow);
}

.bank-cover {
  width: 100%;
  height: 100rpx;
  background: var(--primary-weak);
}

.bank-body {
  padding: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.bank-name {
  font-size: 24rpx;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta {
  display: flex;
  justify-content: space-between;
  font-size: 20rpx;
  color: var(--muted);
}

.bank-action {
  background: var(--primary-weak);
  color: var(--primary);
  font-size: 22rpx;
  border-radius: var(--radius-full);
  height: 44rpx;
  line-height: 44rpx;
  text-align: center;
  font-weight: 500;
  margin-top: 4rpx;
}
</style>
