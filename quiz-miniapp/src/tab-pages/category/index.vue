<template>
  <view class="page-shell category-page">
    <view class="category-head">
      <view>
        <text class="head-title">题库分类</text>
        <text class="head-sub">按方向筛选，快速进入高频题库</text>
      </view>
      <view class="head-pill">{{ banks.length }} 个题库</view>
    </view>

    <view class="layout">
      <scroll-view class="sidebar glass-card" scroll-y>
        <view
          class="side-item"
          :class="{ active: activeId === 0 }"
          @tap="selectCategory(0, '全部')"
        >
          <text class="side-text">全部</text>
        </view>
        <view
          v-for="item in categories"
          :key="item.id"
          class="side-item"
          :class="{ active: activeId === item.id }"
          @tap="selectCategory(item.id, item.name)"
        >
          <text class="side-text">{{ item.name }}</text>
        </view>
      </scroll-view>

      <scroll-view class="content" scroll-y>
        <view class="content-panel glass-card">
          <view class="content-header">
            <view>
              <text class="content-title">{{ activeName }}</text>
              <text class="content-sub">精选题库与练习热度一并展示</text>
            </view>
          </view>

          <view class="bank-list">
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
              <view v-else class="bank-cover placeholder">题库</view>
              <view class="bank-info">
                <text class="bank-name">{{ bank.name }}</text>
                <text class="bank-meta">{{ bank.questionCount || 0 }}题</text>
                <text class="bank-users">{{ formatCount(bank.practiceCount) }}人练习</text>
              </view>
              <view class="bank-btn">练习</view>
            </view>
          </view>

          <view v-if="banks.length === 0 && !loading" class="empty">
            <text class="empty-icon">题库空</text>
            <text class="empty-text">当前分类暂无题库</text>
          </view>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getBanksByCategory, getCategories, type BankItem, type Category } from "@/api/category";
import { getBankList } from "@/api/bank";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const categories = ref<Category[]>([]);
const activeId = ref<number>(0);
const activeName = ref("全部");
const banks = ref<BankItem[]>([]);
const loading = ref(false);

const fetchCategories = async () => {
  categories.value = await getCategories();
  activeId.value = 0;
  activeName.value = "全部";
  await fetchBanks(0);
};

const fetchBanks = async (categoryId: number) => {
  loading.value = true;
  try {
    const res = categoryId === 0
      ? await getBankList(undefined, 1, 50)
      : await getBanksByCategory(categoryId, 1, 50);
    banks.value = res.list || [];
  } finally {
    loading.value = false;
  }
};

const selectCategory = (id: number, name: string) => {
  if (activeId.value === id) return;
  activeId.value = id;
  activeName.value = name;
  void fetchBanks(id).catch(() => null);
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
};

onShow(() => {
  void fetchCategories().catch(() => null);
});
</script>

<style lang="scss" scoped>
.category-page {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.category-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16rpx;
}

.head-title {
  display: block;
  font-size: 34rpx;
  font-weight: 700;
}

.head-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.head-pill {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: var(--card);
  border: 1rpx solid var(--border);
  font-size: 22rpx;
  color: var(--text-secondary);
}

.layout {
  display: flex;
  gap: 16rpx;
  min-height: calc(100vh - 180rpx);
}

.sidebar {
  width: 196rpx;
  padding: 14rpx;
  flex-shrink: 0;
}

.side-item {
  padding: 18rpx 14rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10rpx;
}

.side-item.active {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: #fff;
  box-shadow: var(--shadow-sm);
}

.side-text {
  font-size: 24rpx;
  font-weight: 700;
  text-align: center;
}

.content {
  flex: 1;
  min-width: 0;
}

.content-panel {
  padding: 18rpx;
  min-height: 100%;
}

.content-title {
  display: block;
  font-size: 30rpx;
  font-weight: 700;
}

.content-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
  margin-top: 18rpx;
}

.bank-card {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 18rpx;
  border-radius: 24rpx;
  background: rgba(255,255,255,0.58);
  border: 1rpx solid var(--border);
}

.bank-cover {
  width: 88rpx;
  height: 88rpx;
  border-radius: 22rpx;
  overflow: hidden;
  background: var(--primary-weak);
  flex-shrink: 0;
}

.bank-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-dark);
  font-size: 22rpx;
  font-weight: 700;
}

.bank-info {
  flex: 1;
  min-width: 0;
}

.bank-name {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta,
.bank-users {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.bank-btn {
  padding: 12rpx 20rpx;
  border-radius: 999rpx;
  background: var(--primary);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100rpx 0 60rpx;
}

.empty-icon {
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  background: var(--primary-weak);
  color: var(--primary-dark);
  font-size: 26rpx;
  font-weight: 700;
}

.empty-text {
  margin-top: 18rpx;
  font-size: 24rpx;
  color: var(--muted);
}
</style>
