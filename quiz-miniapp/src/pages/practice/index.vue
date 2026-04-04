<template>
  <view class="page">
    <view class="top-bar">
      <button class="nav-btn" @tap="prevQuestion" :disabled="currentIndex === 0">上一题</button>
      <view class="progress-pill">第 {{ currentIndex + 1 }}/{{ total }} 题</view>
      <button class="nav-btn primary" @tap="nextQuestion">下一题</button>
    </view>

    <SwipeQuestionPanel @swipe-left="nextQuestion" @swipe-right="prevQuestion">
      <view v-if="question" class="card question-card">
        <view class="question-meta">
          <text class="question-type">{{ typeText }}</text>
        </view>
        <button class="favorite-btn" :class="{ active: isFavorite }" @tap="toggleFav">
          {{ isFavorite ? '♥' : '♡' }}
        </button>
        <text class="question-content">{{ question.content }}</text>
      </view>

      <view v-if="question" class="card answer-card">
        <view class="options">
          <OptionItem
            v-for="opt in question.options || []"
            :key="opt.label"
            :label="opt.label"
            :content="opt.content"
            :selected="selectedAnswers.includes(opt.label)"
            :correct="showResult && isCorrectOption(opt.label)"
            :wrong="showResult && isWrongOption(opt.label)"
            :disabled="isAnswered"
            @select="onSelectOption(opt.label)"
          />
        </view>
        <view v-if="question.type === 4" class="fill">
          <input
            class="fill-input"
            placeholder="请输入答案"
            v-model="fillAnswer"
            @blur="emitFill"
            :disabled="isAnswered"
          />
        </view>
      </view>

    <view v-if="showResult && question" class="result-card">
      <view class="result-head">
        <text class="result-title">答题结果</text>
        <text class="result-tag" :class="{ correct: isAnswerCorrect(), wrong: !isAnswerCorrect() }">
          {{ isAnswerCorrect() ? "回答正确" : "回答错误" }}
        </text>
      </view>
      <view class="result-grid">
        <view class="result-row">
          <text class="result-label">你的答案</text>
          <text class="result-value" :class="{ wrong: !isAnswerCorrect() }">{{ userAnswerText }}</text>
        </view>
        <view class="result-row">
          <text class="result-label">正确答案</text>
          <text class="result-value correct">{{ correctAnswerText }}</text>
        </view>
      </view>
    </view>

      <view v-if="showResult && question" class="analysis">
        <view class="analysis-head">
          <text class="analysis-title">答案解析</text>
        </view>
        <view class="analysis-box">
          <text class="analysis-text">
            {{ isVip ? question.analysis || "暂无解析" : "内容已隐藏" }}
          </text>
          <view v-if="!isVip" class="analysis-mask" @tap="goVip">
            <text class="analysis-mask-text">VIP可查看完整解析</text>
            <text class="analysis-mask-btn">立即开通</text>
          </view>
        </view>
      </view>
    </SwipeQuestionPanel>

  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import OptionItem from "@/components/OptionItem.vue";
import SwipeQuestionPanel from "@/components/SwipeQuestionPanel.vue";
import { getPracticeQuestion, submitPracticeAnswer, finishPractice, startPractice as startPracticeApi } from "@/api/practice";
import { toggleFavorite } from "@/api/favorite";
import { useUserStore } from "@/stores/user";
import { useAppStore } from "@/stores/app";
import type { QuestionVO } from "@/api/home";

const recordId = ref<number>(0);
const bankId = ref<number>(0);
const mode = ref("ORDER");
const currentIndex = ref(0);
const total = ref(1);
const question = ref<QuestionVO>();
const selectedAnswers = ref<string[]>([]);
const showResult = ref(false);
const answeredSet = ref<Set<number>>(new Set());
const answerMap = ref<Record<number, string[]>>({});
const fillAnswer = ref("");

const userStore = useUserStore();
const isVip = computed(() => (userStore.userInfo?.isVip || 0) === 1);
const appStore = useAppStore();
const autoNext = computed(() => appStore.settings.autoNext);
const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

const normalizeAnswer = (value: string) => value.trim();
const parseAnswer = (value?: string) =>
  (value || "")
    .split(",")
    .map((item) => normalizeAnswer(item))
    .filter((item) => item.length > 0);

const isAnswerCorrect = () => {
  if (!question.value?.answer) return false;
  const correct = parseAnswer(question.value.answer).sort().join("|");
  const chosen = parseAnswer(selectedAnswers.value.join(",")).sort().join("|");
  return correct === chosen;
};

const isCorrectOption = (label: string) => {
  if (!question.value?.answer) return false;
  return parseAnswer(question.value.answer).includes(label);
};

const isWrongOption = (label: string) => {
  if (!showResult.value) return false;
  return selectedAnswers.value.includes(label) && !isCorrectOption(label);
};

const isAnswered = computed(() => answeredSet.value.has(currentIndex.value));
const isFavorite = computed(() => !!question.value?.isFavorite);

const formatAnswerText = (values: string[]) => {
  if (!question.value || values.length === 0) return "未作答";
  if (question.value.type === 4) {
    return values.join(" / ");
  }
  return values.join(" / ");
};

const userAnswerText = computed(() => formatAnswerText(selectedAnswers.value));
const correctAnswerText = computed(() => formatAnswerText(parseAnswer(question.value?.answer)));

const fetchQuestion = async () => {
  const res = await getPracticeQuestion(recordId.value, currentIndex.value);
  question.value = res;
  if (res.userAnswer) {
    const stored = parseAnswer(res.userAnswer);
    selectedAnswers.value = stored;
    showResult.value = true;
    fillAnswer.value = res.userAnswer;
    answerMap.value = { ...answerMap.value, [currentIndex.value]: stored };
    const next = new Set(answeredSet.value);
    next.add(currentIndex.value);
    answeredSet.value = next;
    return;
  }
  const stored = answerMap.value[currentIndex.value];
  if (stored && stored.length > 0) {
    selectedAnswers.value = [...stored];
    showResult.value = true;
    fillAnswer.value = stored.join(",");
    return;
  }
  showResult.value = false;
  selectedAnswers.value = [];
  fillAnswer.value = "";
};

const onSelectOption = async (label: string) => {
  if (!question.value) return;
  if (isAnswered.value) return;
  if (question.value.type === 2) {
    selectedAnswers.value = selectedAnswers.value.includes(label)
      ? selectedAnswers.value.filter((item) => item !== label)
      : [...selectedAnswers.value, label];
    return;
  }
  selectedAnswers.value = [label];
  await submit();
};

const emitFill = async () => {
  if (!question.value) return;
  if (isAnswered.value) return;
  selectedAnswers.value = [fillAnswer.value];
  await submit();
};

const submit = async (allowAutoNext = true) => {
  if (!question.value) return;
  if (isAnswered.value) return;
  const answer = selectedAnswers.value.join(",");
  await submitPracticeAnswer(recordId.value, question.value.id, answer);
  await fetchQuestion();
  if (allowAutoNext && autoNext.value && isAnswerCorrect()) {
    const targetIndex = currentIndex.value;
    setTimeout(() => {
      if (currentIndex.value === targetIndex) {
        void nextQuestion();
      }
    }, 350);
  }
};

const nextQuestion = async () => {
  if (question.value && question.value.type === 2 && !showResult.value) {
    await submit(false);
  }
  if (currentIndex.value + 1 >= total.value) {
    await finishPractice(recordId.value);
    const modal = await uni.showModal({
      title: "练习完成",
      content: "是否重新练习？",
      confirmText: "重新练习",
      cancelText: "返回首页"
    });
    if (!modal.confirm) {
      uni.switchTab({ url: "/pages/index/index" });
      return;
    }
    if (!bankId.value) {
      uni.switchTab({ url: "/pages/index/index" });
      return;
    }
    const record = await startPracticeApi(bankId.value, mode.value, true);
    const nextIndex =
      record.answerCount > 0 && record.lastIndex < record.totalCount - 1
        ? record.lastIndex + 1
        : record.lastIndex || 0;
    uni.redirectTo({
      url: `/pages/practice/index?recordId=${record.id}&bankId=${bankId.value}&mode=${mode.value}&total=${record.totalCount}&index=${nextIndex}`
    });
    return;
  }
  currentIndex.value += 1;
  await fetchQuestion();
};

const prevQuestion = async () => {
  if (currentIndex.value === 0) return;
  currentIndex.value -= 1;
  await fetchQuestion();
};

const toggleFav = async () => {
  if (!question.value) return;
  await toggleFavorite(question.value.id);
  question.value = {
    ...question.value,
    isFavorite: !question.value.isFavorite
  };
};

const typeText = computed(() => {
  const map: Record<number, string> = {
    1: "单选题",
    2: "多选题",
    3: "判断题",
    4: "填空题"
  };
  return question.value ? map[question.value.type] || "题目" : "";
});

onLoad((query: any) => {
  recordId.value = Number(query.recordId || 0);
  bankId.value = Number(query.bankId || 0);
  mode.value = String(query.mode || "ORDER");
  total.value = Number(query.total || 1);
  const idx = Number(query.index || 0);
  currentIndex.value = Number.isNaN(idx) ? 0 : Math.max(0, idx);
  fetchQuestion();
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
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  background: var(--bg);
  min-height: 100vh;
}

.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space);
}

.nav-btn {
  width: 160rpx;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: var(--radius);
  background: var(--card);
  border: 2rpx solid var(--border);
  color: var(--text-secondary);
  font-size: 26rpx;
  font-weight: 500;
}

.nav-btn.primary {
  background: var(--primary);
  border-color: var(--primary);
  color: #ffffff;
}

.nav-btn[disabled] {
  opacity: 0.5;
}

.progress-pill {
  flex: 1;
  text-align: center;
  background: var(--primary-weak);
  color: var(--primary-dark);
  border-radius: var(--radius-full);
  font-size: 26rpx;
  font-weight: 600;
  height: 52rpx;
  line-height: 52rpx;
  user-select: none;
}

.card {
  background: var(--card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  box-shadow: var(--shadow);
}

.question-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  padding-top: var(--space-xl);
}

.question-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.question-type {
  font-size: 22rpx;
  color: var(--primary-dark);
  background: var(--primary-weak);
  padding: 6rpx 14rpx;
  border-radius: var(--radius-full);
  font-weight: 500;
}

.favorite-btn {
  width: 52rpx;
  height: 52rpx;
  line-height: 52rpx;
  border-radius: 50%;
  background: var(--bg);
  color: var(--muted);
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  top: var(--space-lg);
  right: var(--space-lg);
  font-size: 28rpx;
}

.favorite-btn.active {
  background: var(--danger-weak);
  color: var(--danger);
}

.question-content {
  font-size: 30rpx;
  line-height: 1.7;
  color: var(--text);
}

.answer-card {
  display: flex;
  flex-direction: column;
  gap: var(--space);
}

.options {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.fill {
  margin-top: var(--space-xs);
}

.fill-input {
  border: 2rpx solid var(--border);
  border-radius: var(--radius);
  padding: var(--space);
  font-size: 26rpx;
}

.result-card {
  background: var(--card);
  border-radius: var(--radius-xl);
  padding: 20rpx 22rpx;
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}

.result-title {
  font-size: 24rpx;
  font-weight: 700;
  color: var(--text);
}

.result-tag {
  padding: 6rpx 16rpx;
  border-radius: var(--radius-full);
  font-size: 20rpx;
  font-weight: 700;
}

.result-tag.correct {
  background: var(--success-weak);
  color: var(--success);
}

.result-tag.wrong {
  background: var(--danger-weak);
  color: var(--danger);
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12rpx;
}

.result-row {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
  padding: 14rpx 16rpx;
  border-radius: var(--radius);
  background: var(--bg);
}

.result-label {
  font-size: 20rpx;
  color: var(--muted);
}

.result-value {
  font-size: 24rpx;
  line-height: 1.5;
  color: var(--text-secondary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-value.correct {
  color: var(--success);
  font-weight: 600;
}

.result-value.wrong {
  color: var(--danger);
  font-weight: 600;
}

.analysis {
  background: var(--card);
  border-radius: var(--radius-xl);
  padding: var(--space-lg);
  box-shadow: var(--shadow);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.analysis-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.analysis-title {
  font-size: 26rpx;
  font-weight: 600;
  color: var(--text);
}

.analysis-box {
  position: relative;
  min-height: 120rpx;
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
</style>
