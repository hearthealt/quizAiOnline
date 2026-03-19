<template>
  <view class="page">
    <view class="card">
      <text class="title">记录详情</text>
      <view class="meta">
        <text>记录ID {{ record?.id || "-" }}</text>
        <text>答对 {{ record?.correctCount || 0 }}/{{ record?.totalCount || 0 }}</text>
      </view>
      <view class="meta" v-if="record?.score !== undefined">
        <text>得分 {{ record?.score || 0 }}</text>
        <text>用时 {{ record?.duration || 0 }} 秒</text>
      </view>
    </view>
    <view class="card" v-if="details.length">
      <view v-for="(item, idx) in details" :key="idx" class="detail-item">
        <text class="detail-content">{{ getContent(item) }}</text>
        <text class="detail-answer">
          我的答案：{{ getUserAnswer(item) }} | 正确答案：{{ getCorrectAnswer(item) }}
        </text>
      </view>
    </view>
    <EmptyState v-else title="暂无明细" />
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { ref } from "vue";
import { getRecordDetail } from "@/api/record";
import EmptyState from "@/components/EmptyState.vue";

const record = ref<any>(null);
const details = ref<any[]>([]);

const fetchDetail = async (id: number, type: string) => {
  const res = await getRecordDetail(id, type);
  record.value = res.record;
  details.value = res.details || [];
};

const getContent = (item: any) => item.content || item.question?.content || "";
const getUserAnswer = (item: any) => item.userAnswer || "-";
const getCorrectAnswer = (item: any) => item.correctAnswer || item.question?.answer || "-";

onLoad((query: any) => {
  const id = Number(query.id || 0);
  const type = String(query.type || "practice");
  fetchDetail(id, type);
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.card {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
}

.title {
  font-size: 28rpx;
  font-weight: 600;
}

.meta {
  margin-top: 8rpx;
  font-size: 22rpx;
  color: #9aa0a6;
  display: flex;
  justify-content: space-between;
}

.detail-item {
  padding: 12rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}

.detail-content {
  font-size: 26rpx;
  color: #1f2937;
}

.detail-answer {
  font-size: 22rpx;
  color: #6b7280;
  margin-top: 6rpx;
}
</style>
