<template>
  <view class="page">
    <view class="hero">
      <view class="hero-header">
        <text class="hero-title">会员中心</text>
        <view class="badge">{{ statusText }}</view>
      </view>
      <view class="profile-card">
        <image
          v-if="avatar"
          class="avatar"
          :src="resolveAssetUrl(avatar)"
          mode="aspectFill"
        />
        <view v-else class="avatar" />
        <view class="profile-info">
          <text class="profile-name">{{ nickname }}</text>
          <text class="profile-level">{{ statusText }}</text>
          <text v-if="vipStatus?.expireTime" class="profile-expire">
            到期 {{ vipStatus.expireTime }}
          </text>
        </view>
      </view>
    </view>

    <view class="section">
      <text class="section-title">选择套餐</text>
      <scroll-view class="plan-scroll" scroll-x>
        <view class="plan-row">
          <view
            v-for="plan in plans"
            :key="plan.id"
            class="plan-card"
            :class="{ active: plan.id === selectedPlanId }"
            @tap="selectedPlanId = plan.id"
          >
            <text class="plan-name">{{ plan.name }}</text>
            <text class="plan-price">¥{{ plan.price }}</text>
            <text v-if="plan.originalPrice" class="plan-origin">¥{{ plan.originalPrice }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="section">
      <text class="section-title">VIP 专属特权</text>
      <view class="benefits">
        <view class="benefit-item">
          <view class="benefit-icon">✓</view>
          <view>
            <text class="benefit-title">全站题库畅刷</text>
            <text class="benefit-desc">解锁所有分类下的专业题库，无限制刷题练习。</text>
          </view>
        </view>
        <view class="benefit-item">
          <view class="benefit-icon ai">✓</view>
          <view>
            <text class="benefit-title">AI 智能解析</text>
            <text class="benefit-desc">查看每道题目的详细 AI 解析，知识点一目了然。</text>
          </view>
        </view>
        <view class="benefit-item">
          <view class="benefit-icon">✓</view>
          <view>
            <text class="benefit-title">专属模拟考试</text>
            <text class="benefit-desc">参与专属模拟考试，轻松提升学习效率。</text>
          </view>
        </view>
      </view>
    </view>

    <view class="footer">
      <button class="primary-btn" @tap="openVip">{{ actionText }}</button>
      <text class="footer-tip">开通即代表同意《会员服务协议》</text>
    </view>

    <view v-if="showSuccess" class="modal-mask" @tap="closeModal">
      <view class="modal-card" @tap.stop>
        <text class="modal-title">订单已提交</text>
        <text class="modal-desc">已提交订单，等待人工确认</text>
        <view class="modal-actions">
          <button class="ghost-btn" @tap="goOrders">查看订单</button>
          <button class="ghost-btn" @tap="closeModal">知道了</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getVipPlans, getVipStatus, createVipOrder, type VipPlan } from "@/api/vip";
import { useUserStore } from "@/stores/user";
import { resolveAssetUrl } from "@/utils/assets";

const plans = ref<VipPlan[]>([]);
const vipStatus = ref<any>(null);
const selectedPlanId = ref<number | null>(null);
const showSuccess = ref(false);
const userStore = useUserStore();
const nickname = computed(() => userStore.userInfo?.nickname || "微信用户");
const avatar = computed(() => userStore.userInfo?.avatar || "");

const statusText = computed(() =>
  vipStatus.value?.isVip ? "VIP已开通" : "当前为普通会员"
);
const actionText = computed(() =>
  vipStatus.value?.isVip ? "立即续费 VIP" : "立即开通 VIP"
);

const fetchData = async () => {
  plans.value = await getVipPlans();
  vipStatus.value = await getVipStatus();
  if (plans.value.length) selectedPlanId.value = plans.value[0].id;
};

const openVip = async () => {
  if (!selectedPlanId.value) return;
  await createVipOrder(selectedPlanId.value);
  showSuccess.value = true;
};

const closeModal = () => {
  showSuccess.value = false;
};

const goOrders = () => {
  showSuccess.value = false;
  uni.navigateTo({ url: "/pages/vip/orders" });
};

onLoad(fetchData);
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: #0f172a;
  padding: 24rpx 24rpx 140rpx;
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.hero {
  background: linear-gradient(135deg, #0b1220, #1f2937);
  border-radius: 26rpx;
  padding: 28rpx;
  color: #ffffff;
}

.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hero-title {
  font-size: 34rpx;
  font-weight: 700;
}

.badge {
  background: rgba(255, 255, 255, 0.18);
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}

.profile-card {
  margin-top: 16rpx;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 20rpx;
  padding: 16rpx;
  display: flex;
  gap: 14rpx;
  align-items: center;
}

.avatar {
  width: 76rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, 0.2);
}

.profile-info {
  display: flex;
  flex-direction: column;
  gap: 6rpx;
}

.profile-name {
  font-size: 26rpx;
  font-weight: 600;
}

.profile-level {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
}

.profile-expire {
  font-size: 20rpx;
  color: rgba(255, 255, 255, 0.6);
}

.benefits {
  background: #111827;
  border-radius: 20rpx;
  padding: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.benefit-item {
  display: flex;
  gap: 12rpx;
  align-items: center;
}

.benefit-icon {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: rgba(251, 191, 36, 0.16);
  color: #fbbf24;
  display: flex;
  align-items: center;
  justify-content: center;
}

.benefit-icon.ai {
  background: rgba(99, 102, 241, 0.2);
  color: #a5b4fc;
}

.benefit-title {
  font-size: 24rpx;
  font-weight: 600;
  color: #ffffff;
}

.benefit-desc {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 4rpx;
}

.section-title {
  font-size: 28rpx;
  font-weight: 600;
  margin-bottom: 12rpx;
  color: #ffffff;
}

.plan-scroll {
  width: 100%;
}

.plan-row {
  display: flex;
  gap: 16rpx;
  padding-bottom: 6rpx;
}

.plan-card {
  width: 200rpx;
  background: #1f2937;
  border-radius: 20rpx;
  padding: 16rpx;
  display: flex;
  flex-direction: column;
  gap: 8rpx;
  border: 2rpx solid transparent;
  color: #ffffff;
}

.plan-card.active {
  border-color: #fbbf24;
  background: #111827;
}

.plan-name {
  font-size: 26rpx;
  font-weight: 600;
}

.plan-price {
  color: #fbbf24;
  font-size: 28rpx;
  font-weight: 700;
}

.plan-origin {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.5);
  text-decoration: line-through;
}

.footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx 28rpx;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0), #0f172a 40%);
  display: flex;
  flex-direction: column;
  gap: 10rpx;
  align-items: center;
}

.primary-btn {
  width: 100%;
  background: #fbbf24;
  color: #111827;
  height: 80rpx;
  line-height: 80rpx;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 700;
}

.footer-tip {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.6);
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-card {
  width: 70%;
  background: #ffffff;
  border-radius: 20rpx;
  padding: 24rpx;
  text-align: center;
}

.modal-title {
  font-size: 28rpx;
  font-weight: 700;
}

.modal-desc {
  font-size: 22rpx;
  color: var(--muted);
  margin-top: 10rpx;
}

.modal-actions {
  margin-top: 20rpx;
  display: flex;
  gap: 12rpx;
  justify-content: center;
}

.ghost-btn {
  background: #f1f5ff;
  color: var(--primary);
  height: 68rpx;
  line-height: 68rpx;
  border-radius: 999rpx;
  font-size: 24rpx;
}
</style>
