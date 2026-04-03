import { computed } from 'vue';
import { useUserStore } from '@/stores/user';
import { wxLogin, phoneLogin, logout as apiLogout, getInfo } from '@/api/auth';
import type { UserInfo } from '@/api/types';

/**
 * 用户认证相关逻辑
 */
export function useAuth() {
  const userStore = useUserStore();

  const isLogin = computed(() => userStore.isLogin);
  const userInfo = computed(() => userStore.userInfo);
  const isVip = computed(() => userStore.userInfo?.isVip === 1);
  const token = computed(() => userStore.token);

  /**
   * 微信登录
   */
  const loginByWx = async () => {
    return new Promise<UserInfo>((resolve, reject) => {
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            const result = await wxLogin(loginRes.code);
            userStore.setToken(result.token);
            userStore.setUserInfo(result.userInfo);
            resolve(result.userInfo);
          } catch (e) {
            reject(e);
          }
        },
        fail: (err) => {
          reject(new Error(err.errMsg || '微信登录失败'));
        }
      });
    });
  };

  /**
   * 手机号登录
   */
  const loginByPhone = async (phone: string, password: string) => {
    const result = await phoneLogin(phone, password);
    userStore.setToken(result.token);
    userStore.setUserInfo(result.userInfo);
    return result.userInfo;
  };

  /**
   * 退出登录
   */
  const logout = async (callApi = true) => {
    if (callApi && userStore.token) {
      try {
        await apiLogout();
      } catch (e) {
        // 忽略登出API错误
      }
    }
    userStore.logout();
  };

  /**
   * 刷新用户信息
   */
  const refreshUserInfo = async () => {
    if (!userStore.token) return null;
    try {
      const info = await getInfo();
      userStore.setUserInfo(info);
      return info;
    } catch (e) {
      return null;
    }
  };

  /**
   * 检查登录状态，未登录则显示提示
   */
  const requireLogin = (message = '请先登录'): boolean => {
    if (!isLogin.value) {
      userStore.requestLogin(null, message);
      return false;
    }
    return true;
  };

  /**
   * 检查VIP状态
   */
  const requireVip = (message = '该功能需要VIP'): boolean => {
    if (!requireLogin()) return false;
    if (!isVip.value) {
      uni.showModal({
        title: '温馨提示',
        content: message,
        confirmText: '开通VIP',
        success: (res) => {
          if (res.confirm) {
            uni.navigateTo({ url: '/pages/vip/index' });
          }
        }
      });
      return false;
    }
    return true;
  };

  return {
    isLogin,
    userInfo,
    isVip,
    token,
    loginByWx,
    loginByPhone,
    logout,
    refreshUserInfo,
    requireLogin,
    requireVip
  };
}
