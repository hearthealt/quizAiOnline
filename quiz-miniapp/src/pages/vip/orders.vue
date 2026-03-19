<template>
  <view class="page">
    <view class="header">
      <text class="title">订单列表</text>
      <text class="sub">人工审核后会自动生效</text>
    </view>

    <view v-if="orders.length" class="list">
      <view v-for="item in orders" :key="item.id" class="order-card">
        <view class="order-top">
          <text class="order-name">{{ item.planName || "会员套餐" }}</text>
          <text class="order-status" :class="statusClass(item.status)">{{ statusText(item.status) }}</text>
        </view>
        <view class="order-meta">
          <text>金额 ¥{{ item.amount }}</text>
          <text>时长 {{ item.duration || 0 }} 天</text>
        </view>
        <text class="order-time">下单时间 {{ item.createTime }}</text>
      </view>
    </view>
    <EmptyState v-else title="暂无订单" description="提交订单后会显示在这里" />

    <LoadMore :status="loadStatus" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom } from "@dcloudio/uni-app";
import { ref } from "vue";
import { getVipOrders, type VipOrder } from "@/api/vip";
import EmptyState from "@/components/EmptyState.vue";
import LoadMore from "@/components/LoadMore.vue";

const orders = ref<VipOrder[]>([]);
const pageNum = ref(1);
const total = ref(0);
const loadStatus = ref<"more" | "loading" | "nomore">("more");

const statusText = (status?: number) => {
  if (status === 1) return "已开通";
  return "待确认";
};

const statusClass = (status?: number) => (status === 1 ? "success" : "pending");

const fetchOrders = async () => {
  if (loadStatus.value === "loading" || loadStatus.value === "nomore") return;
  loadStatus.value = "loading";
  const res = await getVipOrders(pageNum.value, 10);
  orders.value = orders.value.concat(res.list || []);
  total.value = res.total;
  loadStatus.value = orders.value.length >= total.value ? "nomore" : "more";
  pageNum.value += 1;
};

onLoad(fetchOrders);
onReachBottom(fetchOrders);
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.header {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.title {
  font-size: 30rpx;
  font-weight: 700;
}

.sub {
  font-size: 22rpx;
  color: var(--muted);
}

.list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.order-card {
  background: var(--card);
  border-radius: 20rpx;
  padding: 20rpx;
  box-shadow: 0 10rpx 30rpx rgba(15, 23, 42, 0.06);
}

.order-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-name {
  font-size: 26rpx;
  font-weight: 600;
}

.order-status {
  font-size: 22rpx;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}

.order-status.pending {
  background: #fff3e4;
  color: #c9690c;
}

.order-status.success {
  background: #e0f2fe;
  color: #0284c7;
}

.order-meta {
  display: flex;
  justify-content: space-between;
  margin-top: 10rpx;
  font-size: 22rpx;
  color: var(--muted);
}

.order-time {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9aa0a6;
}
</style>
