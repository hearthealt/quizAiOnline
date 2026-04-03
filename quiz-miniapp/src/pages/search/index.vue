<template>
  <view class="page-shell search-page">
    <view class="search-bar glass-card">
      <input v-model="keyword" placeholder="输入题目关键词" class="search-input" />
      <view class="search-btn" @tap="doSearch">搜索</view>
    </view>

    <view v-if="hotKeywords.length" class="hot-panel glass-card">
      <text class="panel-title">热门搜索</text>
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

    <view v-if="results.length" class="result-panel">
      <view
        v-for="item in results"
        :key="item.id"
        class="result-item glass-card"
        @tap="goQuestion(item.id, item.bankId)"
      >
        <view class="result-top">
          <text class="result-type">题目 {{ item.type }}</text>
          <text class="result-difficulty" v-if="item.difficulty">难度 {{ item.difficulty }}</text>
        </view>
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

const goQuestion = (_id: number, bankId?: number) => {
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
.search-page {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.search-bar {
  padding: 14rpx;
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.search-input {
  flex: 1;
  height: 76rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: rgba(255,255,255,0.7);
  font-size: 28rpx;
}

.search-btn {
  min-width: 128rpx;
  height: 76rpx;
  line-height: 76rpx;
  text-align: center;
  border-radius: 999rpx;
  background: var(--primary);
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
}

.hot-panel {
  padding: 20rpx;
}

.panel-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.hot-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 16rpx;
}

.hot-item {
  padding: 10rpx 18rpx;
  border-radius: 999rpx;
  background: var(--primary-weak);
  color: var(--primary-dark);
  font-size: 24rpx;
}

.result-panel {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.result-item {
  padding: 20rpx;
}

.result-top {
  display: flex;
  justify-content: space-between;
  gap: 12rpx;
}

.result-type,
.result-difficulty {
  font-size: 20rpx;
  color: var(--muted);
}

.result-content {
  display: block;
  margin-top: 12rpx;
  font-size: 28rpx;
  line-height: 1.7;
  color: var(--text);
}
</style>
