import { ref } from "vue";
import { useUserStore } from "@/stores/user";

export function useLoginSheet(message = "请先登录") {
  const userStore = useUserStore();
  const showLogin = ref(false);
  const pendingAction = ref<null | (() => void | Promise<void>)>(null);

  const requestLogin = (action?: null | (() => void | Promise<void>), customMessage?: string) => {
    if (userStore.isLogin) {
      if (action) action();
      return true;
    }
    pendingAction.value = action || null;
    uni.showToast({ title: customMessage || message, icon: "none" });
    showLogin.value = true;
    return false;
  };

  const handleLoginSuccess = async () => {
    showLogin.value = false;
    const action = pendingAction.value;
    pendingAction.value = null;
    if (action) {
      await action();
    }
  };

  const handleLoginClose = () => {
    showLogin.value = false;
    pendingAction.value = null;
  };

  return {
    showLogin,
    requestLogin,
    handleLoginSuccess,
    handleLoginClose
  };
}
