<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <text class="hero-title">考试结果</text>
        <view class="hero-pill" :class="{ pass: isPass, fail: !isPass }">
          {{ isPass ? "通过" : "未通过" }}
        </view>
      </view>
      <view class="hero-score">
        <text class="score-value">{{ result?.score || 0 }}</text>
        <text class="score-label">成绩(分)</text>
      </view>
      <view class="hero-stats">
        <view class="stat-item">
          <text class="stat-value">{{ result?.correctCount || 0 }}/{{ result?.totalCount || 0 }}</text>
          <text class="stat-label">正确题</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ result?.correctRate || 0 }}%</text>
          <text class="stat-label">正确率</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ result?.duration || 0 }} 秒</text>
          <text class="stat-label">用时</text>
        </view>
        <view class="stat-item">
          <text class="stat-value">{{ result?.passScore || 0 }} 分</text>
          <text class="stat-label">通过线</text>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">答题详情</text>
      <view v-if="details.length" class="detail-list">
        <view v-for="(item, idx) in details" :key="item.questionId" class="detail-card">
          <view class="detail-head">
            <text class="detail-index">第 {{ idx + 1 }} 题</text>
            <text class="detail-status" :class="{ ok: item.isCorrect, bad: !item.isCorrect }">
              {{ item.isCorrect ? "正确" : "错误" }}
            </text>
          </view>
          <text class="detail-content">{{ item.content }}</text>
          <view class="detail-answers">
            <text>你的答案：{{ formatAnswer(item.userAnswer) }}</text>
            <text>正确答案：{{ formatAnswer(item.correctAnswer) }}</text>
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
        </view>
      </view>
      <EmptyState v-else title="暂无明细" />
    </view>

    <button class="primary-btn" @tap="backHome">返回首页</button>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getExamResult, type ExamAnswerDetail, type ExamResult } from "@/api/exam";
import EmptyState from "@/components/EmptyState.vue";
import { useUserStore } from "@/stores/user";

const result = ref<ExamResult>();
const details = ref<ExamAnswerDetail[]>([]);

const userStore = useUserStore();
const isVip = computed(() => (userStore.userInfo?.isVip || 0) === 1);
const isPass = computed(() => !!result.value?.isPass);

const fetchResult = async (examId: number) => {
  const res = await getExamResult(examId);
  result.value = res;
  details.value = res.details || [];
};

const backHome = () => {
  uni.switchTab({ url: "/pages/index/index" });
};

const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

const formatAnswer = (value?: string) => (value && value.length > 0 ? value : "-");

onLoad((query: any) => {
  const examId = Number(query.examId || 0);
  if (examId) fetchResult(examId);
});

onShow(() => {
  if (userStore.isLogin) {
    void userStore.refreshUser().catch(() => null);
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  min-height: 100vh;
  background: #f1f5f9;
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.hero {
  background: linear-gradient(135deg, #0f172a, #1e3a8a);
  border-radius: 28rpx;
  padding: 28rpx;
  color: #ffffff;
  box-shadow: 0 24rpx 50rpx rgba(15, 23, 42, 0.28);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-title {
  font-size: 32rpx;
  font-weight: 700;
}

.hero-pill {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.15);
}

.hero-pill.pass {
  background: rgba(34, 197, 94, 0.2);
  color: #bbf7d0;
}

.hero-pill.fail {
  background: rgba(239, 68, 68, 0.2);
  color: #fecaca;
}

.hero-score {
  margin: 22rpx 0;
  display: flex;
  align-items: baseline;
  gap: 12rpx;
}

.score-value {
  font-size: 60rpx;
  font-weight: 800;
}

.score-label {
  font-size: 24rpx;
  opacity: 0.85;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16rpx;
}

.stat-item {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 18rpx;
  padding: 16rpx 12rpx;
  text-align: center;
}

.stat-value {
  font-size: 28rpx;
  font-weight: 700;
}

.stat-label {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  opacity: 0.85;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #0f172a;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.detail-card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 22rpx;
  box-shadow: 0 18rpx 40rpx rgba(15, 23, 42, 0.08);
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail-index {
  font-size: 24rpx;
  color: #64748b;
}

.detail-status {
  font-size: 22rpx;
  padding: 4rpx 14rpx;
  border-radius: 999rpx;
  font-weight: 600;
}

.detail-status.ok {
  color: #15803d;
  background: #dcfce7;
}

.detail-status.bad {
  color: #b91c1c;
  background: #fee2e2;
}

.detail-content {
  font-size: 28rpx;
  color: #0f172a;
  line-height: 1.6;
}

.detail-answers {
  font-size: 24rpx;
  color: #475569;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.analysis {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.analysis-head {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.analysis-tip {
  font-size: 22rpx;
  color: #b45309;
  background: #fef3c7;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
  align-self: flex-start;
}

.analysis-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #0f172a;
}

.analysis-box {
  position: relative;
  height: 180rpx;
  background: #f8fafc;
  border-radius: 18rpx;
  padding: 16rpx;
  overflow: hidden;
}

.analysis-text {
  font-size: 24rpx;
  color: #475569;
  line-height: 1.6;
}

.analysis-mask {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.72), rgba(15, 23, 42, 0.4));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  color: #ffffff;
  text-align: center;
  padding: 0 24rpx;
}

.analysis-mask-text {
  font-size: 24rpx;
  font-weight: 600;
}

.analysis-mask-btn {
  font-size: 24rpx;
  font-weight: 700;
  color: #1f2937;
  background: #fbbf24;
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
}

.primary-btn {
  background: #111827;
  color: #ffffff;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 18rpx;
  font-size: 28rpx;
  font-weight: 600;
}
</style>
