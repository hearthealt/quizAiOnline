<template>
  <view class="page">
    <!-- 头部信息卡片 -->
    <view class="header-card">
      <image
        v-if="bank?.cover"
        class="header-cover"
        :src="resolveAssetUrl(bank.cover)"
        mode="aspectFill"
      />
      <view v-else class="header-cover placeholder">📚</view>
      <view class="header-info">
        <text class="header-title">{{ bank?.name }}</text>
        <view class="header-tags">
          <text class="tag">{{ bank?.questionCount || 0 }}题</text>
          <text class="tag">{{ formatCount(bank?.practiceCount) }}人练习</text>
        </view>
      </view>
    </view>

    <!-- 数据统计 -->
    <view class="stats-row">
      <view class="stat-item">
        <text class="stat-icon">📖</text>
        <text class="stat-value">{{ bank?.questionCount || 0 }}</text>
        <text class="stat-label">总题数</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-icon">⏱️</text>
        <text class="stat-value">{{ estimatedExamMinutes }}</text>
        <text class="stat-label">预计分钟</text>
      </view>
      <view class="stat-divider" />
      <view class="stat-item">
        <text class="stat-icon">🎯</text>
        <text class="stat-value">{{ bank?.passScore || 0 }}</text>
        <text class="stat-label">及格分</text>
      </view>
    </view>

    <!-- 学习进度 -->
    <view v-if="isLogin" class="section">
      <view class="section-header">
        <text class="section-title">📊 学习进度</text>
        <text class="section-percent">{{ progressPercent }}%</text>
      </view>
      <view class="progress-card">
        <view class="progress-info">
          <text class="progress-text">已做 {{ bank?.userProgress || 0 }} / {{ progressTotal }}</text>
          <text class="progress-rate">正确率 {{ bank?.userCorrectRate || 0 }}%</text>
        </view>
        <view class="progress-track">
          <view class="progress-bar" :style="{ width: progressWidth }" />
        </view>
      </view>
    </view>

    <!-- 题库简介 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">📝 题库简介</text>
      </view>
      <view class="desc-card">
        <text class="desc-text">{{ bank?.description || "暂无描述" }}</text>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <view class="bar-actions">
        <button class="action-btn ghost" @tap="handleExam">
          <text class="btn-icon">📝</text>
          <text class="btn-text">模拟考试</text>
        </button>
        <button class="action-btn primary" @tap="handlePractice">
          <text class="btn-icon">✏️</text>
          <text class="btn-text">开始练习</text>
        </button>
      </view>
    </view>

    <LoginSheet
      :show="showLogin"
      @close="handleLoginClose"
      @success="handleLoginSuccess"
    />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getBankDetail, type BankItem } from "@/api/bank";
import { startPractice as startPracticeApi } from "@/api/practice";
import { getExamOngoing } from "@/api/exam";
import { useUserStore } from "@/stores/user";
import LoginSheet from "@/components/LoginSheet.vue";
import { useLoginSheet } from "@/composables/useLoginSheet";
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const bank = ref<BankItem>();
const bankId = ref<number>(0);
const detailLoading = ref(false);
const skipNextOnShow = ref(false);
const userStore = useUserStore();
const { showLogin, requestLogin, handleLoginSuccess, handleLoginClose } = useLoginSheet("请先登录");

const isLogin = computed(() => userStore.isLogin);

const fetchDetail = async () => {
  if (!bankId.value || detailLoading.value) return;
  detailLoading.value = true;
  try {
  bank.value = await getBankDetail(bankId.value);
  } finally {
    detailLoading.value = false;
  }
};

const progressTotal = computed(() => {
  if (!bank.value) return 0;
  return bank.value.practiceTotalCount && bank.value.practiceTotalCount > 0
    ? bank.value.practiceTotalCount
    : bank.value.questionCount || 0;
});

const progressWidth = computed(() => {
  if (!progressTotal.value) return "0%";
  const percent = Math.min(
    100,
    Math.round(((bank.value?.userProgress || 0) / progressTotal.value) * 100)
  );
  return `${percent}%`;
});

const progressPercent = computed(() => {
  if (!progressTotal.value) return 0;
  return Math.min(
    100,
    Math.round(((bank.value?.userProgress || 0) / progressTotal.value) * 100)
  );
});

const estimatedExamMinutes = computed(() => {
  const totalQuestions = bank.value?.questionCount || 0;
  return totalQuestions > 0 ? totalQuestions * 2 : 0;
});

const hasOngoingPractice = computed(() => {
  if (!bank.value) return false;
  const total = bank.value.practiceTotalCount || 0;
  const progress = bank.value.userProgress || 0;
  return total > 0 && progress < total;
});

const requireLogin = (action: () => void | Promise<void>) => {
  requestLogin(action, "请先登录");
};

const goPractice = async (mode: string, restart = false) => {
  const record = await startPracticeApi(bankId.value, mode, restart);
  const nextIndex =
    record.answerCount > 0 && record.lastIndex < record.totalCount - 1
      ? record.lastIndex + 1
      : record.lastIndex || 0;
  uni.navigateTo({
    url: `/pages/practice/index?recordId=${record.id}&bankId=${bankId.value}&mode=${mode}&total=${record.totalCount}&index=${nextIndex}`
  });
};

const handlePractice = () => {
  requireLogin(async () => {
    if (hasOngoingPractice.value) {
      const modal = await uni.showModal({
        title: "继续练习？",
        content: "检测到未完成的练习，是否继续？",
        confirmText: "继续练习",
        cancelText: "重新开始"
      });
      await goPractice("ORDER", !modal.confirm);
      return;
    }
    await goPractice("ORDER", false);
  });
};

const handleExam = () => {
  requireLogin(async () => {
    try {
      const ongoing = await getExamOngoing(bankId.value);
      if (ongoing.exists && !ongoing.expired && ongoing.examId) {
        const modal = await uni.showModal({
          title: "继续考试？",
          content: "检测到未完成的考试，是否继续？",
          confirmText: "继续考试",
          cancelText: "重新开始"
        });
        if (modal.confirm) {
          uni.navigateTo({
            url: `/pages/exam/index?bankId=${bankId.value}&examId=${ongoing.examId}`
          });
        } else {
          uni.navigateTo({
            url: `/pages/exam/index?bankId=${bankId.value}&restart=1`
          });
        }
        return;
      }
    } catch (err) {
      // ignore, fallback to start new exam
    }
    uni.navigateTo({
      url: `/pages/exam/index?bankId=${bankId.value}`
    });
  });
};

onLoad((query: any) => {
  bankId.value = Number(query.id || 0);
  skipNextOnShow.value = true;
  fetchDetail();
});

onShow(() => {
  if (skipNextOnShow.value) {
    skipNextOnShow.value = false;
    return;
  }
  fetchDetail();
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
  background: var(--bg);
  padding-bottom: 180rpx;
}

/* 头部卡片 */
.header-card {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 20rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-shadow: 0 8rpx 24rpx rgba(229, 57, 53, 0.3);
}

.header-cover {
  width: 100rpx;
  height: 100rpx;
  border-radius: 16rpx;
  background: rgba(255, 255, 255, 0.2);
  overflow: hidden;
  flex-shrink: 0;
}

.header-cover.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
}

.header-info {
  flex: 1;
  min-width: 0;
}

.header-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #fff;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-tags {
  display: flex;
  gap: 12rpx;
  margin-top: 12rpx;
}

.tag {
  padding: 6rpx 16rpx;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20rpx;
  font-size: 22rpx;
  color: #fff;
}

/* 统计行 */
.stats-row {
  background: var(--card);
  border-radius: 20rpx;
  padding: 28rpx 20rpx;
  display: flex;
  align-items: center;
  margin-top: 24rpx;
  box-shadow: var(--shadow-sm);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}

.stat-icon {
  font-size: 32rpx;
}

.stat-value {
  font-size: 32rpx;
  font-weight: 700;
  color: var(--text);
}

.stat-label {
  font-size: 22rpx;
  color: var(--muted);
}

.stat-divider {
  width: 1rpx;
  height: 48rpx;
  background: var(--border);
}

/* 区块样式 */
.section {
  margin-top: 24rpx;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
  padding-left: 8rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text);
}

.section-percent {
  font-size: 28rpx;
  font-weight: 600;
  color: var(--primary);
}

/* 进度卡片 */
.progress-card {
  background: var(--card);
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: var(--shadow-sm);
}

.progress-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16rpx;
}

.progress-text {
  font-size: 26rpx;
  color: var(--text-secondary);
}

.progress-rate {
  font-size: 26rpx;
  color: var(--primary);
  font-weight: 600;
}

.progress-track {
  height: 12rpx;
  background: var(--border-light);
  border-radius: 6rpx;
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  border-radius: 6rpx;
  transition: width 0.3s;
}

/* 简介卡片 */
.desc-card {
  background: var(--card);
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: var(--shadow-sm);
}

.desc-text {
  font-size: 26rpx;
  color: var(--text-secondary);
  line-height: 1.8;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 24rpx;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(10rpx);
  border-top: 1rpx solid var(--border-light);
}

.bar-actions {
  display: flex;
  gap: 16rpx;
}

.action-btn {
  flex: 1;
  height: 88rpx;
  border-radius: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.08);
}

.action-btn.ghost {
  background: var(--primary-weak);
  color: var(--primary);
}

.action-btn.primary {
  background: var(--primary);
  color: #fff;
}

.btn-icon {
  font-size: 32rpx;
}

.btn-text {
  white-space: nowrap;
}
</style>
