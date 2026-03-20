<template>
  <view class="page">
    <!-- 顶部AI介绍 -->
    <view class="ai-header">
      <view class="ai-avatar">🤖</view>
      <view class="ai-info">
        <text class="ai-name">AI 学习助手</text>
        <text class="ai-desc">有问题随时问我</text>
      </view>
      <view class="clear-btn" @tap="clearHistory">清空</view>
    </view>

    <!-- 聊天区域 -->
    <scroll-view 
      class="chat-area" 
      scroll-y 
      :scroll-into-view="scrollIntoViewId"
      :scroll-with-animation="true"
    >
      <view 
        v-for="item in messages" 
        :key="item.id" 
        :id="'msg-' + item.id"
        class="msg-row"
        :class="{ 'msg-user': item.role === 'user' }"
      >
        <view v-if="item.role === 'assistant'" class="msg-avatar bot">🤖</view>
        <view class="msg-bubble" :class="{ 'bubble-user': item.role === 'user' }">
          {{ item.content }}
        </view>
        <view v-if="item.role === 'user'" class="msg-avatar user">👤</view>
      </view>
      
      <view v-if="loading" class="msg-row" id="msg-loading">
        <view class="msg-avatar bot">🤖</view>
        <view class="msg-bubble typing">
          <text class="dot">●</text>
          <text class="dot">●</text>
          <text class="dot">●</text>
        </view>
      </view>
      
      <view id="scroll-bottom" style="height: 20rpx;" />
    </scroll-view>

    <!-- 输入区域 -->
    <view class="input-area">
      <input 
        v-model="input" 
        class="input" 
        placeholder="输入你的问题..." 
        confirm-type="send"
        @confirm="send"
      />
      <view class="send-btn" :class="{ disabled: loading }" @tap="send">发送</view>
    </view>

    <LoginSheet :show="showLogin" @close="showLogin = false" @success="handleLoginSuccess" />
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { computed, nextTick, ref } from "vue";
import { aiChat, getAiHistory, clearAiHistory } from "@/api/ai";
import { getAppConfig } from "@/api/config";
import { useUserStore } from "@/stores/user";
import LoginSheet from "@/components/LoginSheet.vue";

type UiMessage = { id: number; role: "user" | "assistant"; content: string };

const userStore = useUserStore();
const isLogin = computed(() => userStore.isLogin);
const showLogin = ref(false);
const input = ref("");
const loading = ref(false);
const pendingSend = ref(false);
const idSeed = ref(1);
const scrollIntoViewId = ref("");
const defaultGreeting = "你好！我是AI学习助手，遇到不懂的题目、需要解释的知识点，都可以问我哦！";
const messages = ref<UiMessage[]>([]);

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoViewId.value = "";
    setTimeout(() => { scrollIntoViewId.value = "scroll-bottom"; }, 50);
  });
};

const loadHistory = async () => {
  if (!isLogin.value) return false;
  try {
    const history = await getAiHistory();
    if (history?.length > 0) {
      messages.value = history.map((m, idx) => ({ id: m.id || idx + 1, role: m.role, content: m.content }));
      idSeed.value = Math.max(...messages.value.map(m => m.id)) + 1;
      scrollToBottom();
      return true;
    }
  } catch { /* ignore */ }
  return false;
};

const clearHistory = () => {
  uni.showModal({
    title: "提示",
    content: "确定清空对话记录吗？",
    success: async (res) => {
      if (res.confirm) {
        try {
          if (isLogin.value) await clearAiHistory();
          messages.value = [{ id: 1, role: "assistant", content: defaultGreeting }];
          idSeed.value = 2;
          uni.showToast({ title: "已清空", icon: "none" });
        } catch { uni.showToast({ title: "清空失败", icon: "none" }); }
      }
    }
  });
};

const handleLoginSuccess = () => {
  showLogin.value = false;
  loadHistory();
  if (pendingSend.value) { pendingSend.value = false; send(); }
};

const send = async () => {
  if (loading.value || !input.value.trim()) return;
  if (!isLogin.value) { pendingSend.value = true; showLogin.value = true; return; }

  const content = input.value.trim();
  input.value = "";
  messages.value.push({ id: ++idSeed.value, role: "user", content });
  scrollToBottom();
  loading.value = true;
  
  try {
    const history = messages.value.slice(-6).map(m => ({ role: m.role, content: m.content }));
    const res = await aiChat(content, history);
    messages.value.push({ id: ++idSeed.value, role: "assistant", content: res.reply });
    scrollToBottom();
  } finally {
    loading.value = false;
  }
};

onLoad(async () => {
  messages.value = [{ id: 1, role: "assistant", content: defaultGreeting }];
  idSeed.value = 2;
  if (isLogin.value) {
    const hasHistory = await loadHistory();
    if (!hasHistory) {
      try {
        const config = await getAppConfig();
        if (config?.aiChatGreeting) messages.value = [{ id: 1, role: "assistant", content: config.aiChatGreeting }];
      } catch { /* ignore */ }
    }
  }
});
</script>

<style lang="scss" scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg);
}

.ai-header {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 24rpx;
  background: var(--card);
  border-bottom: 1rpx solid var(--border-light);
}

.ai-avatar {
  width: 80rpx;
  height: 80rpx;
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
}

.ai-info {
  flex: 1;
}

.ai-name {
  font-size: 30rpx;
  font-weight: 600;
  color: var(--text);
  display: block;
}

.ai-desc {
  font-size: 24rpx;
  color: var(--muted);
}

.clear-btn {
  padding: 12rpx 24rpx;
  background: var(--bg);
  border-radius: 24rpx;
  font-size: 24rpx;
  color: var(--muted);
}

.chat-area {
  flex: 1;
  padding: 24rpx;
  overflow-y: auto;
}

.msg-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
  align-items: flex-start;
}

.msg-row.msg-user {
  flex-direction: row-reverse;
}

.msg-avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  flex-shrink: 0;
}

.msg-avatar.bot {
  background: #fef3c7;
}

.msg-avatar.user {
  background: var(--primary-weak);
}

.msg-bubble {
  max-width: 70%;
  padding: 20rpx 24rpx;
  background: var(--card);
  border-radius: 24rpx;
  border-top-left-radius: 8rpx;
  font-size: 28rpx;
  color: var(--text);
  line-height: 1.6;
  box-shadow: var(--shadow-sm);
}

.msg-bubble.bubble-user {
  background: var(--primary);
  color: #fff;
  border-radius: 24rpx;
  border-top-right-radius: 8rpx;
}

.msg-bubble.typing {
  display: flex;
  gap: 8rpx;
  padding: 24rpx 32rpx;
}

.dot {
  animation: blink 1.4s infinite both;
  font-size: 20rpx;
  color: var(--muted);
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}

.input-area {
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: var(--card);
  border-top: 1rpx solid var(--border-light);
}

.input {
  flex: 1;
  height: 80rpx;
  padding: 0 24rpx;
  background: var(--bg);
  border-radius: 40rpx;
  font-size: 28rpx;
}

.send-btn {
  padding: 0 40rpx;
  height: 80rpx;
  line-height: 80rpx;
  background: var(--primary);
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
  font-weight: 500;
}

.send-btn.disabled {
  opacity: 0.5;
}
</style>
