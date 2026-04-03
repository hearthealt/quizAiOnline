<template>
  <view class="page">
    <!-- 顶部卡片 -->
    <view class="header-card">
      <view class="vip-badge">👑 VIP</view>
      <text class="header-title">会员中心</text>
      
      <!-- 用户信息 -->
      <view class="user-info">
        <image v-if="avatar" class="user-avatar" :src="resolveAssetUrl(avatar)" mode="aspectFill" />
        <view v-else class="user-avatar placeholder">👤</view>
        <view class="user-detail">
          <text class="user-name">{{ nickname }}</text>
          <text class="user-status">{{ statusText }}</text>
          <text v-if="vipStatus?.expireTime" class="user-expire">有效期至 {{ vipStatus.expireTime }}</text>
        </view>
      </view>
    </view>

    <!-- 套餐选择 -->
    <view class="section">
      <view class="section-header">
        <text class="section-icon">💎</text>
        <text class="section-title">选择套餐</text>
      </view>
      <view class="plans-grid">
        <view 
          v-for="plan in plans" 
          :key="plan.id" 
          class="plan-item"
          :class="{ active: plan.id === selectedPlanId }"
          @tap="selectedPlanId = plan.id"
        >
          <view class="plan-tag" v-if="plan.id === plans[0]?.id">推荐</view>
          <text class="plan-name">{{ plan.name }}</text>
          <view class="plan-price-box">
            <text class="plan-price">¥{{ plan.price }}</text>
            <text v-if="plan.originalPrice" class="plan-origin">¥{{ plan.originalPrice }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 会员权益 -->
    <view class="section">
      <view class="section-header">
        <text class="section-icon">✨</text>
        <text class="section-title">专属特权</text>
      </view>
      <view class="benefits-list">
        <view class="benefit-item">
          <view class="benefit-left">
            <text class="benefit-icon">📚</text>
            <text class="benefit-title">全站题库畅刷</text>
          </view>
          <text class="benefit-desc">解锁所有专业题库</text>
        </view>
        <view class="benefit-item">
          <view class="benefit-left">
            <text class="benefit-icon">🤖</text>
            <text class="benefit-title">AI 智能解析</text>
          </view>
          <text class="benefit-desc">查看详细 AI 解析</text>
        </view>
        <view class="benefit-item">
          <view class="benefit-left">
            <text class="benefit-icon">📝</text>
            <text class="benefit-title">专属模拟考试</text>
          </view>
          <text class="benefit-desc">提升学习效率</text>
        </view>
        <view class="benefit-item">
          <view class="benefit-left">
            <text class="benefit-icon">⭐</text>
            <text class="benefit-title">错题收藏不限</text>
          </view>
          <text class="benefit-desc"> unlimited 收藏题目</text>
        </view>
      </view>
    </view>

    <!-- 底部操作栏 -->
    <view class="bottom-bar">
      <button class="open-btn" @tap="openVip">{{ actionText }}</button>
      <text class="footer-tip">开通即代表同意《会员服务协议》</text>
    </view>

    <!-- 成功提示弹窗 -->
    <view v-if="showSuccess" class="modal-mask" @tap="closeModal">
      <view class="modal-card" @tap.stop>
        <text class="modal-icon">✅</text>
        <text class="modal-title">订单已提交</text>
        <text class="modal-desc">已提交订单，等待人工确认</text>
        <view class="modal-actions">
          <button class="action-btn primary" @tap="goOrders">查看订单</button>
          <button class="action-btn ghost" @tap="closeModal">知道了</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad, onShow } from "@dcloudio/uni-app";
import { computed, ref } from "vue";
import { getVipPlans, getVipStatus, createVipOrder, type VipPlan } from "@/api/vip";
import { useUserStore } from "@/stores/user";
import { resolveAssetUrl } from "@/utils/assets";

const plans = ref<VipPlan[]>([]);
const vipStatus = ref<any>(null);
const selectedPlanId = ref<number | null>(null);
const showSuccess = ref(false);
const skipNextOnShow = ref(false);
const userStore = useUserStore();
const nickname = computed(() => userStore.userInfo?.nickname || "微信用户");
const avatar = computed(() => userStore.userInfo?.avatar || "");

const statusText = computed(() =>
  vipStatus.value?.isVip ? "VIP 已开通" : "当前为普通会员"
);
const actionText = computed(() =>
  vipStatus.value?.isVip ? "立即续费 VIP" : "立即开通 VIP"
);

const fetchData = async () => {
  if (userStore.isLogin) {
    await userStore.refreshUser().catch(() => null);
  }
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

onLoad(() => {
  skipNextOnShow.value = true;
  void fetchData();
});
onShow(() => {
  if (skipNextOnShow.value) {
    skipNextOnShow.value = false;
    return;
  }
  void fetchData();
});
</script>

<style lang="scss" scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fee2e2 0%, #ffffff 100%);
  padding: 24rpx 24rpx 160rpx;
}

/* 头部卡片 */
.header-card {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border-radius: 24rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 32rpx rgba(239, 68, 68, 0.3);
  position: relative;
  overflow: hidden;
}

.vip-badge {
  position: absolute;
  top: 20rpx;
  right: 20rpx;
  background: rgba(255, 255, 255, 0.25);
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  font-size: 22rpx;
  font-weight: 600;
  color: #fff;
}

.header-title {
  font-size: 36rpx;
  font-weight: 700;
  color: #fff;
  display: block;
  margin-bottom: 24rpx;
}

.user-info {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 20rpx;
  padding: 20rpx;
  display: flex;
  align-items: center;
  gap: 20rpx;
}

.user-avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  flex-shrink: 0;
}

.user-avatar.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
}

.user-detail {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #fff;
  display: block;
}

.user-status {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  display: block;
  margin-top: 6rpx;
}

.user-expire {
  font-size: 22rpx;
  color: rgba(255, 255, 255, 0.75);
  display: block;
  margin-top: 4rpx;
}

/* 区块样式 */
.section {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.06);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.section-icon {
  font-size: 32rpx;
}

.section-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}

/* 套餐网格 */
.plans-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16rpx;
}

.plan-item {
  background: #f8fafc;
  border-radius: 16rpx;
  padding: 20rpx 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  border: 2rpx solid transparent;
  position: relative;
}

.plan-item.active {
  border-color: #ef4444;
  background: #fef2f2;
}

.plan-tag {
  position: absolute;
  top: -8rpx;
  right: -8rpx;
  background: #fbbf24;
  color: #1f2937;
  padding: 4rpx 12rpx;
  border-radius: 12rpx;
  font-size: 20rpx;
  font-weight: 600;
}

.plan-name {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f2937;
}

.plan-price-box {
  text-align: center;
}

.plan-price {
  font-size: 32rpx;
  font-weight: 700;
  color: #ef4444;
  display: block;
}

.plan-origin {
  font-size: 20rpx;
  color: #9ca3af;
  text-decoration: line-through;
  margin-left: 6rpx;
}

/* 权益列表 */
.benefits-list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.benefit-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx;
  background: #f8fafc;
  border-radius: 12rpx;
}

.benefit-left {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.benefit-icon {
  font-size: 36rpx;
}

.benefit-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #1f2937;
}

.benefit-desc {
  font-size: 22rpx;
  color: #6b7280;
}

/* 底部操作栏 */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 20rpx 24rpx 28rpx;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0), #ffffff 50%);
  display: flex;
  flex-direction: column;
  gap: 12rpx;
  align-items: center;
}

.open-btn {
  width: 100%;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  height: 88rpx;
  line-height: 88rpx;
  border-radius: 44rpx;
  font-size: 30rpx;
  font-weight: 700;
  box-shadow: 0 8rpx 24rpx rgba(239, 68, 68, 0.4);
}

.footer-tip {
  font-size: 22rpx;
  color: #9ca3af;
}

/* 弹窗样式 */
.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-card {
  width: 70%;
  background: #fff;
  border-radius: 24rpx;
  padding: 32rpx;
  text-align: center;
}

.modal-icon {
  font-size: 64rpx;
  display: block;
  margin-bottom: 16rpx;
}

.modal-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
  display: block;
}

.modal-desc {
  font-size: 24rpx;
  color: #6b7280;
  display: block;
  margin-top: 12rpx;
}

.modal-actions {
  margin-top: 24rpx;
  display: flex;
  gap: 16rpx;
  justify-content: center;
}

.action-btn {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  border-radius: 36rpx;
  font-size: 26rpx;
  font-weight: 600;
}

.action-btn.primary {
  background: #ef4444;
  color: #fff;
}

.action-btn.ghost {
  background: #f3f4f6;
  color: #1f2937;
}
</style>
