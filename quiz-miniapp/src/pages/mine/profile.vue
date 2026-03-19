<template>
  <view class="page">
    <view class="card">
      <text class="title">编辑资料</text>
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
      <button class="primary-btn" @tap="save">保存</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { ref } from "vue";
import { updateProfile } from "@/api/user";
import { uploadImage } from "@/api/upload";
import { useUserStore } from "@/stores/user";
import { resolveAssetUrl } from "@/utils/assets";

const userStore = useUserStore();
const nickname = ref("");
const avatar = ref("");
const phone = ref("");

const save = async () => {
  await updateProfile(nickname.value, avatar.value, phone.value);
  await userStore.refreshUser();
  uni.navigateBack();
};

const chooseAvatar = async () => {
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

onLoad(() => {
  nickname.value = userStore.userInfo?.nickname || "";
  avatar.value = userStore.userInfo?.avatar || "";
  phone.value = userStore.userInfo?.phone || "";
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

.primary-btn {
  background: var(--primary);
  color: #ffffff;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
}
</style>
