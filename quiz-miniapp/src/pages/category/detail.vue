<template>
  <view class="page">
    <view class="header">
      <text class="title">{{ name }}</text>
      <text class="subtitle">精选题库推荐</text>
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
import { getBanksByCategory, type BankItem } from "@/api/category";
import LoadMore from "@/components/LoadMore.vue";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const id = ref<number>(0);
const name = ref("");
const banks = ref<BankItem[]>([]);
const pageNum = ref(1);
const total = ref(0);
const loadStatus = ref<"more" | "loading" | "nomore">("more");

const fetchBanks = async () => {
  if (loadStatus.value === "loading" || loadStatus.value === "nomore") return;
  loadStatus.value = "loading";
  const res = await getBanksByCategory(id.value, pageNum.value, 10);
  banks.value = banks.value.concat(res.list || []);
  total.value = res.total;
  loadStatus.value = banks.value.length >= total.value ? "nomore" : "more";
  pageNum.value += 1;
};

const goDetail = (bankId: number) => {
  uni.navigateTo({ url: `/pages/bank/detail?id=${bankId}` });
};

onLoad((query: any) => {
  id.value = Number(query.id || 0);
  name.value = String(query.name || "");
  fetchBanks();
});

onReachBottom(fetchBanks);
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
}

.header {
  margin-bottom: 20rpx;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
}

.subtitle {
  font-size: 22rpx;
  color: var(--muted);
}

.bank-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.bank-item {
  background: var(--card);
  border-radius: 20rpx;
  padding: 18rpx;
  display: flex;
  gap: 16rpx;
  align-items: center;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
}

.bank-cover {
  width: 90rpx;
  height: 90rpx;
  border-radius: 20rpx;
  background: #eef0ff;
  overflow: hidden;
}

.bank-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.bank-name {
  font-size: 26rpx;
  font-weight: 600;
}

.bank-meta {
  font-size: 20rpx;
  color: var(--muted);
}

.bank-btn {
  background: var(--primary-weak);
  color: var(--primary);
  height: 56rpx;
  line-height: 56rpx;
  border-radius: 999rpx;
  padding: 0 22rpx;
  font-size: 22rpx;
}
</style>
