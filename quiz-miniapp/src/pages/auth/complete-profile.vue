<template>
  <view class="complete-page">
    <view class="intro">
      <text class="intro-title">首次登录请完善资料</text>
      <text class="intro-desc">只需要设置头像和昵称。</text>
    </view>

    <view class="panel">
      <!-- #ifdef MP-WEIXIN -->
      <button
        class="avatar-panel"
        :class="{ disabled: avatarBusy }"
        :disabled="avatarBusy"
        open-type="chooseAvatar"
        @tap="handleChooseAvatarTap"
        @chooseavatar="handleChooseAvatar"
      >
        <image v-if="avatarPreviewUrl" class="avatar-image" :src="avatarPreviewUrl" mode="aspectFill" />
        <view v-else class="avatar-placeholder">
          <text class="avatar-symbol">Q</text>
        </view>
        <text class="avatar-title">{{ uploading ? "头像上传中..." : choosingAvatar ? "头像选择中..." : "选择头像" }}</text>
        <text class="avatar-hint">{{ choosingAvatar ? "请在微信弹窗中确认头像" : "点击后可直接使用微信头像" }}</text>
      </button>
      <!-- #endif -->
      <!-- #ifndef MP-WEIXIN -->
      <button class="avatar-panel" @tap="pickAvatarImage">
        <image v-if="avatarPreviewUrl" class="avatar-image" :src="avatarPreviewUrl" mode="aspectFill" />
        <view v-else class="avatar-placeholder">
          <text class="avatar-symbol">Q</text>
        </view>
        <text class="avatar-title">{{ uploading ? "头像上传中..." : "选择头像" }}</text>
        <text class="avatar-hint">点击选择一张图片</text>
      </button>
      <!-- #endif -->

      <view class="field-block">
        <view class="field-head">
          <text class="field-label">昵称</text>
          <text class="field-required">必填</text>
        </view>
        <!-- #ifdef MP-WEIXIN -->
        <view class="nickname-tip">
          <text class="nickname-tip-title">微信昵称</text>
          <text class="nickname-tip-text">点输入框后，在底部点“使用微信昵称”。</text>
        </view>
        <!-- #endif -->
        <!-- #ifdef MP-WEIXIN -->
        <input
          class="field-input"
          type="nickname"
          maxlength="20"
          :value="nickname"
          placeholder="请输入微信昵称"
          @input="handleNicknameInput"
        />
        <!-- #endif -->
        <!-- #ifndef MP-WEIXIN -->
        <input
          class="field-input"
          maxlength="20"
          :value="nickname"
          placeholder="请输入昵称"
          @input="handleNicknameInput"
        />
        <!-- #endif -->
      </view>
    </view>

    <view class="footer">
      <button class="primary-btn" :disabled="loading || uploading" @tap="save">
        {{ uploading ? "头像上传中..." : loading ? "保存中..." : "保存并进入" }}
      </button>
      <button v-if="fromLogin" class="secondary-btn" @tap="exitFlow">暂不完善，退出登录</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onBackPress, onLoad, onUnload } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { logout as apiLogout } from "@/api/auth";
import { updateProfile } from "@/api/user";
import { useUserStore } from "@/stores/user";
import { buildRemoteUrl, ensureApiBaseUrl, missingBaseUrlMessage } from "@/utils/baseUrl";
import { normalizeAvatarPath, resolveAssetUrl } from "@/utils/assets";
import { traceRuntime } from "@/utils/runtimeTrace";

const userStore = useUserStore();

const fromLogin = ref(false);
const completed = ref(false);
const loading = ref(false);
const uploading = ref(false);
const choosingAvatar = ref(false);
const nickname = ref("");
const avatar = ref("");
const avatarPreview = ref("");

const normalizeValue = (value?: string) => value?.trim() || "";
let chooseAvatarResetTimer: ReturnType<typeof setTimeout> | null = null;

const avatarBusy = computed(() => loading.value || uploading.value || choosingAvatar.value);

const avatarPreviewUrl = computed(() => {
  const path = normalizeValue(avatarPreview.value);
  if (!path) return "";
  if (/^https?:\/\//i.test(path) || path.startsWith("//") || path.startsWith("/")) {
    return resolveAssetUrl(path);
  }
  return path;
});

const leaveIncompleteFlow = () => {
  if (!fromLogin.value || completed.value) return;
  if (userStore.token) {
    void apiLogout(true).catch(() => null);
  }
  userStore.logout();
  userStore.cancelPendingLoginAction();
};

const handleNicknameInput = (event: any) => {
  nickname.value = normalizeValue(event.detail?.value);
};

const clearChooseAvatarLock = () => {
  choosingAvatar.value = false;
  if (chooseAvatarResetTimer) {
    clearTimeout(chooseAvatarResetTimer);
    chooseAvatarResetTimer = null;
  }
};

const handleChooseAvatarTap = () => {
  // #ifdef MP-WEIXIN
  if (avatarBusy.value) return;
  choosingAvatar.value = true;
  chooseAvatarResetTimer = setTimeout(() => {
    choosingAvatar.value = false;
    chooseAvatarResetTimer = null;
  }, 4000);
  // #endif
};

const uploadImage = (filePath: string): Promise<string> =>
  new Promise((resolve, reject) => {
    const baseUrlCheck = ensureApiBaseUrl();
    if (!baseUrlCheck.ok) {
      uni.showToast({ title: "请配置小程序接口域名", icon: "none" });
      reject(new Error(missingBaseUrlMessage));
      return;
    }

    const requestUrl = buildRemoteUrl("/api/app/upload/image");
    const startedAt = Date.now();

    traceRuntime("upload:start", {
      url: requestUrl,
      timeout: 60000
    });

    uni.uploadFile({
      url: requestUrl,
      filePath,
      name: "file",
      timeout: 60000,
      header: {
        Authorization: userStore.token || ""
      },
      success: (res) => {
        try {
          traceRuntime("upload:success", {
            url: requestUrl,
            statusCode: res.statusCode,
            duration: Date.now() - startedAt
          });
          const payload = JSON.parse(res.data as unknown as string);
          if (res.statusCode === 200 && payload.code === 200) {
            resolve(payload.data);
            return;
          }
          if (res.statusCode === 401 || payload.code === 401) {
            const message = payload.message || payload.msg || "登录已过期";
            userStore.handleSessionExpired(message);
            uni.showToast({ title: message, icon: "none" });
            reject(new Error(message));
            return;
          }
          reject(new Error(payload.message || payload.msg || "上传失败"));
        } catch (error) {
          reject(error);
        }
      },
      fail: (error) => {
        const errMsg = (error as { errMsg?: string })?.errMsg || "upload fail";
        traceRuntime("upload:fail", {
          url: requestUrl,
          errMsg,
          duration: Date.now() - startedAt
        });
        reject(error);
      }
    });
  });

const leaveCurrentPage = (fallbackUrl = "/pages/index/index") => {
  if (getCurrentPages().length > 1) {
    uni.navigateBack();
    return;
  }
  uni.switchTab({ url: fallbackUrl });
};

const uploadAvatarFile = async (filePath: string) => {
  if (!filePath || uploading.value) return;
  uploading.value = true;
  avatarPreview.value = filePath;
  avatar.value = "";
  uni.showLoading({ title: "上传头像中" });
  try {
    const url = await uploadImage(filePath);
    avatar.value = url;
  } catch (error) {
    avatar.value = "";
    avatarPreview.value = "";
    uni.showToast({ title: "头像上传失败，请重试", icon: "none" });
  } finally {
    uploading.value = false;
    uni.hideLoading();
  }
};

const handleChooseAvatar = async (event: any) => {
  clearChooseAvatarLock();
  const filePath = normalizeValue(event.detail?.avatarUrl);
  if (!filePath) {
    return;
  }
  await uploadAvatarFile(filePath);
};

const pickAvatarImage = async () => {
  try {
    const res = await uni.chooseImage({ count: 1, sizeType: ["compressed"] });
    const filePath = res.tempFilePaths?.[0];
    if (!filePath) {
      uni.showToast({ title: "未获取到头像，请重试", icon: "none" });
      return;
    }
    await uploadAvatarFile(filePath);
  } catch (error) {
    uni.showToast({ title: "未获取到头像，请重试", icon: "none" });
  }
};

const finishAfterSave = () => {
  const runPending = () => {
    setTimeout(() => {
      void userStore.runPendingLoginAction();
    }, 80);
  };

  if (getCurrentPages().length > 1) {
    uni.navigateBack({
      delta: 1,
      success: runPending,
      fail: () => {
        uni.switchTab({
          url: "/pages/index/index",
          success: runPending
        });
      }
    });
    return;
  }

  uni.switchTab({
    url: "/pages/index/index",
    success: runPending
  });
};

const save = async () => {
  if (loading.value || uploading.value) return;
  const nextNickname = normalizeValue(nickname.value);
  const nextAvatar = normalizeValue(avatar.value);

  if (!nextAvatar) {
    uni.showToast({ title: "请先上传头像", icon: "none" });
    return;
  }
  if (!nextNickname) {
    uni.showToast({ title: "请输入昵称", icon: "none" });
    return;
  }
  if (!userStore.token) {
    uni.showToast({ title: "登录状态失效，请重新登录", icon: "none" });
    userStore.logout();
    return;
  }

  loading.value = true;
  try {
    await updateProfile(nextNickname, nextAvatar);
    await userStore.refreshUser(true);
    completed.value = true;
    uni.showToast({ title: "资料已保存", icon: "none" });

    if (fromLogin.value) {
      finishAfterSave();
      return;
    }

    leaveCurrentPage();
  } finally {
    loading.value = false;
  }
};

const exitFlow = () => {
  leaveIncompleteFlow();
  leaveCurrentPage();
};

onLoad((options) => {
  fromLogin.value = options?.from === "login";
  if (!userStore.token) {
    uni.showToast({ title: "请先登录", icon: "none" });
    setTimeout(() => {
      leaveCurrentPage();
    }, 120);
    return;
  }

  const currentNickname = normalizeValue(userStore.userInfo?.nickname);
  const currentAvatar = normalizeAvatarPath(userStore.userInfo?.avatar);
  nickname.value = currentNickname && currentNickname !== "微信用户" ? currentNickname : "";
  avatar.value = currentAvatar;
  avatarPreview.value = avatar.value;
});

onBackPress(() => {
  leaveIncompleteFlow();
  return false;
});

onUnload(() => {
  leaveIncompleteFlow();
});
</script>

<style lang="scss" scoped>
.complete-page {
  min-height: 100vh;
  padding: 24rpx 24rpx calc(32rpx + env(safe-area-inset-bottom));
  background: #f6f6f6;
  box-sizing: border-box;
}

.intro {
  padding: 8rpx 4rpx 20rpx;
}

.intro-title {
  display: block;
  font-size: 38rpx;
  font-weight: 700;
  color: #222222;
}

.intro-desc {
  display: block;
  font-size: 26rpx;
  line-height: 1.6;
  color: #666666;
}

.panel {
  padding: 24rpx;
  border-radius: 24rpx;
  background: #ffffff;
  border: 1rpx solid #ececec;
}

.avatar-panel {
  width: 100%;
  padding: 28rpx 24rpx;
  border-radius: 20rpx;
  background: #fafafa;
  border: 1rpx dashed #dddddd;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
}

.avatar-panel.disabled {
  opacity: 0.72;
}

.avatar-panel::after,
.primary-btn::after,
.secondary-btn::after {
  border: 0;
}

.avatar-image,
.avatar-placeholder {
  width: 132rpx;
  height: 132rpx;
  border-radius: 999rpx;
  overflow: hidden;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-symbol {
  font-size: 48rpx;
  font-weight: 700;
  color: #999999;
}

.avatar-title {
  font-size: 30rpx;
  font-weight: 600;
  color: #222222;
}

.avatar-hint {
  font-size: 22rpx;
  color: #888888;
}

.field-block {
  margin-top: 24rpx;
}

.field-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.field-label {
  font-size: 26rpx;
  font-weight: 600;
  color: #222222;
}

.field-required {
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  background: #fff2f0;
  font-size: 22rpx;
  font-weight: 500;
  line-height: 1;
  color: #d84b36;
}

.nickname-tip {
  margin-bottom: 12rpx;
  padding: 16rpx 18rpx;
  border-radius: 16rpx;
  background: #f6f7f8;
}

.nickname-tip-title {
  display: block;
  font-size: 22rpx;
  font-weight: 600;
  color: #222222;
}

.nickname-tip-text {
  display: block;
  margin-top: 6rpx;
  font-size: 22rpx;
  line-height: 1.6;
  color: #666666;
}

.field-input {
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
  padding: 0 20rpx;
  border-radius: 16rpx;
  background: #ffffff;
  border: 1rpx solid #dddddd;
  font-size: 28rpx;
  color: #222222;
  box-sizing: border-box;
}

.footer {
  margin-top: 24rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.primary-btn {
  width: 100%;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 16rpx;
  background: #e67a54;
  color: #ffffff;
  font-size: 28rpx;
  font-weight: 600;
}

.primary-btn[disabled] {
  opacity: 0.72;
}

.secondary-btn {
  width: 100%;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 16rpx;
  background: #ffffff;
  border: 1rpx solid #dddddd;
  color: #444444;
  font-size: 26rpx;
}
</style>
