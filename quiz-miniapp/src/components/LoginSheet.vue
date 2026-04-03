<template>
  <view v-if="show" class="sheet-mask" :class="{ leaving: leaving }" @tap="close">
    <view class="sheet-panel" :class="{ leaving: leaving }" @tap.stop>
      <view class="sheet-header">
        <text class="sheet-title">登录/注册</text>
        <text class="sheet-close" @tap="close">x</text>
      </view>
      <view class="sheet-body">
        <view class="avatar">U</view>
        <text class="welcome">欢迎来到题库小程序</text>
        <text class="desc">{{ reasonText || "登录后即可进行练习、考试，并同步您的收藏与错题记录" }}</text>
        <button class="wechat-btn" @tap="handleWxLogin">
          微信一键登录
        </button>
        <button class="phone-btn" @tap="close">暂不登录</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onUnmounted, ref, watch } from "vue";
import { wxLogin } from "@/api/auth";
import { useUserStore } from "@/stores/user";

const props = defineProps<{ show: boolean; reasonText?: string }>();
const emit = defineEmits<{
  (e: "close"): void;
  (e: "success"): void;
}>();

const userStore = useUserStore();

const leaving = ref(false);

const close = () => {
  if (leaving.value) return;
  leaving.value = true;
  setTimeout(() => {
    leaving.value = false;
    emit("close");
  }, 180);
};

const confirmContinue = () =>
  new Promise<boolean>((resolve) => {
    uni.showModal({
      title: "授权提醒",
      content: "未授权头像昵称，将使用默认头像昵称继续登录。",
      confirmText: "继续登录",
      cancelText: "取消",
      success: (res) => resolve(res.confirm)
    });
  });

const loading = ref(false);

const handleWxLogin = async () => {
  if (loading.value) return;
  loading.value = true;
  let nickname = "";
  let avatar = "";
  try {
    const profile = await uni.getUserProfile({ desc: "用于完善用户资料" });
    nickname = profile.userInfo?.nickName || "";
    avatar = profile.userInfo?.avatarUrl || "";
  } catch (err) {
    const ok = await confirmContinue();
    if (!ok) {
      loading.value = false;
      return;
    }
  }
  const loginRes = await uni.login({ provider: "weixin" });
  if (!loginRes.code) {
    uni.showToast({ title: "登录失败，请重试", icon: "none" });
    loading.value = false;
    return;
  }
  const data = await wxLogin(loginRes.code || "", nickname, avatar);
  userStore.setToken(data.token);
  userStore.setUserInfo(data.userInfo);
  emit("success");
  loading.value = false;
};

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      uni.hideTabBar({ animation: true });
      leaving.value = false;
    } else {
      uni.showTabBar({ animation: true });
    }
  },
  { immediate: true }
);

onUnmounted(() => {
  uni.showTabBar({ animation: true });
});
</script>

<style lang="scss" scoped>
.sheet-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.35);
  display: flex;
  align-items: flex-end;
  z-index: 999;
  transition: opacity 0.18s ease;
}

.sheet-panel {
  width: 100%;
  background: #ffffff;
  border-top-left-radius: 28rpx;
  border-top-right-radius: 28rpx;
  padding: 24rpx 24rpx 40rpx;
  animation: slideUp 0.2s ease-out;
  transition: transform 0.18s ease, opacity 0.18s ease;
}

.sheet-mask.leaving {
  opacity: 0;
}

.sheet-panel.leaving {
  transform: translateY(40rpx);
  opacity: 0.8;
}

.sheet-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #f1f5f9;
}

.sheet-title {
  font-size: 28rpx;
  font-weight: 600;
}

.sheet-close {
  font-size: 36rpx;
  line-height: 36rpx;
  color: #94a3b8;
}

.sheet-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  padding-top: 24rpx;
}

.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 60rpx;
  background: var(--primary-weak);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
}

.welcome {
  font-size: 30rpx;
  font-weight: 600;
}

.desc {
  font-size: 24rpx;
  color: var(--muted);
  text-align: center;
  line-height: 1.5;
  padding: 0 24rpx;
}

.wechat-btn {
  margin-top: 8rpx;
  width: 100%;
  background: #22c55e;
  color: #ffffff;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
}

.phone-btn {
  width: 100%;
  background: #f3f4f6;
  color: #374151;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
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
