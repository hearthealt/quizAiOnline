<template>
  <view class="page">
    <view v-if="items.length" class="list">
      <view v-for="item in items" :key="item.id" class="list-item">
        <view class="item-top">
          <text class="badge">收藏</text>
          <text class="bank">{{ item.bankName }}</text>
        </view>
        <text class="content">{{ item.content }}</text>
        <view v-if="item.options && item.options.length" class="options">
          <view v-for="opt in item.options" :key="opt.label" class="option-item">
            <text class="option-label">{{ opt.label }}.</text>
            <text class="option-content">{{ opt.content }}</text>
          </view>
        </view>
        <view class="answer-row">
          <text>正确答案：{{ item.answer || "-" }}</text>
        </view>
        <view class="analysis">
          <view class="analysis-head">
            <text class="analysis-title">解析</text>
          </view>
          <view class="analysis-box">
            <text class="analysis-text">
              {{ isVip ? item.analysis || "暂无解析" : "内容已隐藏" }}
            </text>
            <view v-if="!isVip" class="analysis-mask" @tap="goVip">
              <text class="analysis-mask-text">VIP可查看完整解析</text>
              <text class="analysis-mask-btn">立即开通</text>
            </view>
          </view>
        </view>
        <view class="meta">
          <text>已收藏</text>
          <button class="ghost-btn" @tap="remove(item.questionId)">取消收藏</button>
        </view>
      </view>
    </view>
    <EmptyState v-else title="暂无收藏" description="收藏题目后会出现在这里" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom, onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getFavoriteList, toggleFavorite, type FavoriteItem } from "@/api/favorite";
import EmptyState from "@/components/EmptyState.vue";
import { useUserStore } from "@/stores/user";

const items = ref<FavoriteItem[]>([]);
const pageNum = ref(1);
const total = ref(0);
const loading = ref(false);
const userStore = useUserStore();
const isVip = computed(() => (userStore.userInfo?.isVip || 0) === 1);

const fetchList = async (reset = false) => {
  if (loading.value) return;
  if (!reset && total.value > 0 && items.value.length >= total.value) return;
  loading.value = true;
  if (reset) {
    items.value = [];
    pageNum.value = 1;
    total.value = 0;
  }
  try {
    const res = await getFavoriteList(pageNum.value, 10);
    items.value = items.value.concat(res.list || []);
    total.value = res.total;
    if (items.value.length < total.value) {
      pageNum.value += 1;
    }
  } finally {
    loading.value = false;
  }
};

const remove = async (questionId: number) => {
  await toggleFavorite(questionId);
  items.value = items.value.filter((item) => item.questionId !== questionId);
};

const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

onLoad(() => fetchList(true));
onReachBottom(() => fetchList());
onShow(() => {
  if (userStore.isLogin) {
    void userStore.refreshUser().catch(() => null);
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl);
  background: var(--bg);
  min-height: 100vh;
}

.list {
  display: flex;
  flex-direction: column;
  gap: var(--space);
}

.list-item {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.item-top {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: 22rpx;
  color: var(--text-secondary);
}

.badge {
  background: var(--warning-weak);
  color: #b45309;
  padding: 4rpx 12rpx;
  border-radius: var(--radius-full);
  font-weight: 600;
}

.bank {
  color: var(--text-secondary);
}

.content {
  font-size: 28rpx;
  color: var(--text);
  line-height: 1.6;
}

.options {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  padding: var(--space-sm);
  background: var(--bg);
  border-radius: var(--radius);
}

.option-item {
  display: flex;
  gap: var(--space-xs);
  font-size: 26rpx;
  color: var(--text-secondary);
}

.option-label {
  font-weight: 600;
}

.answer-row {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.analysis {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.analysis-head {
  display: flex;
  flex-direction: column;
  gap: 4rpx;
}

.analysis-title {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--text);
}

.analysis-box {
  position: relative;
  min-height: 140rpx;
  background: var(--bg);
  border-radius: var(--radius);
  padding: var(--space);
  overflow: hidden;
}

.analysis-text {
  font-size: 26rpx;
  color: var(--text-secondary);
  line-height: 1.6;
}

.analysis-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.75), rgba(15, 23, 42, 0.5));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  color: #ffffff;
  text-align: center;
  padding: 0 var(--space-xl);
}

.analysis-mask-text {
  font-size: 24rpx;
  font-weight: 500;
}

.analysis-mask-btn {
  font-size: 24rpx;
  font-weight: 600;
  color: #1f2937;
  background: var(--warning);
  padding: var(--space-xs) var(--space-xl);
  border-radius: var(--radius-full);
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: var(--space-sm);
  color: var(--muted);
  font-size: 24rpx;
}

.ghost-btn {
  background: var(--primary-weak);
  color: var(--primary);
  font-size: 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 var(--space-lg);
  border-radius: var(--radius-full);
  font-weight: 500;
}
</style>
