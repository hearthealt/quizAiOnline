<template>
  <view class="page">
    <view class="tabs">
      <text :class="['tab', type === 'practice' ? 'active' : '']" @tap="switchType('practice')">
        练习记录
      </text>
      <text :class="['tab', type === 'exam' ? 'active' : '']" @tap="switchType('exam')">
        考试记录
      </text>
    </view>

    <view v-if="records.length" class="list">
      <view v-for="item in records" :key="item.id" class="list-item" @tap="goDetail(item)">
        <view class="item-top">
          <text class="title">{{ item.bankName }}</text>
          <text class="tag">{{ type === 'exam' ? '考试' : '练习' }}</text>
        </view>
        <view class="item-stats">
          <view class="stat">
            <text class="stat-value">{{ item.correctRate }}%</text>
            <text class="stat-label">正确率</text>
          </view>
          <view class="stat">
            <text class="stat-value">{{ item.correctCount || 0 }}</text>
            <text class="stat-label">答对</text>
          </view>
          <view class="stat">
            <text class="stat-value">{{ item.totalCount || 0 }}</text>
            <text class="stat-label">总题数</text>
          </view>
        </view>
        <text class="meta">时间 {{ formatTime(item.createTime) }}</text>
      </view>
    </view>
    <EmptyState v-else title="暂无记录" description="做题记录会保存在这里" />
    <LoadMore :status="loadStatus" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom } from "@dcloudio/uni-app";
import { ref } from "vue";
import { getRecordList, type RecordItem } from "@/api/record";
import EmptyState from "@/components/EmptyState.vue";
import LoadMore from "@/components/LoadMore.vue";

const type = ref<"practice" | "exam">("practice");
const records = ref<RecordItem[]>([]);
const pageNum = ref(1);
const total = ref(0);
const loadStatus = ref<"more" | "loading" | "nomore">("more");

const fetchRecords = async () => {
  if (loadStatus.value === "loading" || loadStatus.value === "nomore") return;
  loadStatus.value = "loading";
  try {
    const res = await getRecordList(type.value, pageNum.value, 10);
    records.value = records.value.concat(res.list || []);
    total.value = res.total;
    loadStatus.value = records.value.length >= total.value ? "nomore" : "more";
    pageNum.value += 1;
  } catch (error) {
    loadStatus.value = "more";
    throw error;
  }
};

const switchType = (next: "practice" | "exam") => {
  if (type.value === next) return;
  type.value = next;
  records.value = [];
  pageNum.value = 1;
  loadStatus.value = "more";
  void fetchRecords().catch(() => null);
};

const goDetail = (item: RecordItem) => {
  uni.navigateTo({
    url: `/pages/record/detail?id=${item.id}&type=${item.type || type.value}`
  });
};

const formatTime = (value?: string) => {
  if (!value) return "-";
  return value.replace("T", " ").slice(0, 16);
};

onLoad(() => {
  void fetchRecords().catch(() => null);
});
onReachBottom(() => {
  void fetchRecords().catch(() => null);
});
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl);
}

.tabs {
  display: flex;
  gap: var(--space-lg);
  margin-bottom: var(--space);
}

.tab {
  padding: var(--space-sm) var(--space-lg);
  background: var(--bg-page);
  border-radius: var(--radius-full);
  font-size: 26rpx;
  color: var(--muted);
  font-weight: 500;
}

.tab.active {
  background: var(--primary);
  color: #ffffff;
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
}

.item-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}

.title {
  font-size: 28rpx;
  font-weight: 600;
}

.tag {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: var(--radius-full);
  background: var(--primary-weak);
  color: var(--primary);
}

.item-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}

.stat {
  background: var(--bg);
  border-radius: var(--radius);
  padding: var(--space-sm);
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  text-align: center;
}

.stat-value {
  font-size: 26rpx;
  font-weight: 700;
}

.stat-label {
  font-size: 22rpx;
  color: var(--muted);
}

.meta {
  font-size: 24rpx;
  color: var(--muted);
}
</style>
