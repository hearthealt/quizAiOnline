<template>
  <view class="page-shell bank-page">
    <view class="page-head">
      <view>
        <text class="head-title">全部题库</text>
        <text class="head-sub">按练习热度和题量快速挑选学习入口</text>
      </view>
      <view class="head-badge">{{ total }} 个题库</view>
    </view>

    <view class="bank-list">
      <view
        v-for="bank in banks"
        :key="bank.id"
        class="bank-card glass-card"
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
          <text class="bank-meta">{{ bank.questionCount || 0 }} 题</text>
          <text class="bank-users">{{ formatCount(bank.practiceCount) }} 人练习</text>
        </view>
        <view class="bank-btn">进入</view>
      </view>
    </view>

    <LoadMore :status="loadStatus" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom } from "@dcloudio/uni-app";
import { ref } from "vue";
import { getBankList, type BankItem } from "@/api/bank";
import LoadMore from "@/components/LoadMore.vue";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const banks = ref<BankItem[]>([]);
const pageNum = ref(1);
const total = ref(0);
const loadStatus = ref<"more" | "loading" | "nomore">("more");

const fetchBanks = async () => {
  if (loadStatus.value === "loading" || loadStatus.value === "nomore") return;
  loadStatus.value = "loading";
  try {
    const res = await getBankList(undefined, pageNum.value, 10);
    banks.value = banks.value.concat(res.list || []);
    total.value = res.total;
    loadStatus.value = banks.value.length >= total.value ? "nomore" : "more";
    pageNum.value += 1;
  } catch (error) {
    loadStatus.value = "more";
    throw error;
  }
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
};

onLoad(() => {
  void fetchBanks().catch(() => null);
});
onReachBottom(() => {
  void fetchBanks().catch(() => null);
});
</script>

<style lang="scss" scoped>
.bank-page {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.page-head {
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

.head-badge {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: var(--card);
  border: 1rpx solid var(--border);
  font-size: 22rpx;
  color: var(--text-secondary);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bank-card {
  padding: 18rpx;
  display: flex;
  gap: 16rpx;
  align-items: center;
}

.bank-cover {
  width: 90rpx;
  height: 90rpx;
  border-radius: 24rpx;
  background: var(--primary-weak);
  overflow: hidden;
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
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  min-width: 0;
}

.bank-name {
  font-size: 28rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta,
.bank-users {
  font-size: 22rpx;
  color: var(--muted);
}

.bank-btn {
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  background: var(--primary);
  color: #fff;
  font-size: 22rpx;
  font-weight: 700;
  flex-shrink: 0;
}
</style>
