<template>
  <view class="page">
    <view class="header">
      <text class="title">全部题库</text>
      <text class="subtitle">选择想练习的题库</text>
    </view>
    <view class="bank-list">
      <view
        v-for="bank in banks"
        :key="bank.id"
        class="bank-item"
        @tap="goDetail(bank.id)"
      >
        <image
          v-if="bank.cover"
          class="bank-cover"
          :src="resolveAssetUrl(bank.cover)"
          mode="aspectFill"
        />
        <view v-else class="bank-cover" />
        <view class="bank-info">
          <text class="bank-name">{{ bank.name }}</text>
          <text class="bank-meta">
            {{ bank.questionCount || 0 }} 题 · {{ formatCount(bank.practiceCount) }} 人练习
          </text>
        </view>
        <button class="bank-btn" @tap.stop="goDetail(bank.id)">练习</button>
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
  const res = await getBankList(undefined, pageNum.value, 10);
  banks.value = banks.value.concat(res.list || []);
  total.value = res.total;
  loadStatus.value = banks.value.length >= total.value ? "nomore" : "more";
  pageNum.value += 1;
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/bank/detail?id=${id}` });
};

onLoad(fetchBanks);
onReachBottom(fetchBanks);
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl);
}

.header {
  margin-bottom: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

.subtitle {
  font-size: 24rpx;
  color: var(--muted);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: var(--space);
}

.bank-item {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space);
  display: flex;
  gap: var(--space);
  align-items: center;
  box-shadow: var(--shadow);
}

.bank-cover {
  width: 80rpx;
  height: 80rpx;
  border-radius: var(--radius);
  background: var(--primary-weak);
  overflow: hidden;
  flex-shrink: 0;
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
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bank-meta {
  font-size: 22rpx;
  color: var(--muted);
}

.bank-btn {
  background: var(--primary-weak);
  color: var(--primary);
  height: 56rpx;
  line-height: 56rpx;
  border-radius: var(--radius-full);
  padding: 0 24rpx;
  font-size: 24rpx;
  font-weight: 500;
  flex-shrink: 0;
}
</style>
