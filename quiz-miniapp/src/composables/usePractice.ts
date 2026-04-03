import { ref, computed } from 'vue';
import {
  startPractice,
  getPracticeQuestion,
  submitPracticeAnswer,
  finishPractice,
  getPracticeProgress
} from '@/api/practice';
import type { PracticeRecord, QuestionVO, PracticeResult, PracticeProgress, PracticeMode } from '@/api/types';

export interface UsePracticeOptions {
  bankId: number;
  mode: PracticeMode | string;
}

/**
 * 练习功能逻辑
 */
export function usePractice() {
  const record = ref<PracticeRecord | null>(null);
  const currentQuestion = ref<QuestionVO | null>(null);
  const currentIndex = ref(0);
  const loading = ref(false);
  const submitting = ref(false);
  const result = ref<PracticeResult | null>(null);

  const recordId = computed(() => record.value?.id);
  const totalCount = computed(() => record.value?.totalCount || 0);
  const answerCount = computed(() => record.value?.answerCount || 0);
  const progress = computed(() => {
    if (totalCount.value === 0) return 0;
    return Math.round((currentIndex.value / totalCount.value) * 100);
  });

  /**
   * 开始练习
   */
  const start = async (options: UsePracticeOptions) => {
    loading.value = true;
    try {
      record.value = await startPractice(
        options.bankId,
        options.mode
      );
      currentIndex.value = record.value.lastIndex || 0;
      await loadQuestion(currentIndex.value);
      return record.value;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 恢复练习
   */
  const resume = async (practiceRecord: PracticeRecord) => {
    record.value = practiceRecord;
    currentIndex.value = practiceRecord.lastIndex || 0;
    await loadQuestion(currentIndex.value);
  };

  /**
   * 加载题目
   */
  const loadQuestion = async (index: number) => {
    if (!recordId.value) return;
    loading.value = true;
    try {
      currentQuestion.value = await getPracticeQuestion(recordId.value, index);
      currentIndex.value = index;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 提交答案
   */
  const submitAnswer = async (answer: string, answerTime?: number) => {
    if (!recordId.value || !currentQuestion.value) return false;
    submitting.value = true;
    try {
      const isCorrect = await submitPracticeAnswer(
        recordId.value,
        currentQuestion.value.id,
        answer,
        answerTime
      );
      currentQuestion.value.userAnswer = answer;
      currentQuestion.value.isCorrect = isCorrect ? 1 : 0;
      return isCorrect;
    } finally {
      submitting.value = false;
    }
  };

  /**
   * 下一题
   */
  const next = async () => {
    if (currentIndex.value < totalCount.value - 1) {
      await loadQuestion(currentIndex.value + 1);
      return true;
    }
    return false;
  };

  /**
   * 上一题
   */
  const prev = async () => {
    if (currentIndex.value > 0) {
      await loadQuestion(currentIndex.value - 1);
      return true;
    }
    return false;
  };

  /**
   * 跳转到指定题目
   */
  const goTo = async (index: number) => {
    if (index >= 0 && index < totalCount.value) {
      await loadQuestion(index);
      return true;
    }
    return false;
  };

  /**
   * 完成练习
   */
  const finish = async () => {
    if (!recordId.value) return null;
    loading.value = true;
    try {
      result.value = await finishPractice(recordId.value);
      return result.value;
    } finally {
      loading.value = false;
    }
  };

  /**
   * 获取进度
   */
  const getProgress = async (): Promise<PracticeProgress | null> => {
    if (!recordId.value) return null;
    return await getPracticeProgress(recordId.value);
  };

  /**
   * 重置状态
   */
  const reset = () => {
    record.value = null;
    currentQuestion.value = null;
    currentIndex.value = 0;
    result.value = null;
  };

  return {
    record,
    currentQuestion,
    currentIndex,
    loading,
    submitting,
    result,
    recordId,
    totalCount,
    answerCount,
    progress,
    start,
    resume,
    loadQuestion,
    submitAnswer,
    next,
    prev,
    goTo,
    finish,
    getProgress,
    reset
  };
}
