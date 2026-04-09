import { ref } from "vue";
import { useUserStore } from "@/stores/user";

export function useLoginSheet(message = "请先登录") {
  const userStore = useUserStore();
  const showLogin = ref(false);
  const hasReadySession = () => !!userStore.token && !!userStore.userInfo?.id;

  const requestLogin = (action?: null | (() => void | Promise<void>), customMessage?: string) => {
    if (hasReadySession()) {
      if (action) action();
      return true;
    }
    const nextMessage = customMessage || message;
    showLogin.value = true;
    userStore.requestLogin(action || null, nextMessage);
    uni.showToast({ title: nextMessage, icon: "none" });
    return false;
  };

  const handleLoginSuccess = async () => {
    showLogin.value = false;
    await userStore.runPendingLoginAction();
  };

  const handleLoginClose = () => {
    showLogin.value = false;
    userStore.cancelPendingLoginAction();
  };

  return {
    showLogin,
    requestLogin,
    handleLoginSuccess,
    handleLoginClose
  };
}
