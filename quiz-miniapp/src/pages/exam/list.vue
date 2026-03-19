<template>
  <view class="page">
    <view class="record-list">
      <view
        v-for="item in records"
        :key="item.id"
        class="record-item"
        @tap="goDetail(item.id)"
      >
        <view class="record-info">
          <text class="record-title">{{ item.bankName }}</text>
          <text class="record-meta">{{ item.correctRate }}% · {{ item.totalCount }}题</text>
        </view>
        <text class="record-score">{{ item.correctCount }}</text>
      </view>
    </view>
    <LoadMore :status="loadStatus" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom } from "@dcloudio/uni-app";
import { ref } from "vue";
import { getExamRecords, type RecordItem } from "@/api/exam";
import LoadMore from "@/components/LoadMore.vue";

const records = ref<RecordItem[]>([]);
const pageNum = ref(1);
const total = ref(0);
const loadStatus = ref<"more" | "loading" | "nomore">("more");

const fetchRecords = async () => {
  if (loadStatus.value === "loading" || loadStatus.value === "nomore") return;
  loadStatus.value = "loading";
  const res = await getExamRecords(pageNum.value, 10);
  records.value = records.value.concat(res.list || []);
  total.value = res.total;
  loadStatus.value = records.value.length >= total.value ? "nomore" : "more";
  pageNum.value += 1;
};

const goDetail = (id: number) => {
  uni.navigateTo({ url: `/pages/exam/result?examId=${id}` });
};

onLoad(fetchRecords);
onReachBottom(fetchRecords);
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.record-item {
  background: #ffffff;
  border-radius: 20rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.record-title {
  font-size: 28rpx;
  font-weight: 600;
}

.record-meta {
  font-size: 22rpx;
  color: #9aa0a6;
  margin-top: 6rpx;
}

.record-score {
  font-size: 28rpx;
  font-weight: 700;
  color: #1a73e8;
}
</style>
