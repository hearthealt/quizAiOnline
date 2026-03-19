<template>
  <view class="page">
    <view class="hero">
      <view class="hero-top">
        <image
          v-if="bank?.cover"
          class="hero-cover"
          :src="resolveAssetUrl(bank.cover)"
          mode="aspectFill"
        />
        <view v-else class="hero-cover" />
        <view class="hero-info">
          <text class="hero-title">{{ bank?.name }}</text>
          <text class="hero-sub">
            {{ bank?.questionCount || 0 }} 题 · {{ formatCount(bank?.practiceCount) }} 人练习
          </text>
        </view>
      </view>
    </view>

    <view class="stats-strip">
      <view class="stat-item">
        <text class="stat-value">{{ bank?.questionCount || 0 }}</text>
        <text class="stat-label">题量</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ bank?.passScore || 0 }}</text>
        <text class="stat-label">及格分</text>
      </view>
      <view class="stat-item">
        <text class="stat-value">{{ bank?.examTime || 0 }}m</text>
        <text class="stat-label">考试时长</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">题库简介</text>
      <view class="card">
        <text class="card-text">{{ bank?.description || "暂无描述" }}</text>
      </view>
    </view>

    <view class="section">
      <text class="section-title">学习进度</text>
      <view class="card progress-card">
        <view class="progress-row">
          <text v-if="isLogin">
            已完成 {{ bank?.userProgress || 0 }} / {{ progressTotal }}
          </text>
          <text v-else>登录后查看学习进度</text>
          <text class="progress-rate" v-if="isLogin">{{ progressPercent }}%</text>
        </view>
        <view class="progress-track">
          <view class="progress-bar" :style="{ width: progressWidth }" />
        </view>
      </view>
    </view>

    <LoginSheet
      :show="showLogin"
      @close="showLogin = false"
      @success="handleLoginSuccess"
    />

    <view class="bottom-actions">
      <button class="ghost-btn" @tap="handleExam">考试</button>
      <button class="primary-btn" @tap="handlePractice">练习</button>
    </view>
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
import { resolveAssetUrl } from "@/utils/assets";
import { formatCount } from "@/utils/format";

const bank = ref<BankItem>();
const bankId = ref<number>(0);
const showLogin = ref(false);
const pendingAction = ref<null | (() => void | Promise<void>)>(null);
const userStore = useUserStore();

const isLogin = computed(() => userStore.isLogin);

const fetchDetail = async () => {
  bank.value = await getBankDetail(bankId.value);
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

const hasOngoingPractice = computed(() => {
  if (!bank.value) return false;
  const total = bank.value.practiceTotalCount || 0;
  const progress = bank.value.userProgress || 0;
  return total > 0 && progress < total;
});

const requireLogin = (action: () => void | Promise<void>) => {
  if (userStore.isLogin) {
    action();
    return;
  }
  pendingAction.value = action;
  showLogin.value = true;
};

const handleLoginSuccess = () => {
  showLogin.value = false;
  const action = pendingAction.value;
  pendingAction.value = null;
  if (action) action();
};

const goPractice = async (mode: string, restart = false) => {
  const record = await startPracticeApi(bankId.value, mode, undefined, restart);
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
  fetchDetail();
});

onShow(() => {
  fetchDetail();
  if (userStore.isLogin && pendingAction.value) {
    const action = pendingAction.value;
    pendingAction.value = null;
    action();
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl) var(--space-xl) 160rpx;
}

.hero {
  background: linear-gradient(135deg, var(--primary-dark), var(--primary-light));
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  color: #ffffff;
}

.hero-top {
  display: flex;
  align-items: center;
  gap: var(--space);
}

.hero-cover {
  width: 88rpx;
  height: 88rpx;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.2);
  overflow: hidden;
  flex-shrink: 0;
}

.hero-info {
  flex: 1;
  min-width: 0;
}

.hero-title {
  font-size: 32rpx;
  font-weight: 700;
}

.hero-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.85);
  margin-top: 6rpx;
}

.section {
  margin-top: var(--space-lg);
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: var(--space-sm);
}

.card {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  box-shadow: var(--shadow);
}

.card-text {
  font-size: 26rpx;
  color: var(--text-secondary);
  line-height: 1.6;
}

.stats-strip {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space);
  display: flex;
  justify-content: space-between;
  gap: var(--space-sm);
  box-shadow: var(--shadow);
  margin-top: var(--space);
}

.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4rpx;
  align-items: center;
}

.stat-value {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text);
}

.stat-label {
  font-size: 22rpx;
  color: var(--muted);
}

.progress-card {
  display: flex;
  flex-direction: column;
  gap: var(--space);
}

.progress-row {
  display: flex;
  justify-content: space-between;
  font-size: 26rpx;
  color: var(--text-secondary);
}

.progress-rate {
  color: var(--primary);
  font-weight: 600;
}

.progress-track {
  height: 10rpx;
  background: var(--border-light);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar {
  height: 100%;
  background: linear-gradient(90deg, var(--primary), var(--primary-light));
  border-radius: var(--radius-full);
  transition: width 0.3s;
}

.bottom-actions {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: var(--space) var(--space-xl) 32rpx;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10rpx);
  border-top: 1rpx solid var(--border-light);
  display: flex;
  gap: var(--space);
}

.ghost-btn {
  flex: 1;
  background: var(--primary-weak);
  color: var(--primary);
  height: 80rpx;
  line-height: 80rpx;
  border-radius: var(--radius-full);
  font-size: 28rpx;
  font-weight: 500;
}

.primary-btn {
  flex: 1;
  background: var(--primary);
  color: #ffffff;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: var(--radius-full);
  font-size: 28rpx;
  font-weight: 500;
}
</style>
