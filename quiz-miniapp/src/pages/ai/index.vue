<template>
  <view class="ai-page" :class="{ locked: !isLogin || !hasVipAccess }">
    <view v-if="!isLogin || !hasVipAccess" class="access-shell">
      <view class="access-card glass-card">
        <view class="access-badge">{{ isLogin ? "VIP" : "AI" }}</view>
        <text class="access-title">{{ isLogin ? "AI 辅导为 VIP 专享" : "先登录，再使用 AI 辅导" }}</text>
        <text class="access-desc">
          {{
            isLogin
              ? "开通会员后可使用 AI 对话、保存历史记录，并结合学习过程持续追问。"
              : "登录后可同步学习进度、保存 AI 对话记录，再继续开通 VIP 使用完整辅导能力。"
          }}
        </text>
        <view class="access-actions">
          <view v-if="!isLogin" class="access-btn ghost" @tap="openLoginForAi">立即登录</view>
          <view class="access-btn primary" @tap="isLogin ? goVip() : openLoginForAi()">
            {{ isLogin ? "开通 VIP" : "登录后开通" }}
          </view>
        </view>
      </view>
    </view>

    <scroll-view
      v-else
      class="chat-shell"
      scroll-y
      :scroll-into-view="scrollIntoViewId"
      :scroll-with-animation="true"
    >
      <view class="chat-list">
        <view
          v-for="item in messages"
          :key="item.id"
          :id="'msg-' + item.id"
          class="msg-row"
          :class="{ 'msg-user': item.role === 'user' }"
        >
          <view class="msg-side">
            <image
              v-if="item.role === 'user' && userAvatar"
              class="msg-avatar-image"
              :src="resolveAssetUrl(userAvatar)"
              mode="aspectFill"
            />
            <view v-else class="msg-avatar" :class="item.role === 'user' ? 'user' : 'bot'">
              {{ item.role === "user" ? userInitial : "AI" }}
            </view>
            <text class="msg-role">{{ item.role === "user" ? "你的提问" : "AI 回复" }}</text>
          </view>

          <view class="msg-bubble" :class="{ 'bubble-user': item.role === 'user' }">
            {{ item.content }}
          </view>
        </view>

        <view v-if="loading" class="msg-row">
          <view class="msg-side">
            <view class="msg-avatar bot">AI</view>
            <text class="msg-role">AI 正在思考</text>
          </view>
          <view class="msg-bubble typing">
            <text class="dot">●</text>
            <text class="dot">●</text>
            <text class="dot">●</text>
          </view>
        </view>
      </view>

      <view id="scroll-bottom" style="height: 20rpx;" />
    </scroll-view>

    <view v-if="isLogin && hasVipAccess" class="composer">
      <view class="composer-head">
        <text class="composer-tip">{{ isLogin ? "对话记录已同步" : "登录后可同步记录" }}</text>
        <text class="composer-clear" @tap="clearHistory" v-if="messages.length > 1">清空对话</text>
      </view>
      <view class="composer-main">
        <input
          v-model="input"
          class="composer-input"
          placeholder="输入你的问题，例如：这道题为什么选 A？"
          confirm-type="send"
          @confirm="send"
        />
        <view class="send-btn" :class="{ disabled: loading }" @tap="send">发送</view>
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
import { computed, nextTick, ref } from "vue";
import { aiChat, getAiHistory, clearAiHistory } from "@/api/ai";
import { getAppConfig } from "@/api/config";
import { useUserStore } from "@/stores/user";
import LoginSheet from "@/components/LoginSheet.vue";
import { useLoginSheet } from "@/composables/useLoginSheet";
import { resolveAssetUrl } from "@/utils/assets";

type UiMessage = { id: number; role: "user" | "assistant"; content: string };

const userStore = useUserStore();
const isLogin = computed(() => userStore.isLogin);
const input = ref("");
const loading = ref(false);
const idSeed = ref(1);
const scrollIntoViewId = ref("");
const defaultGreeting = "你好！我是AI学习助手。你可以问我题目解析、知识点梳理、学习计划，或者把不会的题直接发给我。";
const messages = ref<UiMessage[]>([]);
const { showLogin, requestLogin, handleLoginSuccess: onLoginSuccess, handleLoginClose } = useLoginSheet("请先登录后再使用 AI 辅导");
const userAvatar = computed(() => userStore.userInfo?.avatar || "");
const hasVipAccess = computed(() => {
  const user = userStore.userInfo;
  if (!user || user.isVip !== 1 || !user.vipExpireTime) {
    return false;
  }
  const expireAt = new Date(user.vipExpireTime.replace("T", " ").replace(/-/g, "/")).getTime();
  return Number.isFinite(expireAt) && expireAt > Date.now();
});
const userInitial = computed(() => {
  const nickname = userStore.userInfo?.nickname?.trim();
  return nickname ? nickname.slice(0, 1).toUpperCase() : "你";
});
const skipNextOnShow = ref(false);

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoViewId.value = "";
    setTimeout(() => {
      scrollIntoViewId.value = "scroll-bottom";
    }, 50);
  });
};

const resetMessages = () => {
  messages.value = [{ id: 1, role: "assistant", content: defaultGreeting }];
  idSeed.value = 2;
};

const showVipRequired = (message = "AI辅导仅限VIP使用") => {
  uni.showModal({
    title: "VIP专享",
    content: message,
    confirmText: "开通VIP",
    success: (res) => {
      if (res.confirm) {
        goVip();
      }
    }
  });
};

const openLoginForAi = () => {
  requestLogin(null, "请先登录后再使用 AI 辅导");
};

const goVip = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

const isVipRequiredError = (error: unknown) => {
  return typeof error === "object" && error !== null && "code" in error && (error as { code?: number }).code === 30001;
};

const loadHistory = async () => {
  if (!isLogin.value || !hasVipAccess.value) return false;
  try {
    const history = await getAiHistory();
    if (history?.length > 0) {
      messages.value = history.map((m, idx) => ({ id: m.id || idx + 1, role: m.role, content: m.content }));
      idSeed.value = Math.max(...messages.value.map(m => m.id)) + 1;
      scrollToBottom();
      return true;
    }
  } catch (error) {
    if (isVipRequiredError(error)) {
      showVipRequired();
    }
  }
  return false;
};

const clearHistory = () => {
  if (!hasVipAccess.value) {
    showVipRequired();
    return;
  }
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
        } catch {
          uni.showToast({ title: "清空失败", icon: "none" });
        }
      }
    }
  });
};

const send = async () => {
  if (loading.value || !input.value.trim()) return;
  if (!isLogin.value) {
    const deferredContent = input.value.trim();
    requestLogin(async () => {
      if (!deferredContent) return;
      input.value = deferredContent;
      await loadHistory();
      await send();
    }, "请先登录后再使用 AI 辅导");
    return;
  }
  if (!hasVipAccess.value) {
    showVipRequired();
    return;
  }

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
  } catch (error) {
    if (isVipRequiredError(error)) {
      showVipRequired();
    }
  } finally {
    loading.value = false;
  }
};

const hydratePage = async () => {
  if (userStore.isLogin) {
    await userStore.refreshUser().catch(() => null);
  }
  resetMessages();
  if (!isLogin.value || !hasVipAccess.value) {
    return;
  }
  const hasHistory = await loadHistory();
  if (!hasHistory) {
    try {
      const config = await getAppConfig();
      if (config?.aiChatGreeting) {
        messages.value = [{ id: 1, role: "assistant", content: config.aiChatGreeting }];
      }
    } catch {
      /* ignore */
    }
  }
};

const handleLoginSuccess = async () => {
  await onLoginSuccess();
  await hydratePage();
};

onLoad(async () => {
  skipNextOnShow.value = true;
  await hydratePage();
});

onShow(() => {
  if (skipNextOnShow.value) {
    skipNextOnShow.value = false;
    return;
  }
  void hydratePage();
});
</script>

<style lang="scss" scoped>
.ai-page {
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: calc(152rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(152rpx + env(safe-area-inset-bottom));
}

.ai-page.locked {
  padding-bottom: 0;
}

.access-shell {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24rpx;
}

.access-card {
  width: 100%;
  padding: 36rpx 30rpx;
  text-align: center;
}

.access-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96rpx;
  height: 52rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: var(--warning-weak);
  color: var(--warning);
  font-size: 22rpx;
  font-weight: 700;
}

.access-title {
  display: block;
  margin-top: 22rpx;
  font-size: 36rpx;
  font-weight: 700;
  color: var(--text);
}

.access-desc {
  display: block;
  margin-top: 16rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: var(--muted);
}

.access-actions {
  display: flex;
  justify-content: center;
  gap: 16rpx;
  margin-top: 28rpx;
}

.access-btn {
  min-width: 180rpx;
  height: 76rpx;
  line-height: 76rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
  font-weight: 700;
}

.access-btn.primary {
  background: var(--primary);
  color: #fff;
}

.access-btn.ghost {
  background: var(--bg);
  color: var(--text-secondary);
}

.chat-shell {
  flex: 1;
  min-height: 0;
  padding: 18rpx 24rpx 12rpx;
  background: transparent;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.msg-row {
  display: flex;
  gap: 14rpx;
  align-items: flex-start;
}

.msg-row.msg-user {
  flex-direction: row-reverse;
}

.msg-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6rpx;
  width: 68rpx;
  flex-shrink: 0;
}

.msg-row.msg-user .msg-side {
  align-items: center;
}

.msg-avatar {
  width: 60rpx;
  height: 60rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  font-weight: 700;
}

.msg-avatar-image {
  width: 60rpx;
  height: 60rpx;
  border-radius: 20rpx;
  overflow: hidden;
  flex-shrink: 0;
  background: var(--primary-weak);
}

.msg-avatar.bot {
  background: #f9e9c8;
  color: #9a5c10;
}

.msg-avatar.user {
  background: var(--primary-weak);
  color: var(--primary-dark);
}

.msg-role {
  font-size: 18rpx;
  color: var(--muted);
}

.msg-bubble {
  max-width: calc(100% - 90rpx);
  padding: 18rpx 20rpx;
  background: rgba(255,255,255,0.72);
  border-radius: 24rpx;
  border-top-left-radius: 10rpx;
  font-size: 26rpx;
  color: var(--text);
  line-height: 1.7;
  box-shadow: var(--shadow-sm);
}

.msg-bubble.bubble-user {
  background: linear-gradient(135deg, var(--primary), var(--primary-light));
  color: #fff;
  border-top-right-radius: 10rpx;
}

.msg-bubble.typing {
  display: flex;
  gap: 8rpx;
}

.dot {
  animation: blink 1.4s infinite both;
  font-size: 18rpx;
  color: var(--muted);
}

.dot:nth-child(2) { animation-delay: 0.2s; }
.dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.3; }
  40% { opacity: 1; }
}

.composer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 20;
  padding: 12rpx 24rpx calc(12rpx + constant(safe-area-inset-bottom));
  padding: 12rpx 24rpx calc(12rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid var(--border);
  background: rgba(255, 253, 249, 0.96);
  backdrop-filter: blur(18rpx);
  box-shadow: 0 -10rpx 24rpx rgba(78, 45, 27, 0.06);
}

.composer-head {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 10rpx;
}

.composer-tip {
  font-size: 21rpx;
  color: var(--muted);
}

.composer-clear {
  margin-left: auto;
  font-size: 22rpx;
  color: var(--muted);
}

.composer-main {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.composer-input {
  flex: 1;
  height: 80rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: var(--bg);
  font-size: 26rpx;
}

.send-btn {
  min-width: 120rpx;
  height: 80rpx;
  line-height: 80rpx;
  text-align: center;
  border-radius: 999rpx;
  background: var(--primary);
  color: #fff;
  font-size: 26rpx;
  font-weight: 700;
}

.send-btn.disabled {
  opacity: 0.5;
}
</style>
