<template>
  <view class="card">
    <view class="card-header">
      <text class="card-title">第 {{ index + 1 }} 题</text>
      <text class="card-type">{{ typeText }}</text>
    </view>
    <view class="card-content">
      <text>{{ question.content }}</text>
    </view>
    <view class="card-options">
      <OptionItem
        v-for="(opt, idx) in question.options || []"
        :key="opt.label"
        :label="opt.label"
        :content="opt.content"
        :selected="selectedAnswers.includes(opt.label)"
        :correct="showResult && isCorrectOption(opt.label)"
        :wrong="showResult && isWrongOption(opt.label)"
        @select="onSelect(opt.label)"
      />
    </view>
    <view v-if="question.type === 4" class="fill">
      <input
        class="fill-input"
        placeholder="请输入答案"
        v-model="fillAnswer"
        @blur="emitFill"
      />
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";
import OptionItem from "@/components/OptionItem.vue";
import type { QuestionVO } from "@/api/home";

const props = defineProps<{
  question: QuestionVO;
  index: number;
  selected: string[];
  showResult?: boolean;
}>();

const emit = defineEmits<{
  (e: "select", answer: string[]): void;
  (e: "fill", answer: string): void;
}>();

const fillAnswer = ref("");

watch(
  () => props.question?.id,
  () => {
    fillAnswer.value = "";
  }
);

const selectedAnswers = computed(() => props.selected || []);

const typeText = computed(() => {
  const map: Record<number, string> = {
    1: "单选题",
    2: "多选题",
    3: "判断题",
    4: "填空题"
  };
  return map[props.question.type] || "题目";
});

const onSelect = (label: string) => {
  if (props.question.type === 2) {
    const next = selectedAnswers.value.includes(label)
      ? selectedAnswers.value.filter((item) => item !== label)
      : [...selectedAnswers.value, label];
    emit("select", next);
    return;
  }
  emit("select", [label]);
};

const emitFill = () => {
  emit("fill", fillAnswer.value);
};

const isCorrectOption = (label: string) => {
  if (!props.question.answer) return false;
  return props.question.answer.split(",").includes(label);
};

const isWrongOption = (label: string) => {
  if (!props.showResult) return false;
  return selectedAnswers.value.includes(label) && !isCorrectOption(label);
};
</script>

<style lang="scss" scoped>
.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 20rpx 50rpx rgba(15, 23, 42, 0.06);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.card-title {
  font-size: 30rpx;
  font-weight: 600;
}

.card-type {
  font-size: 22rpx;
  color: #6b7280;
  background: #f3f4f6;
  padding: 4rpx 12rpx;
  border-radius: 999rpx;
}

.card-content {
  font-size: 28rpx;
  line-height: 1.6;
  margin-bottom: 20rpx;
  color: #111827;
}

.fill {
  margin-top: 16rpx;
}

.fill-input {
  border: 2rpx solid #e5e7eb;
  border-radius: 16rpx;
  padding: 16rpx;
  font-size: 26rpx;
}
</style>
