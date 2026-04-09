<template>
  <view class="page">
    <view class="header">
      <text class="title">错题本</text>
      <button class="ghost-btn" @tap="startAll">错题练习</button>
    </view>
    <view class="list" v-if="items.length">
      <view v-for="item in items" :key="item.id" class="list-item">
        <view class="item-top">
          <text class="badge">错题</text>
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
          <text v-if="item.lastWrongAnswer">你的答案：{{ item.lastWrongAnswer }}</text>
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
              <text class="analysis-mask-text">查看完整解析</text>
              <text class="analysis-mask-btn">功能说明</text>
            </view>
          </view>
        </view>
        <view class="meta">
          <text>错题次数 {{ item.wrongCount || 1 }}</text>
          <view class="actions">
            <button class="ghost-btn" @tap="startOne(item.bankId)">练习</button>
            <button class="danger-btn" @tap="remove(item.id)">移除</button>
          </view>
        </view>
      </view>
    </view>
    <EmptyState v-else title="暂无错题" description="错题会自动收录" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom, onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getWrongList, removeWrong, type WrongItem } from "@/api/wrong";
import { startPractice } from "@/api/practice";
import EmptyState from "@/components/EmptyState.vue";
import { useUserStore } from "@/stores/user";

const items = ref<WrongItem[]>([]);
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
    const res = await getWrongList(undefined, pageNum.value, 10);
    items.value = items.value.concat(res.list || []);
    total.value = res.total;
    if (items.value.length < total.value) {
      pageNum.value += 1;
    }
  } finally {
    loading.value = false;
  }
};

const startAll = async () => {
  if (!items.value.length) {
    uni.showToast({ title: "暂无错题", icon: "none" });
    return;
  }
  const res = await startPractice(items.value[0].bankId, "WRONG");
  const nextIndex =
    res.answerCount > 0 && res.lastIndex < res.totalCount - 1
      ? res.lastIndex + 1
      : res.lastIndex || 0;
  uni.navigateTo({
    url: `/pages/practice/index?recordId=${res.id}&bankId=${items.value[0].bankId}&mode=WRONG&total=${res.totalCount}&index=${nextIndex}`
  });
};

const startOne = async (bankId: number) => {
  const res = await startPractice(bankId, "WRONG");
  const nextIndex =
    res.answerCount > 0 && res.lastIndex < res.totalCount - 1
      ? res.lastIndex + 1
      : res.lastIndex || 0;
  uni.navigateTo({
    url: `/pages/practice/index?recordId=${res.id}&bankId=${bankId}&mode=WRONG&total=${res.totalCount}&index=${nextIndex}`
  });
};

const remove = async (id: number) => {
  await removeWrong(id);
  items.value = items.value.filter((item) => item.id !== id);
};

const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

onLoad(() => {
  void fetchList(true).catch(() => null);
});
onReachBottom(() => {
  void fetchList().catch(() => null);
});
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

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space);
}

.title {
  font-size: 32rpx;
  font-weight: 700;
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
  background: var(--danger-weak);
  color: #b91c1c;
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

.actions {
  display: flex;
  gap: var(--space-sm);
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

.danger-btn {
  background: var(--danger-weak);
  color: var(--danger);
  font-size: 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  padding: 0 var(--space-lg);
  border-radius: var(--radius-full);
  font-weight: 500;
}
</style>
