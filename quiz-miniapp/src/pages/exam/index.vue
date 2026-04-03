<template>
  <view class="page">
    <view class="top-bar">
      <button class="nav-btn" @tap="prevQuestion" :disabled="currentIndex === 0">上一题</button>
      <view class="progress-pill">第 {{ currentIndex + 1 }}/{{ total }} 题</view>
      <button class="nav-btn primary" @tap="nextQuestion">下一题</button>
    </view>

    <SwipeQuestionPanel @swipe-left="nextQuestion" @swipe-right="prevQuestion">
      <view v-if="currentQuestion" class="card question-card">
        <view class="question-meta">
          <text class="question-type">{{ typeText }}</text>
          <ExamTimer :seconds="leftSeconds" />
        </view>
        <text class="question-content">{{ currentQuestion.content }}</text>
      </view>

      <view v-if="currentQuestion" class="card answer-card">
        <view class="options">
          <OptionItem
            v-for="opt in currentQuestion.options || []"
            :key="opt.label"
            :label="opt.label"
            :content="opt.content"
            :selected="selectedAnswers.includes(opt.label)"
            @select="onSelectOption(opt.label)"
          />
        </view>
        <view v-if="currentQuestion.type === 4" class="fill">
          <input
            class="fill-input"
            placeholder="请输入答案"
            v-model="fillAnswer"
            @blur="emitFill"
          />
        </view>
      </view>
    </SwipeQuestionPanel>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onUnload } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import OptionItem from "@/components/OptionItem.vue";
import ExamTimer from "@/components/ExamTimer.vue";
import SwipeQuestionPanel from "@/components/SwipeQuestionPanel.vue";
import { getExamSession, saveExamAnswer, startExam, submitExam } from "@/api/exam";

type LocalExamQuestion = {
  id: number;
  type: number;
  content: string;
  options: { label: string; content: string }[];
};

const examId = ref<number>(0);
const questions = ref<LocalExamQuestion[]>([]);
const currentIndex = ref(0);
const selectedAnswers = ref<string[]>([]);
const answerMap = ref<Record<number, string[]>>({});
const answeredSet = ref<Set<number>>(new Set());
const total = computed(() => questions.value.length || 1);
const leftSeconds = ref(0);
const timer = ref<number | null>(null);
const fillAnswer = ref("");

const currentQuestion = computed(() => questions.value[currentIndex.value]);

const syncSelected = () => {
  selectedAnswers.value = answerMap.value[currentIndex.value] || [];
  fillAnswer.value = selectedAnswers.value.join(",");
};

const persistAnswer = async () => {
  if (!examId.value || !currentQuestion.value) return;
  const answer = (answerMap.value[currentIndex.value] || []).join(",");
  try {
    await saveExamAnswer(examId.value, currentQuestion.value.id, answer);
  } catch (err) {
    // ignore save errors to avoid blocking the exam flow
  }
};

const updateAnswerState = () => {
  const values = answerMap.value[currentIndex.value] || [];
  const next = new Set(answeredSet.value);
  if (values.length > 0) {
    next.add(currentIndex.value);
  } else {
    next.delete(currentIndex.value);
    delete answerMap.value[currentIndex.value];
  }
  answeredSet.value = next;
};

const onSelectOption = (label: string) => {
  if (!currentQuestion.value) return;
  if (currentQuestion.value.type === 2) {
    selectedAnswers.value = selectedAnswers.value.includes(label)
      ? selectedAnswers.value.filter((item) => item !== label)
      : [...selectedAnswers.value, label];
  } else {
    selectedAnswers.value = [label];
  }
  answerMap.value[currentIndex.value] = selectedAnswers.value;
  updateAnswerState();
  void persistAnswer();
};

const emitFill = () => {
  if (!currentQuestion.value) return;
  answerMap.value[currentIndex.value] = [fillAnswer.value];
  selectedAnswers.value = [fillAnswer.value];
  updateAnswerState();
  void persistAnswer();
};

const nextQuestion = () => {
  if (currentIndex.value + 1 >= questions.value.length) {
    submit();
    return;
  }
  currentIndex.value += 1;
  syncSelected();
};

const prevQuestion = () => {
  if (currentIndex.value === 0) return;
  currentIndex.value -= 1;
  syncSelected();
};

const typeText = computed(() => {
  const map: Record<number, string> = {
    1: "单选题",
    2: "多选题",
    3: "判断题",
    4: "填空题"
  };
  return currentQuestion.value ? map[currentQuestion.value.type] || "题目" : "";
});

const submit = async () => {
  if (timer.value) {
    clearInterval(timer.value);
    timer.value = null;
  }
  const answers = questions.value.map((q, index) => ({
    questionId: q.id,
    answer: (answerMap.value[index] || []).join(",")
  }));
  const res = await submitExam(examId.value, answers);
  uni.redirectTo({ url: `/pages/exam/result?examId=${res.examId}` });
};

const tick = () => {
  if (leftSeconds.value <= 0) {
    submit();
    return;
  }
  leftSeconds.value -= 1;
};

const applySession = (res: {
  examId: number;
  questions: LocalExamQuestion[];
  examTime: number;
  leftSeconds?: number;
  answers?: { questionId: number; answer: string }[];
}) => {
  examId.value = res.examId;
  questions.value = res.questions;
  leftSeconds.value = res.leftSeconds != null ? res.leftSeconds : res.examTime * 60;
  answerMap.value = {};
  answeredSet.value = new Set();
  if (res.answers && res.answers.length > 0) {
    const indexMap = new Map<number, number>();
    questions.value.forEach((q, idx) => indexMap.set(q.id, idx));
    res.answers.forEach((item) => {
      const idx = indexMap.get(item.questionId);
      if (idx == null) return;
      const values = (item.answer || "").split(",").filter((v) => v.length > 0);
      if (values.length === 0) return;
      answerMap.value[idx] = values;
      answeredSet.value.add(idx);
    });
  }
  const firstUnanswered = questions.value.findIndex((_, idx) => !answeredSet.value.has(idx));
  if (firstUnanswered >= 0) {
    currentIndex.value = firstUnanswered;
  } else {
    currentIndex.value = Math.max(0, questions.value.length - 1);
  }
  syncSelected();
  if (timer.value) clearInterval(timer.value);
  timer.value = setInterval(tick, 1000) as unknown as number;
};

const start = async (bankId?: number, resumeExamId?: number, restart?: boolean) => {
  if (resumeExamId) {
    try {
      const res = await getExamSession(resumeExamId);
      applySession(res);
      return;
    } catch (err) {
      if (!bankId) return;
      const res = await startExam(bankId, true);
      applySession(res);
      return;
    }
  }
  if (!bankId) return;
  const res = await startExam(bankId, restart);
  applySession(res);
};

onLoad((query: any) => {
  const bankId = Number(query.bankId || 0);
  const examIdParam = Number(query.examId || 0);
  const restart = String(query.restart || "") === "1";
  start(bankId, examIdParam || undefined, restart);
});

onUnload(() => {
  if (timer.value) clearInterval(timer.value);
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
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.question-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}

.question-type {
  font-size: 22rpx;
  color: var(--primary-dark);
  background: var(--primary-weak);
  padding: 6rpx 14rpx;
  border-radius: var(--radius-full);
  font-weight: 500;
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
</style>
