<template>
  <view class="page">
    <view class="header">
      <text class="title">{{ isExam ? '考试记录' : '练习记录' }}</text>
      <text class="subtitle">{{ record?.bankName || '' }}</text>
    </view>

    <!-- 记录概览 -->
    <view class="overview-card">
      <view class="overview-row">
        <view class="stat-item">
          <text class="stat-label">得分</text>
          <text class="stat-value">{{ record?.score || 0 }}</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-label">正确率</text>
          <text class="stat-value">{{ correctRate }}%</text>
        </view>
        <view class="stat-divider" />
        <view class="stat-item">
          <text class="stat-label">用时</text>
          <text class="stat-value">{{ durationText }}</text>
        </view>
      </view>
      <view class="overview-tags">
        <text class="tag">答对 {{ record?.correctCount || 0 }} / {{ record?.totalCount || 0 }}</text>
        <text class="tag">{{ modeText }}</text>
      </view>
    </view>

    <!-- 题目列表 -->
    <view class="section">
      <text class="section-title">答题详情（{{ details.length }}）</text>
      <view v-if="details.length" class="question-list">
        <view v-for="(item, idx) in details" :key="idx" class="question-item">
          <view class="question-header">
            <text class="question-badge">{{ idx + 1 }}</text>
            <view class="question-status">
              <text class="status-tag" :class="getCorrectClass(item)">{{ getCorrectText(item) }}</text>
            </view>
          </view>

          <!-- 题目内容 -->
          <text class="question-content">{{ item.content || item.question?.content || '-' }}</text>

          <!-- 选项列表 -->
          <view v-if="getOptions(item) && getOptions(item).length" class="options-section">
            <view v-for="opt in getOptions(item)" :key="opt.label || opt.option" class="option-row">
              <text class="option-label">{{ opt.label || opt.option }}.</text>
              <text class="option-content">{{ opt.content }}</text>
            </view>
          </view>

          <!-- 答案区域 -->
          <view class="answer-section">
            <view class="answer-row">
              <text class="answer-label">你的答案：</text>
              <text class="answer-value" :class="{ wrong: !isCorrect(item) }">
                {{ getUserAnswer(item) || '-' }}
              </text>
            </view>
            <view class="answer-row">
              <text class="answer-label">正确答案：</text>
              <text class="answer-value correct">{{ getCorrectAnswer(item) || '-' }}</text>
            </view>
          </view>

          <!-- 解析区域（VIP 蒙层） -->
          <view class="analysis-section">
            <view class="analysis-header">
              <text class="analysis-icon">💡</text>
              <text class="analysis-title">答案解析</text>
            </view>
            <view class="analysis-box">
              <text class="analysis-text">
                {{ isVip ? (item.analysis || item.question?.analysis || '暂无解析') : '内容已隐藏' }}
              </text>
              <view v-if="!isVip" class="analysis-mask" @tap="goVip">
                <text class="mask-icon">👑</text>
                <text class="mask-text">开通 VIP 查看完整解析</text>
                <text class="mask-btn">立即开通</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <EmptyState v-else title="暂无答题明细" />
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getRecordDetail } from "@/api/record";
import EmptyState from "@/components/EmptyState.vue";
import { useUserStore } from "@/stores/user";

const record = ref<any>(null);
const details = ref<any[]>([]);
const isExam = ref(false);
const userStore = useUserStore();

const isVip = computed(() => (userStore.userInfo?.isVip || 0) === 1);

const correctRate = computed(() => {
  if (!record.value || !record.value.totalCount) return 0;
  return Math.round((record.value.correctCount / record.value.totalCount) * 100);
});

const durationText = computed(() => {
  const seconds = record.value?.duration || 0;
  const minutes = Math.floor(seconds / 60);
  const secs = seconds % 60;
  return minutes > 0 ? `${minutes}分${secs}秒` : `${secs}秒`;
});

const modeText = computed(() => {
  const mode = record.value?.mode;
  const modeMap: any = { ORDER: '顺序', RANDOM: '随机', WRONG: '错题', EXAM: '考试' };
  return modeMap[mode] || mode || '';
});

const fetchDetail = async (id: number, type: string) => {
  const res = await getRecordDetail(id, type);
  record.value = res.record;
  details.value = res.details || [];
  isExam.value = type === 'exam';
};

const getOptions = (item: any) => {
  // 优先从 options 获取，其次从 question.options 获取
  if (item.options && item.options.length) {
    return item.options;
  }
  if (item.question && item.question.options && item.question.options.length) {
    return item.question.options;
  }
  return [];
};

const getUserAnswer = (item: any) => {
  return item.userAnswer || item.lastUserAnswer || '-';
};

const getCorrectAnswer = (item: any) => {
  return item.correctAnswer || item.question?.answer || '-';
};

const isCorrect = (item: any) => {
  const userAnswer = getUserAnswer(item);
  const correctAnswer = getCorrectAnswer(item);
  return userAnswer === correctAnswer && userAnswer !== '-';
};

const getCorrectClass = (item: any) => {
  return isCorrect(item) ? 'correct' : 'wrong';
};

const getCorrectText = (item: any) => {
  return isCorrect(item) ? '正确' : '错误';
};

const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

onLoad((query: any) => {
  const id = Number(query.id || 0);
  const type = String(query.type || "practice");
  fetchDetail(id, type);
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  background: var(--bg);
  min-height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.title {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text);
}

.subtitle {
  font-size: 24rpx;
  color: var(--muted);
}

.overview-card {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(229, 57, 53, 0.3);
  margin-bottom: 24rpx;
}

.overview-row {
  display: flex;
  align-items: center;
  margin-bottom: 20rpx;
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.stat-label {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.8);
}

.stat-value {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
}

.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: rgba(255, 255, 255, 0.3);
}

.overview-tags {
  display: flex;
  gap: 12rpx;
}

.tag {
  padding: 6rpx 16rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #fff;
}

.section {
  margin-top: 24rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--text);
  display: block;
  margin-bottom: 16rpx;
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.question-item {
  background: var(--card);
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: var(--shadow-sm);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.question-badge {
  width: 48rpx;
  height: 48rpx;
  background: var(--primary-weak);
  color: var(--primary);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
  font-weight: 600;
}

.status-tag {
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
}

.status-tag.correct {
  background: var(--success-weak);
  color: var(--success);
}

.status-tag.wrong {
  background: var(--danger-weak);
  color: var(--danger);
}

.question-content {
  font-size: 28rpx;
  color: var(--text);
  line-height: 1.6;
  display: block;
  margin-bottom: 16rpx;
}

.options-section {
  background: var(--bg);
  border-radius: 12rpx;
  padding: 16rpx;
  margin-bottom: 16rpx;
}

.option-row {
  display: flex;
  gap: 12rpx;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.option-label {
  font-weight: 600;
  min-width: 32rpx;
}

.answer-section {
  margin-bottom: 16rpx;
}

.answer-row {
  display: flex;
  align-items: flex-start;
  gap: 12rpx;
  margin-bottom: 8rpx;
  font-size: 26rpx;
}

.answer-label {
  color: var(--muted);
  flex-shrink: 0;
}

.answer-value {
  color: var(--text-secondary);
}

.answer-value.correct {
  color: var(--success);
  font-weight: 600;
}

.answer-value.wrong {
  color: var(--danger);
  font-weight: 600;
}

.analysis-section {
  margin-top: 16rpx;
}

.analysis-header {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-bottom: 12rpx;
}

.analysis-icon {
  font-size: 28rpx;
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
  border-radius: 12rpx;
  padding: 16rpx;
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
  background: linear-gradient(135deg, rgba(15, 23, 42, 0.85), rgba(15, 23, 42, 0.6));
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
}

.mask-icon {
  font-size: 40rpx;
}

.mask-text {
  font-size: 24rpx;
  color: #fff;
}

.mask-btn {
  font-size: 24rpx;
  font-weight: 600;
  color: #1f2937;
  background: var(--warning);
  padding: 8rpx 24rpx;
  border-radius: 20rpx;
}
</style>
