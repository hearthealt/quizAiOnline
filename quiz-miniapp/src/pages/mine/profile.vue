<template>
  <view class="page">
    <view class="card">
      <text class="title">编辑资料</text>
      <view class="notice-card">
        <text class="notice-title">资料与手机号说明</text>
        <text class="notice-text">头像、昵称和手机号仅用于资料展示、账号识别、登录校验和必要服务通知。</text>
      </view>
      <view class="avatar-row">
        <image
          v-if="avatar"
          class="avatar"
          :src="resolveAssetUrl(avatar)"
          mode="aspectFill"
        />
        <view v-else class="avatar" />
        <button class="ghost-btn" @tap="chooseAvatar">上传头像</button>
      </view>
      <input v-model="nickname" class="input" placeholder="昵称" />
      <input v-model="phone" class="input" placeholder="手机号（可直接绑定）" />
      <text class="field-tip">手机号仅用于资料绑定、账号识别和必要服务通知，不会在未获同意时收集。</text>
      <view class="agreement-row" @tap="toggleAgreement">
        <view class="agreement-check" :class="{ active: agreementAccepted }">
          <text class="agreement-mark">{{ agreementAccepted ? "✓" : "" }}</text>
        </view>
        <view class="agreement-copy">
          <view class="agreement-line">
            <text class="agreement-label">已阅读并同意</text>
            <text class="agreement-link" @tap.stop="openServiceAgreement">《用户服务协议》</text>
            <text class="agreement-label">与</text>
            <text class="agreement-link" @tap.stop="openPrivacyPolicy">《隐私政策》</text>
          </view>
        </view>
      </view>
      <button class="primary-btn" @tap="save">保存</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { ref } from "vue";
import { updateProfile } from "@/api/user";
import { useUserStore } from "@/stores/user";
import { buildRemoteUrl, ensureApiBaseUrl, missingBaseUrlMessage } from "@/utils/baseUrl";
import { resolveAssetUrl } from "@/utils/assets";
import { traceRuntime } from "@/utils/runtimeTrace";
import {
  clearLegalConsent,
  ensureLegalConsent,
  hasLegalConsent,
  openAgreementPage,
  saveLegalConsent
} from "@/utils/privacy";

const userStore = useUserStore();
const nickname = ref("");
const avatar = ref("");
const phone = ref("");
const agreementAccepted = ref(hasLegalConsent());

const leaveCurrentPage = (fallbackUrl = "/tab-pages/mine/index") => {
  if (getCurrentPages().length > 1) {
    uni.navigateBack();
    return;
  }
  uni.switchTab({ url: fallbackUrl });
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

const save = async () => {
  if (!ensureLegalConsent("请先阅读并同意协议后再保存资料")) return;
  saveLegalConsent();

  await updateProfile(nickname.value, avatar.value, phone.value);
  await userStore.refreshUser();
  leaveCurrentPage();
};

const chooseAvatar = async () => {
  if (!ensureLegalConsent("请先阅读并同意协议后再上传头像")) return;
  saveLegalConsent();

  const res = await uni.chooseImage({ count: 1, sizeType: ["compressed"] });
  const filePath = res.tempFilePaths?.[0];
  if (!filePath) return;
  uni.showLoading({ title: "上传中" });
  try {
    const url = await uploadImage(filePath);
    avatar.value = url;
    uni.showToast({ title: "上传成功", icon: "none" });
  } finally {
    uni.hideLoading();
  }
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
  setAgreementAccepted(!agreementAccepted.value);
};

const openServiceAgreement = () => {
  openAgreementPage("service");
};

const openPrivacyPolicy = () => {
  openAgreementPage("privacy");
};

onLoad(() => {
  nickname.value = userStore.userInfo?.nickname || "";
  avatar.value = userStore.userInfo?.avatar || "";
  phone.value = userStore.userInfo?.phone || "";
  agreementAccepted.value = hasLegalConsent();
});
</script>

<style lang="scss" scoped>
.page {
  padding: 24rpx;
}

.card {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 12rpx 40rpx rgba(15, 23, 42, 0.08);
}

.title {
  font-size: 30rpx;
  font-weight: 600;
  margin-bottom: 16rpx;
}

.notice-card {
  margin-bottom: 18rpx;
  padding: 20rpx;
  border-radius: 18rpx;
  background: #fff7ef;
  border: 1rpx solid rgba(230, 122, 84, 0.12);
}

.notice-title {
  display: block;
  font-size: 24rpx;
  font-weight: 600;
  color: #9a401f;
}

.notice-text {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: #7c5a48;
}

.avatar-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 16rpx;
}

.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 48rpx;
  background: #f1f5ff;
  overflow: hidden;
}

.ghost-btn {
  background: #f1f5ff;
  color: var(--primary);
  height: 60rpx;
  line-height: 60rpx;
  border-radius: 999rpx;
  padding: 0 20rpx;
  font-size: 24rpx;
}

.input {
  border: 2rpx solid #e5e7eb;
  border-radius: 16rpx;
  padding: 16rpx;
  font-size: 26rpx;
  margin-bottom: 16rpx;
}

.field-tip {
  display: block;
  margin: -4rpx 0 12rpx;
  font-size: 22rpx;
  line-height: 1.7;
  color: #7c5a48;
}

.agreement-row {
  display: flex;
  align-items: flex-start;
  gap: 14rpx;
  padding: 18rpx 0 8rpx;
}

.agreement-check {
  width: 32rpx;
  height: 32rpx;
  border-radius: 999rpx;
  border: 2rpx solid rgba(230, 122, 84, 0.36);
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

.agreement-mark {
  font-size: 20rpx;
  line-height: 1;
  color: #ffffff;
}

.agreement-copy {
  flex: 1;
  min-width: 0;
}

.agreement-line {
  display: flex;
  flex-wrap: wrap;
  gap: 4rpx;
}

.agreement-label,
.agreement-link {
  font-size: 22rpx;
  line-height: 1.8;
}

.agreement-label {
  color: #6b7280;
}

.agreement-link {
  color: var(--primary);
  font-weight: 600;
}

.primary-btn {
  background: var(--primary);
  color: #ffffff;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
}

.ghost-btn::after,
.primary-btn::after {
  border: 0;
}
</style>
