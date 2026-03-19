<template>
  <view class="page">
    <view class="chat-hero">
      <view class="hero-left">
        <view class="hero-badge">AI 辅导</view>
        <text class="chat-title">你的专属学习教练</text>
        <text class="chat-sub">讲解题目、梳理知识点、制定计划</text>
      </view>
      <view class="hero-orb">AI</view>
    </view>

    <view class="chat-wrap">
      <scroll-view 
        class="chat-card" 
        scroll-y 
        :scroll-into-view="scrollIntoViewId"
        :scroll-with-animation="true"
      >
      <view
        class="message"
        :class="item.role === 'user' ? 'message-user' : ''"
        v-for="item in messages"
        :key="item.id"
        :id="'msg-' + item.id"
      >
        <view v-if="item.role === 'assistant'" class="bot-avatar">AI</view>
        <view v-else class="user-avatar">我</view>
        <view
          class="bubble"
          :class="item.role === 'user' ? 'bubble-user' : ''"
        >
          {{ item.content }}
        </view>
      </view>
      <view v-if="loading" class="message" id="msg-loading">
        <view class="bot-avatar">AI</view>
        <view class="bubble">正在思考...</view>
      </view>
      <view id="scroll-bottom" style="height: 2rpx;" />
      </scroll-view>
    </view>

    <view class="input-bar">
      <view class="clear-btn" @tap="clearHistory">清空</view>
      <input
        v-model="input"
        class="input"
        placeholder="输入你的问题..."
        confirm-type="send"
        @confirm="send"
      />
      <button class="send-btn" :disabled="loading" @tap="send">发送</button>
    </view>

    <LoginSheet
      :show="showLogin"
      @close="showLogin = false"
      @success="handleLoginSuccess"
    />
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from "@dcloudio/uni-app";
import { computed, nextTick, ref } from "vue";
import { aiChat, getAiHistory, clearAiHistory, type ChatMessage } from "@/api/ai";
import { getAppConfig } from "@/api/config";
import { useUserStore } from "@/stores/user";
import LoginSheet from "@/components/LoginSheet.vue";

type UiMessage = {
  id: number;
  role: "user" | "assistant";
  content: string;
};

const userStore = useUserStore();
const isLogin = computed(() => userStore.isLogin);
const showLogin = ref(false);
const input = ref("");
const loading = ref(false);
const pendingSend = ref(false);
const idSeed = ref(1);
const scrollIntoViewId = ref("");
const defaultGreeting =
  "你好！我是你的 AI 智能导师。遇到不懂的题目、需要解释的知识点，或者想要制定学习计划，都可以随时问我哦！";
const messages = ref<UiMessage[]>([]);

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    // 先清空，再设置，触发滚动
    scrollIntoViewId.value = "";
    setTimeout(() => {
      scrollIntoViewId.value = "scroll-bottom";
    }, 50);
  });
};

// 从后端加载对话历史
const loadHistory = async () => {
  if (!isLogin.value) return false;
  try {
    const history = await getAiHistory();
    if (history && history.length > 0) {
      messages.value = history.map((m, idx) => ({
        id: m.id || idx + 1,
        role: m.role,
        content: m.content
      }));
      idSeed.value = Math.max(...messages.value.map(m => m.id)) + 1;
      scrollToBottom();
      return true;
    }
  } catch {
    // ignore
  }
  return false;
};

// 清空对话历史
const clearHistory = () => {
  uni.showModal({
    title: "提示",
    content: "确定清空对话记录吗？",
    success: async (res) => {
      if (res.confirm) {
        try {
          if (isLogin.value) {
            await clearAiHistory();
          }
          messages.value = [
            { id: 1, role: "assistant", content: defaultGreeting }
          ];
          idSeed.value = 2;
          uni.showToast({ title: "已清空", icon: "none" });
        } catch {
          uni.showToast({ title: "清空失败", icon: "none" });
        }
      }
    }
  });
};

const applyGreeting = (value?: string) => {
  if (!value) return;
  const content = value.trim();
  if (!content) return;
  const onlyDefault =
    messages.value.length === 1 &&
    messages.value[0].role === "assistant" &&
    messages.value[0].content === defaultGreeting;
  if (onlyDefault) {
    messages.value = [
      {
        id: 1,
        role: "assistant",
        content
      }
    ];
  }
};

const handleLoginSuccess = () => {
  showLogin.value = false;
  // 登录成功后加载历史
  loadHistory();
  if (pendingSend.value) {
    pendingSend.value = false;
    send();
  }
};

const send = async () => {
  if (loading.value) return;
  if (!input.value.trim()) {
    uni.showToast({ title: "请输入问题", icon: "none" });
    return;
  }
  if (!isLogin.value) {
    pendingSend.value = true;
    showLogin.value = true;
    return;
  }

  const content = input.value.trim();
  input.value = "";
  const nextMessages = messages.value.concat({
    id: idSeed.value + 1,
    role: "user",
    content
  });
  messages.value = nextMessages;
  idSeed.value += 1;
  scrollToBottom();
  loading.value = true;
  try {
    const history = nextMessages
      .filter((m) => m.role === "user" || m.role === "assistant")
      .slice(-6)
      .map((m) => ({ role: m.role, content: m.content }));
    const res = await aiChat(content, history);
    // 后端已自动保存，前端只需更新 UI
    messages.value.push({
      id: (idSeed.value += 1),
      role: "assistant",
      content: res.reply
    });
    scrollToBottom();
  } finally {
    loading.value = false;
  }
};

onLoad(async () => {
  // 显示默认问候语
  messages.value = [
    { id: 1, role: "assistant", content: defaultGreeting }
  ];
  idSeed.value = 2;
  
  // 如果已登录，加载后端历史
  if (isLogin.value) {
    const hasHistory = await loadHistory();
    if (!hasHistory) {
      // 没有历史，尝试加载配置的问候语
      try {
        const config = await getAppConfig();
        applyGreeting(config?.aiChatGreeting);
      } catch {
        // ignore
      }
    }
  } else {
    // 未登录，加载配置的问候语
    try {
      const config = await getAppConfig();
      applyGreeting(config?.aiChatGreeting);
    } catch {
      // ignore
    }
  }
});
</script>

<style lang="scss" scoped>
.page {
  padding: var(--space-xl) var(--space-xl) 180rpx;
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  min-height: 100vh;
  background: var(--bg);
}

.chat-hero {
  position: relative;
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  padding: var(--space-xl);
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, #1f2937, #0f172a);
  color: #ffffff;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.chat-hero::after {
  content: "";
  position: absolute;
  top: -80rpx;
  right: -120rpx;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(252, 211, 77, 0.35), transparent 70%);
}

.hero-left {
  display: flex;
  flex-direction: column;
  gap: var(--space-xs);
  z-index: 1;
}

.hero-badge {
  align-self: flex-start;
  background: rgba(255, 255, 255, 0.15);
  border: 1rpx solid rgba(255, 255, 255, 0.2);
  padding: 6rpx 14rpx;
  border-radius: var(--radius-full);
  font-size: 20rpx;
}

.hero-orb {
  width: 88rpx;
  height: 88rpx;
  border-radius: var(--radius-xl);
  background: linear-gradient(135deg, var(--warning), #f97316);
  color: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
  font-size: 28rpx;
  font-weight: 700;
  animation: float 3.8s ease-in-out infinite;
}

.chat-title {
  font-size: 32rpx;
  font-weight: 700;
}

.chat-sub {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.75);
}

.chat-card {
  background: var(--card);
  border-radius: var(--radius-xl);
  padding: var(--space-xl);
  box-shadow: var(--shadow);
  border: 1rpx solid var(--border-light);
  height: calc(100vh - 420rpx);
}

.chat-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.message {
  display: flex;
  gap: var(--space);
  align-items: flex-start;
  margin-bottom: var(--space);
}

.message-user {
  justify-content: flex-end;
}

.bot-avatar {
  width: 52rpx;
  height: 52rpx;
  border-radius: var(--radius);
  background: #fde68a;
  color: #1f2937;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.user-avatar {
  width: 52rpx;
  height: 52rpx;
  border-radius: var(--radius);
  background: #1f2937;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22rpx;
  font-weight: 500;
  flex-shrink: 0;
}

.bubble {
  background: var(--bg);
  border-radius: var(--radius-lg);
  padding: var(--space);
  font-size: 26rpx;
  color: var(--text-secondary);
  box-shadow: var(--shadow-sm);
  line-height: 1.6;
  position: relative;
  max-width: 75%;
}

.bubble::before {
  content: "";
  position: absolute;
  left: -8rpx;
  top: var(--space);
  width: 16rpx;
  height: 16rpx;
  background: var(--bg);
  border-radius: 4rpx;
  transform: rotate(45deg);
}

.bubble-user {
  margin-left: auto;
  background: var(--primary-weak);
  color: var(--text);
}

.message-user .user-avatar {
  order: 2;
}

.message-user .bubble {
  order: 1;
}

.message-user .bubble::before {
  left: auto;
  right: -8rpx;
  background: var(--primary-weak);
}

.input-bar {
  background: rgba(255, 255, 255, 0.96);
  border-radius: var(--radius-xl);
  padding: var(--space-sm);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  box-shadow: var(--shadow-lg);
  border: 1rpx solid var(--border-light);
  position: fixed;
  left: var(--space-xl);
  right: var(--space-xl);
  bottom: var(--space-xl);
  backdrop-filter: blur(10rpx);
}

.clear-btn {
  color: var(--muted);
  font-size: 24rpx;
  padding: var(--space-xs) var(--space);
  flex-shrink: 0;
}

.input {
  flex: 1;
  border: 2rpx solid var(--border);
  border-radius: var(--radius);
  padding: var(--space) var(--space-lg);
  font-size: 26rpx;
  background: var(--bg);
}

.send-btn {
  background: linear-gradient(135deg, var(--warning), #f97316);
  color: #1f2937;
  height: 68rpx;
  line-height: 68rpx;
  border-radius: var(--radius);
  padding: 0 var(--space-xl);
  font-size: 26rpx;
  font-weight: 600;
  flex-shrink: 0;
}

.send-btn[disabled] {
  opacity: 0.5;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6rpx); }
}
</style>
