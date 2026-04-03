<template>
  <view class="page">
    <view class="search-box">
      <input v-model="keyword" placeholder="输入题目关键词" class="search-input" />
      <button class="search-btn" @tap="doSearch">搜索</button>
    </view>

    <view v-if="hotKeywords.length" class="hot">
      <text class="hot-title">热门搜索</text>
      <view class="hot-list">
        <text
          v-for="word in hotKeywords"
          :key="word"
          class="hot-item"
          @tap="selectHot(word)"
        >
          {{ word }}
        </text>
      </view>
    </view>

    <view v-if="results.length" class="result-list">
      <view
        v-for="item in results"
        :key="item.id"
        class="result-item"
        @tap="goQuestion(item.id, item.bankId)"
      >
        <text class="result-content">{{ item.content }}</text>
      </view>
    </view>

    <EmptyState v-else title="暂无结果" description="试试其他关键词" />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onReachBottom } from "@dcloudio/uni-app";
import { ref } from "vue";
import { searchQuestions, getHotKeywords } from "@/api/search";
import type { QuestionListVO } from "@/api/home";
import EmptyState from "@/components/EmptyState.vue";

const keyword = ref("");
const results = ref<QuestionListVO[]>([]);
const hotKeywords = ref<string[]>([]);
const pageNum = ref(1);
const total = ref(0);

const doSearch = async () => {
  if (!keyword.value) return;
  pageNum.value = 1;
  results.value = [];
  await fetchMore();
};

const fetchMore = async () => {
  if (!keyword.value) return;
  const res = await searchQuestions(keyword.value, pageNum.value, 10);
  results.value = results.value.concat(res.list || []);
  total.value = res.total;
  if (results.value.length < total.value) {
    pageNum.value += 1;
  }
};

const selectHot = (word: string) => {
  keyword.value = word;
  doSearch();
};

const goQuestion = (id: number, bankId?: number) => {
  if (bankId) {
    uni.navigateTo({ url: `/pages/bank/detail?id=${bankId}` });
    return;
  }
  uni.showToast({ title: "暂无题库信息", icon: "none" });
};

const loadHot = async () => {
  hotKeywords.value = await getHotKeywords();
};

onLoad(loadHot);
onReachBottom(fetchMore);
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl);
}

.search-box {
  display: flex;
  gap: var(--space-sm);
}

.search-input {
  flex: 1;
  border: 2rpx solid var(--border);
  border-radius: var(--radius);
  padding: var(--space);
  font-size: 28rpx;
  background: var(--card);
}

.search-btn {
  background: var(--primary);
  color: #ffffff;
  border-radius: var(--radius);
  height: 72rpx;
  line-height: 72rpx;
  padding: 0 var(--space-xl);
  font-size: 28rpx;
  font-weight: 500;
}

.hot {
  margin-top: var(--space-lg);
}

.hot-title {
  font-size: 26rpx;
  color: var(--text-secondary);
}

.hot-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
  margin-top: var(--space-sm);
}

.hot-item {
  background: var(--bg-page);
  padding: var(--space-xs) var(--space);
  border-radius: var(--radius-full);
  font-size: 26rpx;
  color: var(--text-secondary);
}

.result-list {
  margin-top: var(--space-xl);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.result-item {
  background: var(--card);
  border-radius: var(--radius-lg);
  padding: var(--space);
  font-size: 28rpx;
  box-shadow: var(--shadow-sm);
}
</style>
