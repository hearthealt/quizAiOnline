<template>
  <view v-if="show" class="sheet-mask" :class="{ leaving: leaving }" @tap="close">
    <view class="sheet-panel" :class="{ leaving: leaving }" @tap.stop>
      <view class="sheet-header">
        <text class="sheet-title">登录/注册</text>
        <text class="sheet-close" @tap="close">x</text>
      </view>
      <view class="sheet-body">
        <view class="hero-mark">
          <view class="mark-orbit" />
          <text class="mark-text">Q</text>
        </view>
        <text class="welcome">欢迎来到题库小程序</text>
        <text class="desc">{{ reasonText || "登录后即可进行练习、考试，并同步你的学习记录与收藏内容。" }}</text>
        <view class="agreement-card" @tap="toggleAgreement">
          <view class="agreement-check" :class="{ active: agreementAccepted }">
            <text class="agreement-checkmark">{{ agreementAccepted ? "✓" : "" }}</text>
          </view>
          <view class="agreement-copy">
            <view class="agreement-line">
              <text class="agreement-label">已阅读并同意</text>
              <text class="agreement-link" @tap.stop="openDocument('service')">《用户服务协议》</text>
              <text class="agreement-label">与</text>
              <text class="agreement-link" @tap.stop="openDocument('privacy')">《隐私政策》</text>
            </view>
            <text class="agreement-tip">登录后可同步学习记录、收藏内容和个人资料。</text>
          </view>
        </view>
        <button class="wechat-btn" :disabled="loading" @tap="handleWxLogin">
          {{ loading ? "处理中..." : "微信登录" }}
        </button>
        <button class="phone-btn" @tap="close">暂不登录</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from "vue";
import { wxLogin } from "@/api/auth";
import { useUserStore } from "@/stores/user";
import {
  clearLegalConsent,
  ensureLegalConsent,
  openAgreementPage,
  saveLegalConsent
} from "@/utils/privacy";

const props = defineProps<{ show: boolean; reasonText?: string }>();
const emit = defineEmits<{
  (e: "close"): void;
  (e: "success"): void;
}>();

const userStore = useUserStore();
const leaving = ref(false);
const loading = ref(false);
const agreementAccepted = ref(false);

const close = (force = false, afterClose?: () => void) => {
  if (leaving.value || (loading.value && !force)) return;
  leaving.value = true;
  setTimeout(() => {
    leaving.value = false;
    emit("close");
    afterClose?.();
  }, force ? 120 : 180);
};

const redirectToCompleteProfile = () => {
  close(true, () => {
    uni.navigateTo({ url: "/pages/auth/complete-profile?from=login" });
  });
};

const setAgreementAccepted = (value: boolean) => {
  agreementAccepted.value = value;
  if (value) {
    saveLegalConsent();
    return;
  }
  clearLegalConsent();
};

const toggleAgreement = () => {
  if (loading.value) return;
  setAgreementAccepted(!agreementAccepted.value);
};

const openDocument = (type: "service" | "privacy") => {
  openAgreementPage(type);
};

const ensureReadyToCollect = async () => {
  if (!agreementAccepted.value) {
    ensureLegalConsent();
    return false;
  }
  saveLegalConsent();
  return true;
};

const runLogin = async () => {
  if (loading.value) return;
  const ready = await ensureReadyToCollect();
  if (!ready) return;

  loading.value = true;

  try {
    const loginRes = await uni.login({ provider: "weixin" });
    if (!loginRes.code) {
      uni.showToast({ title: "登录失败，请重试", icon: "none" });
      return;
    }

    const data = await wxLogin(loginRes.code || "");
    userStore.setToken(data.token);
    userStore.setUserInfo(data.userInfo);

    if (data.newUser) {
      redirectToCompleteProfile();
      return;
    }

    emit("success");
  } catch (_error) {
  } finally {
    loading.value = false;
  }
};

const handleWxLogin = async () => {
  await runLogin();
};

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      leaving.value = false;
      agreementAccepted.value = false;
    }
  },
  { immediate: false }
);
</script>

<style lang="scss" scoped>
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.38);
  display: flex;
  align-items: flex-end;
  z-index: 999;
  transition: opacity 0.18s ease;
}

.sheet-panel {
  width: 100%;
  background:
    radial-gradient(circle at top right, rgba(197, 76, 47, 0.08), transparent 30%),
    linear-gradient(180deg, #fffdfa 0%, #fff8f3 100%);
  border-top-left-radius: 32rpx;
  border-top-right-radius: 32rpx;
  padding: 28rpx 28rpx 42rpx;
  animation: slideUp 0.2s ease-out;
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.sheet-mask.leaving {
  opacity: 0;
}

.sheet-panel.leaving {
  transform: translateY(40rpx);
  opacity: 0.84;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14rpx;
}

.sheet-title {
  font-size: 28rpx;
  font-weight: 700;
  color: var(--text);
}

.sheet-close {
  font-size: 36rpx;
  line-height: 36rpx;
  color: #a08b7b;
}

.sheet-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18rpx;
  padding-top: 20rpx;
}

.hero-mark {
  position: relative;
  width: 150rpx;
  height: 150rpx;
  border-radius: 75rpx;
  background: linear-gradient(145deg, #fff3e9, #f9dccd);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 20rpx 44rpx rgba(197, 76, 47, 0.12);
}

.mark-orbit {
  position: absolute;
  inset: 14rpx;
  border: 2rpx dashed rgba(197, 76, 47, 0.18);
  border-radius: 999rpx;
}

.mark-text {
  position: relative;
  font-size: 56rpx;
  font-weight: 800;
  color: var(--primary-dark);
}

.welcome {
  font-size: 34rpx;
  font-weight: 700;
  color: var(--text);
}

.desc {
  font-size: 24rpx;
  color: var(--muted);
  text-align: center;
  line-height: 1.6;
  padding: 0 26rpx;
}

.agreement-card {
  width: 100%;
  display: flex;
  gap: 16rpx;
  align-items: flex-start;
  padding: 22rpx 20rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, 0.72);
  border: 1rpx solid rgba(106, 74, 52, 0.08);
  box-sizing: border-box;
}

.agreement-check {
  width: 34rpx;
  height: 34rpx;
  border-radius: 999rpx;
  border: 2rpx solid rgba(150, 50, 29, 0.36);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 4rpx;
}

.agreement-check.active {
  background: var(--primary);
  border-color: var(--primary);
}

.agreement-checkmark {
  font-size: 20rpx;
  color: #ffffff;
  line-height: 1;
}

.agreement-copy {
  flex: 1;
  min-width: 0;
}

.agreement-line {
  display: flex;
  flex-wrap: wrap;
  gap: 4rpx;
  line-height: 1.8;
}

.agreement-label,
.agreement-link,
.agreement-tip {
  font-size: 22rpx;
}

.agreement-label {
  color: var(--text-secondary);
}

.agreement-link {
  color: var(--primary);
  font-weight: 700;
}

.agreement-tip {
  display: block;
  margin-top: 8rpx;
  color: var(--muted);
  line-height: 1.7;
}

.wechat-btn {
  margin-top: 12rpx;
  width: 100%;
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #ffffff;
  height: 86rpx;
  line-height: 86rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  box-shadow: 0 16rpx 30rpx rgba(34, 197, 94, 0.22);
}

.wechat-btn[disabled] {
  opacity: 0.75;
}

.phone-btn {
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.phone-btn {
  background: rgba(148, 163, 184, 0.12);
  color: #5b6472;
}

.wechat-btn::after,
.phone-btn::after {
  border: 0;
}

@keyframes slideUp {
  from {
    transform: translateY(40rpx);
    opacity: 0.9;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}
</style>
