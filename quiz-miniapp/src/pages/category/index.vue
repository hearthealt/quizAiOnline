<template>
  <view class="page">
    <view class="layout">
      <!-- 左侧分类 -->
      <scroll-view class="sidebar" scroll-y>
        <view 
          class="side-item"
          :class="{ active: activeId === 0 }"
          @tap="selectCategory(0, '全部')"
        >
          <view class="side-bar" />
          <text class="side-text">全部</text>
        </view>
        <view 
          v-for="item in categories" 
          :key="item.id" 
          class="side-item"
          :class="{ active: activeId === item.id }"
          @tap="selectCategory(item.id, item.name)"
        >
          <view class="side-bar" />
          <text class="side-text">{{ item.name }}</text>
        </view>
      </scroll-view>

      <!-- 右侧题库列表 -->
      <scroll-view class="content" scroll-y>
        <view class="content-header">
          <text class="content-title">{{ activeName }}</text>
          <text class="content-count">共 {{ banks.length }} 个题库</text>
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
            <view v-else class="bank-cover placeholder">📚</view>
            <view class="bank-info">
              <text class="bank-name">{{ bank.name }}</text>
              <text class="bank-meta">{{ bank.questionCount || 0 }}题</text>
              <text class="bank-users">{{ formatCount(bank.practiceCount) }}人练习</text>
            </view>
            <view class="bank-btn">练习</view>
          </view>
        </view>

        <!-- 空状态 -->
        <view v-if="banks.length === 0 && !loading" class="empty">
          <text class="empty-icon">📭</text>
          <text class="empty-text">暂无题库</text>
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getBanksByCategory, getCategories, type BankItem, type Category } from "@/api/category";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const categories = ref<Category[]>([]);
const activeId = ref<number>(0);
const activeName = ref("全部");
const banks = ref<BankItem[]>([]);
const loading = ref(false);

const fetchCategories = async () => {
  categories.value = await getCategories();
  // 默认获取全部题库
  activeId.value = 0;
  activeName.value = "全部";
  fetchBanks(0);
};

const fetchBanks = async (categoryId: number) => {
  loading.value = true;
  try {
    const res = await getBanksByCategory(categoryId, 1, 50);
    banks.value = res.list || [];
  } finally {
    loading.value = false;
  }
};

const selectCategory = (id: number, name: string) => {
  if (activeId.value === id) return;
  activeId.value = id;
  activeName.value = name;
  fetchBanks(id);
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
};

onShow(fetchCategories);
</script>

<style lang="scss" scoped>
.page {
  height: 100vh;
  background: var(--bg);
}

.layout {
  display: flex;
  height: 100vh;
}

.sidebar {
  width: 180rpx;
  height: 100vh;
  background: var(--card);
  flex-shrink: 0;
}

.side-item {
  position: relative;
  padding: 28rpx 16rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
  text-align: center;
}

.side-item.active {
  color: var(--primary);
  font-weight: 600;
  background: var(--bg);
}

.side-bar {
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6rpx;
  height: 36rpx;
  border-radius: 0 6rpx 6rpx 0;
  background: transparent;
}

.side-item.active .side-bar {
  background: var(--primary);
}

.side-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  flex: 1;
  height: 100vh;
  padding: 20rpx;
  box-sizing: border-box;
}

.content-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.content-title {
  font-size: 32rpx;
  font-weight: 600;
  color: var(--text);
}

.content-count {
  font-size: 24rpx;
  color: var(--muted);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bank-card {
  background: var(--card);
  border-radius: 16rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
  box-shadow: var(--shadow-sm);
}

.bank-cover {
  width: 80rpx;
  height: 80rpx;
  border-radius: 12rpx;
  background: var(--primary-weak);
  flex-shrink: 0;
}

.bank-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32rpx;
}

.bank-info {
  flex: 1;
  min-width: 0;
}

.bank-name {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text);
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta {
  font-size: 22rpx;
  color: var(--muted);
  margin-top: 6rpx;
  display: inline;
}

.bank-users {
  font-size: 22rpx;
  color: var(--muted);
  margin-left: 12rpx;
}

.bank-btn {
  padding: 12rpx 24rpx;
  background: var(--primary);
  color: #fff;
  border-radius: 24rpx;
  font-size: 24rpx;
  flex-shrink: 0;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80rpx 0;
}

.empty-icon {
  font-size: 64rpx;
  margin-bottom: 16rpx;
}

.empty-text {
  font-size: 26rpx;
  color: var(--muted);
}
</style>
