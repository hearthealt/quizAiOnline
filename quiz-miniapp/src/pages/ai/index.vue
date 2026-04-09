<template>
  <view class="assistant-page" :class="{ preview: isPreviewMode }">
    <scroll-view v-if="isPreviewMode" class="preview-shell" scroll-y>
      <view class="preview-hero">
        <view class="hero-orb orb-left" />
        <view class="hero-orb orb-right" />

        <view class="hero-top">
          <view class="hero-status">{{ previewStatus }}</view>
          <text class="hero-status-copy">{{ previewStatusCopy }}</text>
        </view>

        <view v-if="isLogin" class="hero-user">
          <image
            v-if="userAvatar"
            class="hero-avatar-image"
            :src="resolveAssetUrl(userAvatar)"
            mode="aspectFill"
          />
          <view v-else class="hero-avatar">{{ userInitial }}</view>
          <view class="hero-user-copy">
            <text class="hero-user-name">{{ userName }}</text>
            <text class="hero-user-desc">已进入学习助手预览页，可先看适合怎么用。</text>
          </view>
        </view>

        <text class="hero-title">{{ previewTitle }}</text>
        <text class="hero-desc">{{ previewDesc }}</text>

        <view class="hero-metrics">
          <view v-for="item in previewMetrics" :key="item.label" class="metric-item">
            <text class="metric-label">{{ item.label }}</text>
            <text class="metric-value">{{ item.value }}</text>
          </view>
        </view>

        <view class="hero-actions">
          <view class="hero-btn secondary" @tap="goHome">{{ secondaryActionLabel }}</view>
          <view class="hero-btn primary" @tap="handleAccessPrimaryTap">{{ primaryActionLabel }}</view>
        </view>
      </view>

      <view class="preview-section glass-card">
        <view class="section-head">
          <text class="section-eyebrow">能帮你处理什么</text>
          <text class="section-title">把问题说出来，重点会更快浮出来</text>
        </view>

        <view class="feature-list">
          <view v-for="item in previewFeatures" :key="item.title" class="feature-item">
            <text class="feature-tag">{{ item.tag }}</text>
            <text class="feature-title">{{ item.title }}</text>
            <text class="feature-desc">{{ item.desc }}</text>
          </view>
        </view>
      </view>

      <view class="preview-section glass-card">
        <view class="section-head">
          <text class="section-eyebrow">怎么开口更高效</text>
          <text class="section-title">照着下面这种方式问，通常更容易得到清晰答案</text>
        </view>

        <view class="prompt-list">
          <view
            v-for="item in promptExamples"
            :key="item"
            class="prompt-item"
            @tap="copyPrompt(item)"
          >
            <text class="prompt-mark">问</text>
            <text class="prompt-text">{{ item }}</text>
            <text class="prompt-copy">点按复制</text>
          </view>
        </view>
      </view>

      <view class="preview-section glass-card">
        <view class="section-head">
          <text class="section-eyebrow">建议的使用节奏</text>
          <text class="section-title">别急着一次问完，顺着这三个阶段推进更有效</text>
        </view>

        <view class="step-list">
          <view v-for="item in workflowSteps" :key="item.index" class="step-item">
            <text class="step-index">{{ item.index }}</text>
            <view class="step-copy">
              <text class="step-title">{{ item.title }}</text>
              <text class="step-desc">{{ item.desc }}</text>
            </view>
          </view>
        </view>
      </view>

      <view class="preview-section final-panel">
        <text class="final-title">{{ finalCalloutTitle }}</text>
        <text class="final-desc">{{ finalCalloutDesc }}</text>
        <view class="final-actions">
          <view class="final-btn ghost" @tap="goHome">去首页练习</view>
          <view class="final-btn solid" @tap="handleAccessPrimaryTap">{{ primaryActionLabel }}</view>
        </view>
      </view>
    </scroll-view>

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
              {{ item.role === "user" ? userInitial : "助" }}
            </view>
          </view>

          <view class="msg-bubble" :class="{ 'bubble-user': item.role === 'user' }">
            {{ item.content }}
          </view>
        </view>

        <view v-if="loading" class="msg-row">
          <view class="msg-side">
            <view class="msg-avatar bot">助</view>
          </view>
          <view class="msg-bubble typing">
            <text class="dot">●</text>
            <text class="dot">●</text>
            <text class="dot">●</text>
          </view>
        </view>
      </view>

      <view id="scroll-bottom" class="scroll-bottom-space" />
    </scroll-view>

    <view v-if="isLogin && hasFullAccess" class="composer">
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
import { aiChat, clearAiHistory, getAiHistory } from "@/api/ai";
import { getAppConfig } from "@/api/config";
import LoginSheet from "@/components/LoginSheet.vue";
import { useLoginSheet } from "@/composables/useLoginSheet";
import { useUserStore } from "@/stores/user";
import { resolveAssetUrl } from "@/utils/assets";

type UiMessage = { id: number; role: "user" | "assistant"; content: string };
type PreviewMetric = { label: string; value: string };
type PreviewFeature = { tag: string; title: string; desc: string };
type WorkflowStep = { index: string; title: string; desc: string };

const userStore = useUserStore();

const input = ref("");
const loading = ref(false);
const idSeed = ref(1);
const scrollIntoViewId = ref("");
const messages = ref<UiMessage[]>([]);
const skipNextOnShow = ref(false);

const defaultGreeting = "你好！我是学习助手。你可以问我题目解析、知识点梳理、学习计划，或者把不会的题直接发给我。";
const { showLogin, requestLogin, handleLoginSuccess: onLoginSuccess, handleLoginClose } = useLoginSheet("请先登录后再使用学习助手");

const isLogin = computed(() => userStore.isLogin);
const userAvatar = computed(() => userStore.userInfo?.avatar || "");
const userName = computed(() => userStore.userInfo?.nickname?.trim() || "同学");
const userInitial = computed(() => {
  const nickname = userStore.userInfo?.nickname?.trim();
  return nickname ? nickname.slice(0, 1).toUpperCase() : "你";
});
const hasFullAccess = computed(() => {
  const user = userStore.userInfo;
  if (!user || user.isVip !== 1 || !user.vipExpireTime) {
    return false;
  }
  const expireAt = new Date(user.vipExpireTime.replace("T", " ").replace(/-/g, "/")).getTime();
  return Number.isFinite(expireAt) && expireAt > Date.now();
});
const isPreviewMode = computed(() => !isLogin.value || !hasFullAccess.value);

const previewMetrics = computed<PreviewMetric[]>(() => {
  if (isLogin.value) {
    return [
      { label: "适合处理", value: "题目拆解" },
      { label: "继续推进", value: "错因归纳" },
      { label: "延伸使用", value: "复习规划" }
    ];
  }

  return [
    { label: "登录后可保留", value: "提问记录" },
    { label: "登录后可承接", value: "学习上下文" },
    { label: "登录后可回看", value: "历史内容" }
  ];
});
const previewStatus = computed(() => isLogin.value ? "预览中" : "未登录");
const previewStatusCopy = computed(() => (
  isLogin.value
    ? "当前账号可以先浏览学习助手的使用方式。"
    : "先登录，再继续使用学习助手。"
));
const previewTitle = computed(() => (
  isLogin.value
    ? "把难点拆开，按你的节奏一点点学明白"
    : "登录后，把每一次提问都接到你的学习过程里"
));
const previewDesc = computed(() => (
  isLogin.value
    ? "这里适合围绕题目、知识点和复盘节奏展开交流。先看适合怎么问、怎么追问，再决定何时开启完整使用。"
    : "登录后会同步你的提问轨迹和历史记录，后续复盘时能顺着上下文继续往下问，学习过程也更连贯。"
));
const primaryActionLabel = computed(() => isLogin.value ? "开启完整使用" : "立即登录");
const secondaryActionLabel = computed(() => isLogin.value ? "返回首页" : "先去练习");
const finalCalloutTitle = computed(() => (
  isLogin.value
    ? "如果你已经有题目、错题或知识点，现在就可以继续往下走。"
    : "先登录，后面的提问记录和学习上下文会更完整。"
));
const finalCalloutDesc = computed(() => (
  isLogin.value
    ? "建议先从最近一道没弄懂的题开始，用“为什么错”“换一种讲法”“接下来怎么练”这样的节奏追问。"
    : "你也可以先去做几道题，带着真实问题再回来。这样提问会更具体，得到的帮助也更直接。"
));

const previewFeatures: PreviewFeature[] = [
  {
    tag: "题目拆解",
    title: "先把这道题讲清楚",
    desc: "围绕题干、选项、关键词和易错点展开，适合追问“为什么错”和“为什么对”。"
  },
  {
    tag: "错因归纳",
    title: "把总出错的地方集中拎出来",
    desc: "适合在做完一轮练习后，快速归纳反复卡住的概念、题型和判断方式。"
  },
  {
    tag: "复习推进",
    title: "把接下来怎么学说得更具体",
    desc: "可以按天安排复习顺序、练习密度和巩固重点，让后续学习更有连续性。"
  }
];
const promptExamples = [
  "这道题为什么不能选 B？请按题干和选项分别讲。",
  "把这个知识点换成更容易记住的说法，再给我一个小例子。",
  "根据我最近总错的内容，帮我排一个三天复习顺序。"
];
const workflowSteps: WorkflowStep[] = [
  {
    index: "01",
    title: "先带着具体问题来",
    desc: "可以是一道题、一个概念，或者一句“我总在这里卡住”。问题越具体，梳理越快。"
  },
  {
    index: "02",
    title: "先问原因，再问方法",
    desc: "先把“为什么错”说清楚，再追问“以后怎么判断”，比一次问很多更容易吸收。"
  },
  {
    index: "03",
    title: "顺着追问到能自己复述",
    desc: "直到你能用自己的话讲出来，或者能重新做对同类题，这次交流才算真正落地。"
  }
];

const normalizeUiCopy = (value: string) => value
  .replace(/OpenAI/g, "学习助手")
  .replace(/AI/g, "学习助手")
  .replace(/Vip/g, "完整服务")
  .replace(/VIP/g, "完整服务")
  .replace(/会员/g, "完整服务");

const scrollToBottom = () => {
  nextTick(() => {
    scrollIntoViewId.value = "";
    setTimeout(() => {
      scrollIntoViewId.value = "scroll-bottom";
    }, 50);
  });
};

const resetMessages = () => {
  messages.value = [{ id: 1, role: "assistant", content: normalizeUiCopy(defaultGreeting) }];
  idSeed.value = 2;
};

const goAccessPage = () => {
  uni.navigateTo({ url: "/pages/vip/index" });
};

const goHome = () => {
  uni.switchTab({ url: "/pages/index/index" });
};

const showAccessGuide = (message = "当前账号可先查看学习助手预览，完整使用可在下一页开启。") => {
  uni.showModal({
    title: "学习助手",
    content: message,
    confirmText: "去开启",
    success: (res) => {
      if (res.confirm) {
        goAccessPage();
      }
    }
  });
};

const openLoginForAi = () => {
  requestLogin(null, "请先登录后再使用学习助手");
};

const handleAccessPrimaryTap = () => {
  if (isLogin.value) {
    goAccessPage();
    return;
  }
  openLoginForAi();
};

const copyPrompt = (value: string) => {
  uni.setClipboardData({
    data: value,
    showToast: false,
    success: () => {
      uni.showToast({ title: "示例已复制", icon: "none" });
    }
  });
};

const isVipRequiredError = (error: unknown) => {
  return typeof error === "object" && error !== null && "code" in error && (error as { code?: number }).code === 30001;
};

const loadHistory = async () => {
  if (!isLogin.value || !hasFullAccess.value) return false;
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
      showAccessGuide();
    }
  }
  return false;
};

const clearHistory = () => {
  if (!hasFullAccess.value) {
    showAccessGuide();
    return;
  }
  uni.showModal({
    title: "提示",
    content: "确定清空对话记录吗？",
    success: async (res) => {
      if (res.confirm) {
        try {
          if (isLogin.value) await clearAiHistory();
          resetMessages();
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
    }, "请先登录后再使用学习助手");
    return;
  }
  if (!hasFullAccess.value) {
    showAccessGuide();
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
      showAccessGuide();
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
  if (!isLogin.value || !hasFullAccess.value) {
    return;
  }
  const hasHistory = await loadHistory();
  if (!hasHistory) {
    try {
      const config = await getAppConfig();
      if (config?.aiChatGreeting) {
        messages.value = [{ id: 1, role: "assistant", content: normalizeUiCopy(config.aiChatGreeting) }];
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

onLoad(() => {
  skipNextOnShow.value = true;
  void hydratePage().catch(() => null);
});

onShow(() => {
  if (skipNextOnShow.value) {
    skipNextOnShow.value = false;
    return;
  }
  void hydratePage().catch(() => null);
});
</script>

<style lang="scss" scoped>
.assistant-page {
  position: relative;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding-bottom: calc(152rpx + constant(safe-area-inset-bottom));
  padding-bottom: calc(152rpx + env(safe-area-inset-bottom));
}

.assistant-page.preview {
  padding-bottom: 0;
}

.preview-shell {
  flex: 1;
  min-height: 0;
  padding: 24rpx 24rpx 40rpx;
  box-sizing: border-box;
}

.preview-hero {
  position: relative;
  overflow: hidden;
  padding: 30rpx;
  border-radius: 34rpx;
  background:
    linear-gradient(150deg, rgba(36, 23, 17, 0.98), rgba(94, 46, 29, 0.94)),
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.12), transparent 26%);
  color: #fff8f1;
  box-shadow: 0 24rpx 52rpx rgba(78, 45, 27, 0.16);
}

.hero-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  filter: blur(6rpx);
}

.orb-left {
  width: 180rpx;
  height: 180rpx;
  top: -38rpx;
  right: -48rpx;
}

.orb-right {
  width: 120rpx;
  height: 120rpx;
  left: -30rpx;
  bottom: 120rpx;
}

.hero-top {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18rpx;
}

.hero-status {
  min-width: 108rpx;
  height: 48rpx;
  line-height: 48rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, 0.14);
  font-size: 22rpx;
  font-weight: 700;
  text-align: center;
  box-sizing: border-box;
}

.hero-status-copy {
  flex: 1;
  min-width: 0;
  font-size: 22rpx;
  line-height: 1.6;
  text-align: right;
  color: rgba(255, 248, 241, 0.72);
}

.hero-user {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 26rpx;
  padding: 18rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.08);
}

.hero-avatar-image,
.hero-avatar {
  width: 82rpx;
  height: 82rpx;
  border-radius: 26rpx;
  flex-shrink: 0;
}

.hero-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.14);
  color: #fff8f1;
  font-size: 30rpx;
  font-weight: 700;
}

.hero-user-copy {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.hero-user-name {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
}

.hero-user-desc {
  display: block;
  font-size: 22rpx;
  line-height: 1.6;
  color: rgba(255, 248, 241, 0.72);
}

.hero-title {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 26rpx;
  font-size: 42rpx;
  line-height: 1.28;
  font-weight: 700;
}

.hero-desc {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 18rpx;
  font-size: 24rpx;
  line-height: 1.85;
  color: rgba(255, 248, 241, 0.76);
}

.hero-metrics {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 28rpx;
}

.metric-item {
  min-width: 0;
  padding: 18rpx 16rpx;
  border-radius: 22rpx;
  background: rgba(255, 255, 255, 0.08);
}

.metric-label {
  display: block;
  font-size: 20rpx;
  line-height: 1.5;
  color: rgba(255, 248, 241, 0.58);
}

.metric-value {
  display: block;
  margin-top: 10rpx;
  font-size: 28rpx;
  line-height: 1.35;
  font-weight: 700;
}

.hero-actions {
  position: relative;
  z-index: 1;
  display: flex;
  gap: 14rpx;
  margin-top: 28rpx;
}

.hero-btn {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 999rpx;
  text-align: center;
  font-size: 26rpx;
  font-weight: 700;
}

.hero-btn.primary {
  background: linear-gradient(135deg, #f3c77b, #f0ad59);
  color: #503010;
}

.hero-btn.secondary {
  background: rgba(255, 255, 255, 0.1);
  color: #fff8f1;
}

.preview-section {
  margin-top: 22rpx;
  padding: 28rpx 24rpx;
}

.section-head {
  display: flex;
  flex-direction: column;
  gap: 10rpx;
}

.section-eyebrow {
  font-size: 22rpx;
  font-weight: 700;
  color: var(--primary);
}

.section-title {
  font-size: 32rpx;
  line-height: 1.45;
  font-weight: 700;
  color: var(--text);
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
}

.feature-item {
  padding: 22rpx;
  border-radius: 24rpx;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(251, 247, 242, 0.98));
  border: 1rpx solid var(--border-light);
}

.feature-tag {
  display: inline-flex;
  align-items: center;
  height: 42rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: var(--primary-weak);
  color: var(--primary-dark);
  font-size: 20rpx;
  font-weight: 700;
}

.feature-title {
  display: block;
  margin-top: 14rpx;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text);
}

.feature-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: var(--text-secondary);
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-top: 24rpx;
}

.prompt-item {
  display: flex;
  align-items: flex-start;
  gap: 16rpx;
  padding: 22rpx;
  border-radius: 24rpx;
  background: rgba(248, 226, 216, 0.52);
  border: 1rpx solid rgba(197, 76, 47, 0.08);
}

.prompt-mark {
  width: 44rpx;
  height: 44rpx;
  line-height: 44rpx;
  border-radius: 14rpx;
  text-align: center;
  background: #ffffff;
  color: var(--primary);
  font-size: 22rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.prompt-text {
  flex: 1;
  min-width: 0;
  font-size: 25rpx;
  line-height: 1.8;
  color: var(--text);
}

.prompt-copy {
  font-size: 21rpx;
  line-height: 1.7;
  color: var(--muted);
  flex-shrink: 0;
}

.step-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  margin-top: 24rpx;
}

.step-item {
  display: flex;
  gap: 18rpx;
  align-items: flex-start;
}

.step-index {
  width: 58rpx;
  height: 58rpx;
  line-height: 58rpx;
  border-radius: 18rpx;
  text-align: center;
  background: var(--text);
  color: #fff8f1;
  font-size: 24rpx;
  font-weight: 700;
  flex-shrink: 0;
}

.step-copy {
  flex: 1;
  min-width: 0;
  padding: 10rpx 0 0;
}

.step-title {
  display: block;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text);
}

.step-desc {
  display: block;
  margin-top: 10rpx;
  font-size: 24rpx;
  line-height: 1.8;
  color: var(--text-secondary);
}

.final-panel {
  margin-top: 22rpx;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: 30rpx;
  background:
    linear-gradient(180deg, rgba(255, 253, 249, 0.94), rgba(248, 242, 234, 0.98));
  border: 1rpx solid var(--border);
  box-shadow: var(--shadow);
}

.final-title {
  display: block;
  font-size: 30rpx;
  line-height: 1.55;
  font-weight: 700;
  color: var(--text);
}

.final-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 24rpx;
  line-height: 1.85;
  color: var(--text-secondary);
}

.final-actions {
  display: flex;
  gap: 14rpx;
  margin-top: 24rpx;
}

.final-btn {
  flex: 1;
  height: 82rpx;
  line-height: 82rpx;
  border-radius: 999rpx;
  text-align: center;
  font-size: 26rpx;
  font-weight: 700;
}

.final-btn.solid {
  background: var(--primary);
  color: #ffffff;
}

.final-btn.ghost {
  background: var(--bg);
  color: var(--text-secondary);
}

.chat-shell {
  flex: 1;
  min-height: 0;
  padding: 18rpx 20rpx 12rpx 16rpx;
  background: transparent;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
  padding-right: 4rpx;
  box-sizing: border-box;
}

.msg-row {
  display: flex;
  gap: 10rpx;
  align-items: flex-start;
}

.msg-row.msg-user {
  flex-direction: row-reverse;
  padding-right: 4rpx;
}

.msg-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 60rpx;
  flex-shrink: 0;
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
  display: block;
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

.msg-bubble {
  max-width: 72%;
  padding: 18rpx 20rpx;
  background: rgba(255, 255, 255, 0.72);
  border-radius: 24rpx;
  border-top-left-radius: 10rpx;
  font-size: 26rpx;
  color: var(--text);
  line-height: 1.7;
  word-break: break-word;
  box-shadow: var(--shadow-sm);
}

.msg-row:not(.msg-user) .msg-bubble {
  margin-right: 18rpx;
}

.msg-row.msg-user .msg-bubble {
  margin-left: 18rpx;
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

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
  }

  40% {
    opacity: 1;
  }
}

.scroll-bottom-space {
  height: 20rpx;
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
